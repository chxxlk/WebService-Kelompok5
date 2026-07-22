package com.kelompok5.orchestrator.service;

import com.kelompok5.orchestrator.dto.OrderRequest;
import com.kelompok5.orchestrator.dto.OrderResponse;
import com.kelompok5.orchestrator.model.*;
import com.kelompok5.orchestrator.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

    private final WebClient webClient;
    private final OrderRepository orderRepository;
    private final String orderServiceUrl;
    private final String paymentServiceUrl;
    private final String inventoryServiceUrl;

    public OrderSagaOrchestrator(WebClient webClient, OrderRepository orderRepository,
                                  @Value("${services.order.url:http://localhost:8080}") String orderServiceUrl,
                                  @Value("${services.payment.url:http://localhost:8080}") String paymentServiceUrl,
                                  @Value("${services.inventory.url:http://localhost:8080}") String inventoryServiceUrl) {
        this.webClient = webClient;
        this.orderRepository = orderRepository;
        this.orderServiceUrl = orderServiceUrl;
        this.paymentServiceUrl = paymentServiceUrl;
        this.inventoryServiceUrl = inventoryServiceUrl;
    }

    @SuppressWarnings("unchecked")
    public OrderResponse execute(OrderRequest request) {
        String sagaId = UUID.randomUUID().toString();
        List<String> executedSteps = new ArrayList<>();
        String orderId = null;
        Order order = null;

        Map<String, Object> step1Response = null;
        Map<String, Object> step2Response = null;
        Map<String, Object> step3Response = null;

        log.info("========================================");
        log.info("Saga [{}] STARTED - Product: {}, Qty: {}, Total: {}",
                sagaId, request.getProductName(), request.getQuantity(), request.getTotalPrice());
        log.info("========================================");

        // ══════════════════════════════════════════════════════════════
        // STEP 1: CREATE ORDER
        // ══════════════════════════════════════════════════════════════
        log.info("Saga [{}] ───────────────────────────────────", sagaId);
        log.info("Saga [{}] STEP 1/3 - CREATE ORDER", sagaId);
        log.info("Saga [{}]   → Calling: POST {}/mock1/order", sagaId, orderServiceUrl);
        log.info("Saga [{}]   → Request: productName={}, quantity={}, totalPrice={}",
                sagaId, request.getProductName(), request.getQuantity(), request.getTotalPrice());
        try {
            step1Response = callCreateOrder(request);
            orderId = (String) step1Response.get("orderId");
            order = new Order(request.getProductName(), request.getQuantity(), request.getTotalPrice());
            order.setId(orderId);
            order.setSagaId(sagaId);
            order.setStatus("CREATED");
            order.setStep1CreateOrder(step1Response);
            orderRepository.save(order);
            executedSteps.add("CREATE_ORDER");
            saveLog(sagaId, SagaStep.CREATE_ORDER, SagaStatus.ORDER_CREATED,
                    "Order created: " + orderId);
            log.info("Saga [{}]   ✓ STEP 1 SUCCESS - Order created: {}", sagaId, orderId);
            log.info("Saga [{}]   → Response: {}", sagaId, step1Response);
        } catch (Exception e) {
            saveLog(sagaId, SagaStep.CREATE_ORDER, SagaStatus.FAILED,
                    "Failed to create order: " + e.getMessage());
            log.error("Saga [{}]   ✗ STEP 1 FAILED - {}", sagaId, e.getMessage());
            log.error("Saga [{}]   → Order creation failed, no steps to rollback", sagaId);
            log.info("Saga [{}] ───────────────────────────────────", sagaId);
            log.info("Saga [{}] RESULT: FAILED (Order creation failed)", sagaId);
            log.info("Saga [{}] ───────────────────────────────────", sagaId);
            return buildResponse(sagaId, null, SagaStatus.FAILED,
                    "Order creation failed: " + e.getMessage(),
                    executedSteps, null, null, null);
        }

        // ══════════════════════════════════════════════════════════════
        // STEP 2: PROCESS PAYMENT
        // ══════════════════════════════════════════════════════════════
        log.info("Saga [{}] ───────────────────────────────────", sagaId);
        log.info("Saga [{}] STEP 2/3 - PROCESS PAYMENT", sagaId);
        log.info("Saga [{}]   → Calling: POST {}/mock2/payment", sagaId, paymentServiceUrl);
        log.info("Saga [{}]   → Request: orderId={}, amount={}", sagaId, orderId, request.getTotalPrice());
        try {
            step2Response = callProcessPayment(orderId, request);
            executedSteps.add("PROCESS_PAYMENT");
            order.setStatus("PAID");
            order.setStep2ProcessPayment(step2Response);
            orderRepository.save(order);
            saveLog(sagaId, SagaStep.PROCESS_PAYMENT, SagaStatus.PAYMENT_PROCESSED,
                    "Payment processed for order: " + orderId);
            log.info("Saga [{}]   ✓ STEP 2 SUCCESS - Payment processed: {}", sagaId, orderId);
            log.info("Saga [{}]   → Response: {}", sagaId, step2Response);
        } catch (Exception e) {
            saveLog(sagaId, SagaStep.PROCESS_PAYMENT, SagaStatus.FAILED,
                    "Payment failed: " + e.getMessage());
            log.error("Saga [{}]   ✗ STEP 2 FAILED - {}", sagaId, e.getMessage());
            log.warn("Saga [{}] ───────────────────────────────────", sagaId);
            log.warn("Saga [{}] COMPENSATION REQUIRED", sagaId);
            log.warn("Saga [{}]   → Steps to rollback: {}", sagaId, executedSteps);
            log.warn("Saga [{}]   → Rollback ORDER: DELETE {}/mock1/order/{}", sagaId, orderServiceUrl, orderId);
            log.warn("Saga [{}] ───────────────────────────────────", sagaId);
            compensate(sagaId, executedSteps, orderId, order);
            log.info("Saga [{}] ───────────────────────────────────", sagaId);
            log.info("Saga [{}] RESULT: COMPENSATED (Payment failed, order rolled back)", sagaId);
            log.info("Saga [{}] ───────────────────────────────────", sagaId);
            return buildResponse(sagaId, orderId, SagaStatus.COMPENSATED,
                    "Payment failed, saga compensated: " + e.getMessage(),
                    executedSteps, step1Response, null, null);
        }

        // ══════════════════════════════════════════════════════════════
        // STEP 3: RESERVE INVENTORY
        // ══════════════════════════════════════════════════════════════
        log.info("Saga [{}] ───────────────────────────────────", sagaId);
        log.info("Saga [{}] STEP 3/3 - RESERVE INVENTORY", sagaId);
        log.info("Saga [{}]   → Calling: POST {}/mock3/inventory", sagaId, inventoryServiceUrl);
        log.info("Saga [{}]   → Request: orderId={}, productName={}, quantity={}",
                sagaId, orderId, request.getProductName(), request.getQuantity());
        try {
            step3Response = callReserveInventory(orderId, request);
            executedSteps.add("RESERVE_INVENTORY");
            order.setStatus("COMPLETED");
            order.setStep3ReserveInventory(step3Response);
            orderRepository.save(order);
            saveLog(sagaId, SagaStep.RESERVE_INVENTORY, SagaStatus.INVENTORY_RESERVED,
                    "Inventory reserved for order: " + orderId);
            log.info("Saga [{}]   ✓ STEP 3 SUCCESS - Inventory reserved: {}", sagaId, orderId);
            log.info("Saga [{}]   → Response: {}", sagaId, step3Response);
        } catch (Exception e) {
            saveLog(sagaId, SagaStep.RESERVE_INVENTORY, SagaStatus.FAILED,
                    "Inventory reservation failed: " + e.getMessage());
            log.error("Saga [{}]   ✗ STEP 3 FAILED - {}", sagaId, e.getMessage());
            log.warn("Saga [{}] ───────────────────────────────────", sagaId);
            log.warn("Saga [{}] COMPENSATION REQUIRED", sagaId);
            log.warn("Saga [{}]   → Steps to rollback: {}", sagaId, executedSteps);
            log.warn("Saga [{}]   → Rollback PAYMENT: POST {}/mock2/payment/refund/{}", sagaId, paymentServiceUrl, orderId);
            log.warn("Saga [{}]   → Rollback ORDER:   DELETE {}/mock1/order/{}", sagaId, orderServiceUrl, orderId);
            log.warn("Saga [{}] ───────────────────────────────────", sagaId);
            compensate(sagaId, executedSteps, orderId, order);
            log.info("Saga [{}] ───────────────────────────────────", sagaId);
            log.info("Saga [{}] RESULT: COMPENSATED (Inventory failed, payment & order rolled back)", sagaId);
            log.info("Saga [{}] ───────────────────────────────────", sagaId);
            return buildResponse(sagaId, orderId, SagaStatus.COMPENSATED,
                    "Inventory failed, saga compensated: " + e.getMessage(),
                    executedSteps, step1Response, step2Response, null);
        }

        // ══════════════════════════════════════════════════════════════
        // ALL STEPS SUCCESSFUL
        // ══════════════════════════════════════════════════════════════
        saveLog(sagaId, SagaStep.RESERVE_INVENTORY, SagaStatus.COMPLETED,
                "Saga completed successfully");
        log.info("Saga [{}] ───────────────────────────────────", sagaId);
        log.info("Saga [{}] ✓ ALL STEPS COMPLETED SUCCESSFULLY", sagaId);
        log.info("Saga [{}]   → Order: {} | Status: COMPLETED", sagaId, orderId);
        log.info("Saga [{}] RESULT: SUCCESS", sagaId);
        log.info("Saga [{}] ───────────────────────────────────", sagaId);

        return buildResponse(sagaId, orderId, SagaStatus.COMPLETED,
                "Order placed successfully",
                executedSteps, step1Response, step2Response, step3Response);
    }

    private void compensate(String sagaId, List<String> executedSteps, String orderId, Order order) {
        log.warn("Saga [{}] ══════════════════════════════════════", sagaId);
        log.warn("Saga [{}] COMPENSATION STARTED", sagaId);
        log.warn("Saga [{}]   → Order ID: {}", sagaId, orderId);
        log.warn("Saga [{}]   → Steps to rollback: {}", sagaId, executedSteps);
        log.warn("Saga [{}] ══════════════════════════════════════", sagaId);
        saveLog(sagaId, null, SagaStatus.COMPENSATING, "Compensation started for steps: " + executedSteps);

        if (order != null) {
            order.setStatus("CANCELLED");
            orderRepository.save(order);
            log.warn("Saga [{}]   → Order {} status updated to CANCELLED in database", sagaId, orderId);
        }

        int rollbackCount = 0;
        for (int i = executedSteps.size() - 1; i >= 0; i--) {
            String step = executedSteps.get(i);
            rollbackCount++;
            log.warn("Saga [{}] Rollback {}/{}: {}", sagaId, rollbackCount, executedSteps.size(), step);

            try {
                switch (step) {
                    case "PROCESS_PAYMENT" -> {
                        log.warn("Saga [{}]   → Calling: POST {}/mock2/payment/refund/{}",
                                sagaId, paymentServiceUrl, orderId);
                        callRefundPayment(orderId);
                        saveLog(sagaId, SagaStep.REFUND_PAYMENT, SagaStatus.COMPENSATED,
                                "Payment refunded for order: " + orderId);
                        log.info("Saga [{}]   ✓ Rollback SUCCESS - Payment refunded for order: {}", sagaId, orderId);
                    }
                    case "CREATE_ORDER" -> {
                        log.warn("Saga [{}]   → Calling: DELETE {}/mock1/order/{}",
                                sagaId, orderServiceUrl, orderId);
                        callCancelOrder(orderId);
                        saveLog(sagaId, SagaStep.CANCEL_ORDER, SagaStatus.COMPENSATED,
                                "Order cancelled: " + orderId);
                        log.info("Saga [{}]   ✓ Rollback SUCCESS - Order cancelled: {}", sagaId, orderId);
                    }
                    default -> log.info("Saga [{}]   → No compensation needed for step: {}", sagaId, step);
                }
            } catch (Exception e) {
                log.error("Saga [{}]   ✗ Rollback FAILED for step {} - {}", sagaId, step, e.getMessage());
                saveLog(sagaId, null, SagaStatus.FAILED,
                        "Compensation failed for " + step + ": " + e.getMessage());
            }
        }

        saveLog(sagaId, null, SagaStatus.COMPENSATED, "Compensation completed");
        log.warn("Saga [{}] ══════════════════════════════════════", sagaId);
        log.warn("Saga [{}] COMPENSATION COMPLETED - {} steps rolled back", sagaId, rollbackCount);
        log.warn("Saga [{}] ══════════════════════════════════════", sagaId);
    }

    // ─── HTTP calls to mock microservices ───

    @SuppressWarnings("unchecked")
    private Map<String, Object> callCreateOrder(OrderRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("productName", request.getProductName());
        body.put("quantity", request.getQuantity());
        body.put("totalPrice", request.getTotalPrice());
        body.put("forceError", request.getForceError());

        Map<String, Object> result = webClient.post()
                .uri(orderServiceUrl + "/mock1/order")
                .bodyValue(body)
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
        body.put("forceError", request.getForceError());

        Map<String, Object> result = webClient.post()
                .uri(paymentServiceUrl + "/mock2/payment")
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
        body.put("forceError", request.getForceError());

        Map<String, Object> result = webClient.post()
                .uri(inventoryServiceUrl + "/mock3/inventory")
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
                .uri(paymentServiceUrl + "/mock2/payment/refund/" + orderId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    private void callCancelOrder(String orderId) {
        webClient.delete()
                .uri(orderServiceUrl + "/mock1/order/" + orderId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    // Save log to database
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
