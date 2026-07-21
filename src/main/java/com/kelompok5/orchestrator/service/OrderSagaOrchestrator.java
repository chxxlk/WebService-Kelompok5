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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    public OrderResponse execute(OrderRequest request) {
        String sagaId = UUID.randomUUID().toString();
        List<String> executedSteps = new ArrayList<>();
        String orderId = null;

        log.info("Saga [{}] started for product: {}", sagaId, request.getProductName());

        // STEP 1: Create Order
        try {
            orderId = callCreateOrder(request);
            executedSteps.add("CREATE_ORDER");
            saveLog(sagaId, SagaStep.CREATE_ORDER, SagaStatus.ORDER_CREATED, "Order created: " + orderId);
            log.info("Saga [{}] Step 1 OK - Order created: {}", sagaId, orderId);
        } catch (Exception e) {
            saveLog(sagaId, SagaStep.CREATE_ORDER, SagaStatus.FAILED, "Failed to create order: " + e.getMessage());
            log.error("Saga [{}] Step 1 FAILED - {}", sagaId, e.getMessage());
            return buildResponse(sagaId, null, SagaStatus.FAILED, "Order creation failed: " + e.getMessage(), executedSteps);
        }

        // STEP 2: Process Payment
        try {
            callProcessPayment(orderId, request);
            executedSteps.add("PROCESS_PAYMENT");
            saveLog(sagaId, SagaStep.PROCESS_PAYMENT, SagaStatus.PAYMENT_PROCESSED, "Payment processed for order: " + orderId);
            log.info("Saga [{}] Step 2 OK - Payment processed", sagaId);
        } catch (Exception e) {
            saveLog(sagaId, SagaStep.PROCESS_PAYMENT, SagaStatus.FAILED, "Payment failed: " + e.getMessage());
            log.error("Saga [{}] Step 2 FAILED - {} | Starting compensation...", sagaId, e.getMessage());
            compensate(sagaId, executedSteps, orderId);
            return buildResponse(sagaId, orderId, SagaStatus.COMPENSATED, "Payment failed, saga compensated: " + e.getMessage(), executedSteps);
        }

        // STEP 3: Reserve Inventory
        try {
            callReserveInventory(orderId, request);
            executedSteps.add("RESERVE_INVENTORY");
            saveLog(sagaId, SagaStep.RESERVE_INVENTORY, SagaStatus.INVENTORY_RESERVED, "Inventory reserved for order: " + orderId);
            log.info("Saga [{}] Step 3 OK - Inventory reserved", sagaId);
        } catch (Exception e) {
            saveLog(sagaId, SagaStep.RESERVE_INVENTORY, SagaStatus.FAILED, "Inventory reservation failed: " + e.getMessage());
            log.error("Saga [{}] Step 3 FAILED - {} | Starting compensation...", sagaId, e.getMessage());
            compensate(sagaId, executedSteps, orderId);
            return buildResponse(sagaId, orderId, SagaStatus.COMPENSATED, "Inventory failed, saga compensated: " + e.getMessage(), executedSteps);
        }

        // ALL STEPS SUCCESSFUL
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.setStatus("COMPLETED");
            orderRepository.save(order);
        }

        saveLog(sagaId, SagaStep.RESERVE_INVENTORY, SagaStatus.COMPLETED, "Saga completed successfully");
        log.info("Saga [{}] COMPLETED successfully", sagaId);

        return buildResponse(sagaId, orderId, SagaStatus.COMPLETED, "Order placed successfully", executedSteps);
    }

    private void compensate(String sagaId, List<String> executedSteps, String orderId) {
        saveLog(sagaId, null, SagaStatus.COMPENSATING, "Compensation started");
        log.warn("Saga [{}] Starting compensation for steps: {}", sagaId, executedSteps);

        // Compensate in reverse order
        for (int i = executedSteps.size() - 1; i >= 0; i--) {
            String step = executedSteps.get(i);
            try {
                switch (step) {
                    case "PROCESS_PAYMENT" -> {
                        callRefundPayment(orderId);
                        saveLog(sagaId, SagaStep.REFUND_PAYMENT, SagaStatus.COMPENSATED, "Payment refunded for order: " + orderId);
                        log.info("Saga [{}] Compensation OK - Payment refunded", sagaId);
                    }
                    case "CREATE_ORDER" -> {
                        callCancelOrder(orderId);
                        saveLog(sagaId, SagaStep.CANCEL_ORDER, SagaStatus.COMPENSATED, "Order cancelled: " + orderId);
                        log.info("Saga [{}] Compensation OK - Order cancelled", sagaId);
                    }
                    default -> log.info("Saga [{}] No compensation needed for step: {}", sagaId, step);
                }
            } catch (Exception e) {
                log.error("Saga [{}] Compensation FAILED for step {}: {}", sagaId, step, e.getMessage());
                saveLog(sagaId, null, SagaStatus.FAILED, "Compensation failed for " + step + ": " + e.getMessage());
            }
        }

        saveLog(sagaId, null, SagaStatus.COMPENSATED, "Compensation completed");
    }

    // --- HTTP calls to mock microservices ---

    private String callCreateOrder(OrderRequest request) {
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> result = webClient.post()
                .uri("http://localhost:8080/mock1/order")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(java.util.Map.class)
                .block();

        if (result == null || result.get("orderId") == null) {
            throw new RuntimeException("Order service returned no orderId");
        }
        return (String) result.get("orderId");
    }

    private void callProcessPayment(String orderId, OrderRequest request) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("orderId", orderId);
        body.put("amount", request.getTotalPrice());

        webClient.post()
                .uri("http://localhost:8080/mock2/payment")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(java.util.Map.class)
                .block();
    }

    private void callReserveInventory(String orderId, OrderRequest request) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("orderId", orderId);
        body.put("productName", request.getProductName());
        body.put("quantity", request.getQuantity());

        webClient.post()
                .uri("http://localhost:8080/mock3/inventory")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(java.util.Map.class)
                .block();
    }

    private void callRefundPayment(String orderId) {
        webClient.post()
                .uri("http://localhost:8080/mock2/payment/refund/" + orderId)
                .retrieve()
                .bodyToMono(java.util.Map.class)
                .block();
    }

    private void callCancelOrder(String orderId) {
        webClient.delete()
                .uri("http://localhost:8080/mock1/order/" + orderId)
                .retrieve()
                .bodyToMono(java.util.Map.class)
                .block();
    }

    private void saveLog(String sagaId, SagaStep step, SagaStatus status, String message) {
        SagaLog sagaLog = new SagaLog(sagaId, step, status, message);
        sagaLogRepository.save(sagaLog);
    }

    private OrderResponse buildResponse(String sagaId, String orderId, SagaStatus status, String message, List<String> executedSteps) {
        return new OrderResponse(sagaId, orderId, status, message, executedSteps);
    }
}
