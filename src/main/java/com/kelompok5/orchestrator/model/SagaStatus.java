package com.kelompok5.orchestrator.model;

public enum SagaStatus {
    STARTED,
    ORDER_CREATED,
    PAYMENT_PROCESSED,
    INVENTORY_RESERVED,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED
}
