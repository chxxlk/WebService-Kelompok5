package com.kelompok5.saga_orchestrator.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.kelompok5.saga_orchestrator.dto.CompensationResult;
import com.kelompok5.saga_orchestrator.dto.SagaResponse;
import com.kelompok5.saga_orchestrator.dto.StepResult;
import com.kelompok5.saga_orchestrator.model.SagaLog;
import com.kelompok5.saga_orchestrator.repository.SagaLogRepository;

@Service
public class OrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorService.class);
    private static final String STEP = "STEP";
    private static final String COMPENSATION = "COMPENSATION";

    private final WebClientService webClientService;
    private final SagaLogRepository sagaLogRepository;

    public OrchestratorService(WebClientService webClientService, SagaLogRepository sagaLogRepository) {
        this.webClientService = webClientService;
        this.sagaLogRepository = sagaLogRepository;
    }

    public StepResult executeStep1() {
        return executeStep(null, 1, "order-service", false, () -> webClientService.getMock1());
    }

    public StepResult executeStep2() {
        return executeStep(null, 2, "payment-service", false, () -> webClientService.getMock2());
    }

    public StepResult executeStep3() {
        return executeStep(null, 3, "inventory-service", false, () -> webClientService.getMock3());
    }

    public SagaResponse executeSaga() {
        return executeSaga(null);
    }

    @SuppressWarnings("unchecked")
    public SagaResponse executeSaga(Integer failStep) {
        String sagaId = UUID.randomUUID().toString();
        SagaResponse response = new SagaResponse();
        response.setSagaId(sagaId);
        response.setTimestamp(LocalDateTime.now());
        List<CompensationResult> compensations = new ArrayList<>();

        log.info("[Saga][{}] ========== Starting Saga Orchestration ==========", sagaId);

        // --- Step 1: Order ---
        boolean fail1 = failStep != null && failStep == 1;
        StepResult orderResult = executeStep(sagaId, 1, "order-service", fail1, () -> webClientService.getMock1(fail1));
        response.setOrder(orderResult);
        if ("failed".equals(orderResult.getStatus())) {
            log.error("[Saga][{}] Step 1 failed — cannot proceed, saga aborted", sagaId);
            response.setOverallStatus("FAILED");
            response.setCompensations(Collections.emptyList());
            return response;
        }

        // --- Step 2: Payment ---
        boolean fail2 = failStep != null && failStep == 2;
        StepResult paymentResult = executeStep(sagaId, 2, "payment-service", fail2, () -> webClientService.getMock2(fail2));
        response.setPayment(paymentResult);
        if ("failed".equals(paymentResult.getStatus())) {
            log.error("[Saga][{}] Step 2 (payment) failed — initiating compensation", sagaId);
            log.info("[Saga][{}] Rolling back Step 1: cancelling order...", sagaId);
            compensations.add(compensateStep(sagaId, 1, "order-service", "cancel", () -> webClientService.cancelOrder()));
            response.setOverallStatus("COMPENSATED");
            response.setCompensations(compensations);
            log.info("[Saga][{}] ========== Payment Failed — Compensation Completed ==========", sagaId);
            return response;
        }

        // --- Step 3: Inventory ---
        boolean fail3 = failStep != null && failStep == 3;
        StepResult inventoryResult = executeStep(sagaId, 3, "inventory-service", fail3, () -> webClientService.getMock3(fail3));
        response.setInventory(inventoryResult);
        if ("failed".equals(inventoryResult.getStatus())) {
            log.error("[Saga][{}] Step 3 (inventory) failed — initiating compensation", sagaId);
            log.info("[Saga][{}] Rolling back Step 2: refunding payment...", sagaId);
            compensations.add(compensateStep(sagaId, 2, "payment-service", "refund", () -> webClientService.refundPayment()));
            log.info("[Saga][{}] Rolling back Step 1: cancelling order...", sagaId);
            compensations.add(compensateStep(sagaId, 1, "order-service", "cancel", () -> webClientService.cancelOrder()));
            response.setOverallStatus("COMPENSATED");
            response.setCompensations(compensations);
            log.info("[Saga][{}] ========== Inventory Failed — Compensation Completed ==========", sagaId);
            return response;
        }

        log.info("[Saga][{}] ========== Saga Completed Successfully ==========", sagaId);
        response.setOverallStatus("SUCCESS");
        response.setCompensations(Collections.emptyList());
        return response;
    }

    @SuppressWarnings("unchecked")
    private StepResult executeStep(String sagaId, int step, String service, boolean fail, StepCaller caller) {
        log.info("[Saga][Step {}] Calling mock{} ({})...", step, step, service);
        try {
            Map<String, Object> response = caller.call();
            log.info("[Saga][Step {}] Response: {}", step, response);

            String status = response != null ? String.valueOf(response.get("status")) : "failed";
            Map<String, Object> data = response != null ? (Map<String, Object>) response.get("data") : null;

            SagaLog sagaLog = new SagaLog(sagaId, step, STEP, service, status, response.toString());
            sagaLogRepository.save(sagaLog);
            log.info("[Saga][Step {}] Saved to MongoDB (id={})", step, sagaLog.getId());

            return new StepResult(service, status, data);
        } catch (Exception e) {
            log.error("[Saga][Step {}] ERROR: {}", step, e.getMessage());
            SagaLog sagaLog = new SagaLog(sagaId, step, STEP, service, "failed", e.getMessage());
            sagaLogRepository.save(sagaLog);
            return new StepResult(service, "failed", Collections.singletonMap("error", e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private CompensationResult compensateStep(String sagaId, int step, String service, String action, StepCaller caller) {
        log.info("[Saga][Compensation][Step {}] {}ing...", step, action);
        try {
            Map<String, Object> response = caller.call();
            log.info("[Saga][Compensation][Step {}] Response: {}", step, response);

            String status = response != null ? String.valueOf(response.get("status")) : "failed";

            SagaLog sagaLog = new SagaLog(sagaId, step, COMPENSATION, service, status, response.toString());
            sagaLogRepository.save(sagaLog);
            log.info("[Saga][Compensation][Step {}] Saved to MongoDB (id={})", step, sagaLog.getId());

            return new CompensationResult(step, service, action, status);
        } catch (Exception e) {
            log.error("[Saga][Compensation][Step {}] ERROR: {}", step, e.getMessage());
            SagaLog sagaLog = new SagaLog(sagaId, step, COMPENSATION, service, "failed", e.getMessage());
            sagaLogRepository.save(sagaLog);
            return new CompensationResult(step, service, action, "failed");
        }
    }

    @FunctionalInterface
    private interface StepCaller {
        Map<String, Object> call();
    }
}
