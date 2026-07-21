package com.kelompok5.orchestrator.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/mock2")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final Map<String, Map<String, Object>> payments = new LinkedHashMap<>();

    @PostMapping("/payment")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody Map<String, Object> body) {
        String orderId = (String) body.get("orderId");
        String paymentId = UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> payment = new HashMap<>();
        payment.put("paymentId", paymentId);
        payment.put("orderId", orderId);
        payment.put("amount", body.get("amount"));
        payment.put("status", "PAID");
        payments.put(orderId, payment);

        log.info("[Payment Service] Payment processed: {} for order: {}", paymentId, orderId);

        Map<String, Object> response = new HashMap<>();
        response.put("paymentId", paymentId);
        response.put("status", "SUCCESS");
        response.put("message", "Payment processed successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/payment/refund/{orderId}")
    public ResponseEntity<Map<String, Object>> refundPayment(@PathVariable String orderId) {
        payments.remove(orderId);

        log.info("[Payment Service] Payment refunded for order: {}", orderId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Payment refunded for order: " + orderId);
        return ResponseEntity.ok(response);
    }
}
