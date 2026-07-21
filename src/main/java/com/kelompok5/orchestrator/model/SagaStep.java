package com.kelompok5.orchestrator.model;

public enum SagaStep {
    CREATE_ORDER,
    PROCESS_PAYMENT,
    RESERVE_INVENTORY,
    CANCEL_ORDER,
    REFUND_PAYMENT,
    RELEASE_INVENTORY
}
