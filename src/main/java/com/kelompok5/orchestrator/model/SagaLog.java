package com.kelompok5.orchestrator.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "saga_logs")
public class SagaLog {

    @Id
    private String id;
    private String sagaId;
    private SagaStep step;
    private SagaStatus status;
    private String message;
    private LocalDateTime timestamp;

    public SagaLog() {
        this.timestamp = LocalDateTime.now();
    }

    public SagaLog(String sagaId, SagaStep step, SagaStatus status, String message) {
        this();
        this.sagaId = sagaId;
        this.step = step;
        this.status = status;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public String getSagaId() {
        return sagaId;
    }

    public void setSagaId(String sagaId) {
        this.sagaId = sagaId;
    }

    public SagaStep getStep() {
        return step;
    }

    public void setStep(SagaStep step) {
        this.step = step;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
