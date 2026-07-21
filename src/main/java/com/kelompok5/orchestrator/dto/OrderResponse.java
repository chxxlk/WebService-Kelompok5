package com.kelompok5.orchestrator.dto;

import com.kelompok5.orchestrator.model.SagaStatus;

import java.util.List;

public class OrderResponse {

    private String sagaId;
    private String orderId;
    private SagaStatus status;
    private String message;
    private List<String> executedSteps;

    public OrderResponse() {
    }

    public OrderResponse(String sagaId, String orderId, SagaStatus status, String message, List<String> executedSteps) {
        this.sagaId = sagaId;
        this.orderId = orderId;
        this.status = status;
        this.message = message;
        this.executedSteps = executedSteps;
    }

    public String getSagaId() {
        return sagaId;
    }

    public void setSagaId(String sagaId) {
        this.sagaId = sagaId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public SagaStatus getStatus() {
        return status;
    }

    public void setStatus(SagaStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getExecutedSteps() {
        return executedSteps;
    }

    public void setExecutedSteps(List<String> executedSteps) {
        this.executedSteps = executedSteps;
    }
}
