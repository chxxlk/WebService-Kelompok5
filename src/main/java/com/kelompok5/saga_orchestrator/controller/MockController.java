package com.kelompok5.saga_orchestrator.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MockController {

    @GetMapping("/mock1")
    public ResponseEntity<Map<String, Object>> mock1(
            @RequestParam(required = false, defaultValue = "false") boolean fail) {
        if (fail) {
            return ResponseEntity.ok(Map.of(
                    "service", "order-service",
                    "status", "failed",
                    "data", Map.of("error", "Order creation failed")
            ));
        }
        return ResponseEntity.ok(Map.of(
                "service", "order-service",
                "status", "success",
                "data", Map.of(
                        "orderId", "ORD-001",
                        "amount", 150000,
                        "currency", "IDR"
                )
        ));
    }

    @GetMapping("/mock2")
    public ResponseEntity<Map<String, Object>> mock2(
            @RequestParam(required = false, defaultValue = "false") boolean fail) {
        if (fail) {
            return ResponseEntity.ok(Map.of(
                    "service", "payment-service",
                    "status", "failed",
                    "data", Map.of("error", "Payment processing failed")
            ));
        }
        return ResponseEntity.ok(Map.of(
                "service", "payment-service",
                "status", "success",
                "data", Map.of(
                        "paymentId", "PAY-001",
                        "method", "credit_card",
                        "amount", 150000
                )
        ));
    }

    @GetMapping("/mock3")
    public ResponseEntity<Map<String, Object>> mock3(
            @RequestParam(required = false, defaultValue = "false") boolean fail) {
        if (fail) {
            return ResponseEntity.ok(Map.of(
                    "service", "inventory-service",
                    "status", "failed",
                    "data", Map.of("error", "Stock not available")
            ));
        }
        return ResponseEntity.ok(Map.of(
                "service", "inventory-service",
                "status", "success",
                "data", Map.of(
                        "sku", "ITEM-001",
                        "stock", 50,
                        "warehouse", "WH-JKT"
                )
        ));
    }

    @PostMapping("/mock1/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder() {
        return ResponseEntity.ok(Map.of(
                "service", "order-service",
                "action", "cancel",
                "status", "cancelled"
        ));
    }

    @PostMapping("/mock2/refund")
    public ResponseEntity<Map<String, Object>> refundPayment() {
        return ResponseEntity.ok(Map.of(
                "service", "payment-service",
                "action", "refund",
                "status", "refunded"
        ));
    }

    @PostMapping("/mock3/restock")
    public ResponseEntity<Map<String, Object>> restockInventory() {
        return ResponseEntity.ok(Map.of(
                "service", "inventory-service",
                "action", "restock",
                "status", "restocked"
        ));
    }
}
