package com.kelompok5.orchestrator.service;

import com.kelompok5.orchestrator.dto.OrderRequest;
import com.kelompok5.orchestrator.dto.OrderResponse;
import com.kelompok5.orchestrator.model.*;
import com.kelompok5.orchestrator.repository.OrderRepository;
import com.kelompok5.orchestrator.repository.SagaLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

    private final WebClient webClient;
    private final OrderRepository orderRepository;
    private final SagaLogRepository sagaLogRepository;

    public OrderSagaOrchestrator(WebClient webClient, OrderRepository orderRepository, SagaLogRepository sagaLogRepository) {
        this.webClient = webClient;
        this.orderRepository = orderRepository;
        this.sagaLogRepository = sagaLogRepository;
    }

    @SuppressWarnings("unchecked")
    public OrderResponse execute(OrderRequest request) {
        String sagaId = UUID.randomUUID().toString();
        List<String> executedSteps = new ArrayList<>();
        String orderId = null;

        Map<String, Object> step1Response = null;
        Map<String, Object> step2Response = null;
        Map<String, Object> step3Response = null;

        log.info("========================================");
        log.info("Saga [{}] STARTED - Product: {}, Qty: {}, Total: {}",
                sagaId, request.getProductName(), request.getQuantity(), request.getTotalPrice());
        log.info("========================================");

        // ── STEP 1: Create Order ──
        log.info("Saga [{}] Step 1 - Calling Order Service (POST /mock1/order)...", sagaId);
        try {
            step1Response = callCreateOrder(request);
            orderId = (String) step1Response.get("orderId");
            executedSteps.add("CREATE_ORDER");
            saveLog(sagaId, SagaStep.CREATE_ORDER, SagaStatus.ORDER_CREATED,
                    "Order created: " + orderId);
            log.info("Saga [{}] Step 1 SUCCESS - Response: {}", sagaId, step1Response);
        } catch (Exception e) {
            saveLog(sagaId, SagaStep.CREATE_ORDER, SagaStatus.FAILED,
                    "Failed to create order: " + e.getMessage());
            log.error("Saga [{}] Step 1 FAILED - Error: {}", sagaId, e.getMessage());
            return buildResponse(sagaId, null, SagaStatus.FAILED,
                    "Order creation failed: " + e.getMessage(),
                    executedSteps, null, null, null);
        }

        // ── STEP 2: Process Payment ──
        log.info("Saga [{}] Step 2 - Calling Payment Service (POST /mock2/payment)...", sagaId);
        try {
            step2Response = callProcessPayment(orderId, request);
            executedSteps.add("PROCESS_PAYMENT");
            saveLog(sagaId, SagaStep.PROCESS_PAYMENT, SagaStatus.PAYMENT_PROCESSED,
                    "Payment processed for order: " + orderId);
            log.info("Saga [{}] Step 2 SUCCESS - Response: {}", sagaId, step2Response);
        } catch (Exception e) {
            saveLog(sagaId, SagaStep.PROCESS_PAYMENT, SagaStatus.FAILED,
                    "Payment failed: " + e.getMessage());
            log.error("Saga [{}] Step 2 FAILED - Error: {} | Starting COMPENSATION...", sagaId, e.getMessage());
            compensate(sagaId, executedSteps, orderId);
            return buildResponse(sagaId, orderId, SagaStatus.COMPENSATED,
                    "Payment failed, saga compensated: " + e.getMessage(),
                    executedSteps, step1Response, null, null);
        }

        // ── STEP 3: Reserve Inventory ──
        log.info("Saga [{}] Step 3 - Calling Inventory Service (POST /mock3/inventory)...", sagaId);
        try {
            step3Response = callReserveInventory(orderId, request);
            executedSteps.add("RESERVE_INVENTORY");
            saveLog(sagaId, SagaStep.RESERVE_INVENTORY, SagaStatus.INVENTORY_RESERVED,
                    "Inventory reserved for order: " + orderId);
            log.info("Saga [{}] Step 3 SUCCESS - Response: {}", sagaId, step3Response);
        } catch (Exception e) {
            saveLog(sagaId, SagaStep.RESERVE_INVENTORY, SagaStatus.FAILED,
                    "Inventory reservation failed: " + e.getMessage());
            log.error("Saga [{}] Step 3 FAILED - Error: {} | Starting COMPENSATION...", sagaId, e.getMessage());
            compensate(sagaId, executedSteps, orderId);
            return buildResponse(sagaId, orderId, SagaStatus.COMPENSATED,
                    "Inventory failed, saga compensated: " + e.getMessage(),
                    executedSteps, step1Response, step2Response, null);
        }

        // ── ALL STEPS SUCCESSFUL ──
        saveLog(sagaId, SagaStep.RESERVE_INVENTORY, SagaStatus.COMPLETED,
                "Saga completed successfully");
        log.info("========================================");
        log.info("Saga [{}] COMPLETED - All 3 steps succeeded", sagaId);
        log.info("========================================");

        return buildResponse(sagaId, orderId, SagaStatus.COMPLETED,
                "Order placed successfully",
                executedSteps, step1Response, step2Response, step3Response);
    }

    private void compensate(String sagaId, List<String> executedSteps, String orderId) {
        saveLog(sagaId, null, SagaStatus.COMPENSATING, "Compensation started");
        log.warn("Saga [{}] ═══ COMPENSATION STARTED ═══ for steps: {}", sagaId, executedSteps);

        for (int i = executedSteps.size() - 1; i >= 0; i--) {
            String step = executedSteps.get(i);
            try {
                switch (step) {
                    case "PROCESS_PAYMENT" -> {
                        log.warn("Saga [{}] Compensating PROCESS_PAYMENT - Calling refund...", sagaId);
                        callRefundPayment(orderId);
                        saveLog(sagaId, SagaStep.REFUND_PAYMENT, SagaStatus.COMPENSATED,
                                "Payment refunded for order: " + orderId);
                        log.info("Saga [{}] Compensation OK - Payment refunded for order: {}", sagaId, orderId);
                    }
                    case "CREATE_ORDER" -> {
                        log.warn("Saga [{}] Compensating CREATE_ORDER - Calling cancel...", sagaId);
                        callCancelOrder(orderId);
                        saveLog(sagaId, SagaStep.CANCEL_ORDER, SagaStatus.COMPENSATED,
                                "Order cancelled: " + orderId);
                        log.info("Saga [{}] Compensation OK - Order cancelled: {}", sagaId, orderId);
                    }
                    default -> log.info("Saga [{}] No compensation needed for step: {}", sagaId, step);
                }
            } catch (Exception e) {
                log.error("Saga [{}] Compensation FAILED for step {}: {}", sagaId, step, e.getMessage());
                saveLog(sagaId, null, SagaStatus.FAILED,
                        "Compensation failed for " + step + ": " + e.getMessage());
            }
        }

        saveLog(sagaId, null, SagaStatus.COMPENSATED, "Compensation completed");
        log.warn("Saga [{}] ═══ COMPENSATION COMPLETED ═══", sagaId);
    }

    // ─── HTTP calls to mock microservices ───

    @SuppressWarnings("unchecked")
    private Map<String, Object> callCreateOrder(OrderRequest request) {
        Map<String, Object> result = webClient.post()
                .uri("http://localhost:8080/mock1/order")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (result == null || result.get("orderId") == null) {
            throw new RuntimeException("Order service returned no orderId");
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callProcessPayment(String orderId, OrderRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("orderId", orderId);
        body.put("amount", request.getTotalPrice());

        Map<String, Object> result = webClient.post()
                .uri("http://localhost:8080/mock2/payment")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (result == null) {
            throw new RuntimeException("Payment service returned null response");
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callReserveInventory(String orderId, OrderRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("orderId", orderId);
        body.put("productName", request.getProductName());
        body.put("quantity", request.getQuantity());

        Map<String, Object> result = webClient.post()
                .uri("http://localhost:8080/mock3/inventory")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (result == null) {
            throw new RuntimeException("Inventory service returned null response");
        }
        return result;
    }

    private void callRefundPayment(String orderId) {
        webClient.post()
                .uri("http://localhost:8080/mock2/payment/refund/" + orderId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    private void callCancelOrder(String orderId) {
        webClient.delete()
                .uri("http://localhost:8080/mock1/order/" + orderId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    private void saveLog(String sagaId, SagaStep step, SagaStatus status, String message) {
        SagaLog sagaLog = new SagaLog(sagaId, step, status, message);
        sagaLogRepository.save(sagaLog);
    }

    private OrderResponse buildResponse(String sagaId, String orderId, SagaStatus status,
                                         String message, List<String> executedSteps,
                                         Map<String, Object> step1, Map<String, Object> step2,
                                         Map<String, Object> step3) {
        OrderResponse response = new OrderResponse();
        response.setSagaId(sagaId);
        response.setOrderId(orderId);
        response.setStatus(status);
        response.setMessage(message);
        response.setExecutedSteps(executedSteps);
        response.setStep1CreateOrder(step1);
        response.setStep2ProcessPayment(step2);
        response.setStep3ReserveInventory(step3);
        return response;
    }
}
