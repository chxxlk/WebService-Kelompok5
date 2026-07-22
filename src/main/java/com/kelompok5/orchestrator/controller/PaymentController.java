package com.kelompok5.orchestrator.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/mock2")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private static final String COLLECTION = "mock2_payment";

    private final Map<String, Map<String, Object>> payments = new LinkedHashMap<>();
    private final MongoTemplate mongoTemplate;

    public PaymentController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostMapping("/payment")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody Map<String, Object> body) {
        String orderId = (String) body.get("orderId");
        Map<String, Object> response = new HashMap<>();

        if (Boolean.TRUE.equals(body.get("forceError"))) {
            log.error("[Payment Service] Forced error for payment processing");
            response.put("status", "ERROR");
            response.put("message", "Payment processing failed (forced)");
            saveToDb(body, response, "ERROR");
            return ResponseEntity.internalServerError().body(response);
        }

        String paymentId = UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> payment = new HashMap<>();
        payment.put("paymentId", paymentId);
        payment.put("orderId", orderId);
        payment.put("amount", body.get("amount"));
        payment.put("status", "PAID");
        payments.put(orderId, payment);

        log.info("[Payment Service] Payment processed: {} for order: {}", paymentId, orderId);

        response.put("paymentId", paymentId);
        response.put("status", "SUCCESS");
        response.put("message", "Payment processed successfully");
        saveToDb(body, response, "SUCCESS");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/payment/refund/{orderId}")
    public ResponseEntity<Map<String, Object>> refundPayment(@PathVariable String orderId) {
        payments.remove(orderId);

        log.info("[Payment Service] Payment refunded for order: {}", orderId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Payment refunded for order: " + orderId);
        saveToDb(Map.of("action", "refund", "orderId", orderId), response, "SUCCESS");

        return ResponseEntity.ok(response);
    }

    private void saveToDb(Map<String, Object> request, Map<String, Object> response, String status) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("request", request);
        doc.put("response", response);
        doc.put("status", status);
        doc.put("timestamp", new Date());
        mongoTemplate.save(doc, COLLECTION);
    }
}
