package com.kelompok5.orchestrator.dto;

import java.math.BigDecimal;

public class OrderRequest {

    private String productName;
    private int quantity;
    private BigDecimal totalPrice;

    public OrderRequest() {
    }

    public OrderRequest(String productName, int quantity, BigDecimal totalPrice) {
        this.productName = productName;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
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
}
