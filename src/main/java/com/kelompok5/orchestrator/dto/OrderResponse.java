package com.kelompok5.orchestrator.dto;

import com.kelompok5.orchestrator.model.SagaStatus;

import java.util.List;
import java.util.Map;

public class OrderResponse {

    private String sagaId;
    private String orderId;
    private SagaStatus status;
    private String message;
    private List<String> executedSteps;
    private Map<String, Object> step1CreateOrder;
    private Map<String, Object> step2ProcessPayment;
    private Map<String, Object> step3ReserveInventory;

    public OrderResponse() {
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

    public Map<String, Object> getStep1CreateOrder() {
        return step1CreateOrder;
    }

    public void setStep1CreateOrder(Map<String, Object> step1CreateOrder) {
        this.step1CreateOrder = step1CreateOrder;
    }

    public Map<String, Object> getStep2ProcessPayment() {
        return step2ProcessPayment;
    }

    public void setStep2ProcessPayment(Map<String, Object> step2ProcessPayment) {
        this.step2ProcessPayment = step2ProcessPayment;
    }

    public Map<String, Object> getStep3ReserveInventory() {
        return step3ReserveInventory;
    }

    public void setStep3ReserveInventory(Map<String, Object> step3ReserveInventory) {
        this.step3ReserveInventory = step3ReserveInventory;
    }
}
