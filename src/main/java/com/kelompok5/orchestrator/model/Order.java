package com.kelompok5.orchestrator.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "orders")
public class Order {

    @Id
    private String id;
    private String sagaId;
    private String productName;
    private int quantity;
    private BigDecimal totalPrice;
    private String status;
    private Map<String, Object> step1CreateOrder;
    private Map<String, Object> step2ProcessPayment;
    private Map<String, Object> step3ReserveInventory;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Order() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Order(String productName, int quantity, BigDecimal totalPrice) {
        this();
        this.productName = productName;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.status = "CREATED";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSagaId() {
        return sagaId;
    }

    public void setSagaId(String sagaId) {
        this.sagaId = sagaId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, Object> getStep1CreateOrder() {
        return step1CreateOrder;
    }

    public void setStep1CreateOrder(Map<String, Object> step1CreateOrder) {
        this.step1CreateOrder = step1CreateOrder;
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, Object> getStep2ProcessPayment() {
        return step2ProcessPayment;
    }

    public void setStep2ProcessPayment(Map<String, Object> step2ProcessPayment) {
        this.step2ProcessPayment = step2ProcessPayment;
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, Object> getStep3ReserveInventory() {
        return step3ReserveInventory;
    }

    public void setStep3ReserveInventory(Map<String, Object> step3ReserveInventory) {
        this.step3ReserveInventory = step3ReserveInventory;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
