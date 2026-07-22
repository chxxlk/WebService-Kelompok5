package com.kelompok5.saga_orchestrator.dto;

import java.time.LocalDateTime;
import java.util.List;

public class SagaResponse {

    private String sagaId;
    private String overallStatus;
    private StepResult order;
    private StepResult payment;
    private StepResult inventory;
    private List<CompensationResult> compensations;
    private LocalDateTime timestamp;

    public SagaResponse() {
    }

    public String getSagaId() {
        return sagaId;
    }

    public void setSagaId(String sagaId) {
        this.sagaId = sagaId;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public StepResult getOrder() {
        return order;
    }

    public void setOrder(StepResult order) {
        this.order = order;
    }

    public StepResult getPayment() {
        return payment;
    }

    public void setPayment(StepResult payment) {
        this.payment = payment;
    }

    public StepResult getInventory() {
        return inventory;
    }

    public void setInventory(StepResult inventory) {
        this.inventory = inventory;
    }

    public List<CompensationResult> getCompensations() {
        return compensations;
    }

    public void setCompensations(List<CompensationResult> compensations) {
        this.compensations = compensations;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
