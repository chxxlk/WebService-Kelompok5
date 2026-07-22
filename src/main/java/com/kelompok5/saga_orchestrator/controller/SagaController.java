package com.kelompok5.saga_orchestrator.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kelompok5.saga_orchestrator.dto.SagaResponse;
import com.kelompok5.saga_orchestrator.dto.StepResult;
import com.kelompok5.saga_orchestrator.service.OrchestratorService;

@RestController
public class SagaController {

    private final OrchestratorService orchestratorService;

    public SagaController(OrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @GetMapping("/saga/run")
    public ResponseEntity<SagaResponse> run(
            @RequestParam(required = false) Integer failStep) {
        SagaResponse result = orchestratorService.executeSaga(failStep);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/saga/step1")
    public ResponseEntity<StepResult> step1() {
        StepResult result = orchestratorService.executeStep1();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/saga/step2")
    public ResponseEntity<StepResult> step2() {
        StepResult result = orchestratorService.executeStep2();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/saga/step3")
    public ResponseEntity<StepResult> step3() {
        StepResult result = orchestratorService.executeStep3();
        return ResponseEntity.ok(result);
    }
}
