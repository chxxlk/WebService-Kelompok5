package com.kelompok5.orchestrator.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/mock1")
public class MockController {

    private static final Logger log = LoggerFactory.getLogger(MockController.class);
    private static final String COLLECTION = "mock1_order";

    private final Map<String, Map<String, Object>> orders = new LinkedHashMap<>();
    private final MongoTemplate mongoTemplate;

    public MockController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostMapping("/order")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();

        if (Boolean.TRUE.equals(body.get("forceError"))) {
            log.error("[Order Service] Forced error for order creation");
            response.put("status", "ERROR");
            response.put("message", "Order creation failed (forced)");
            saveToDb(body, response, "ERROR");
            return ResponseEntity.internalServerError().body(response);
        }

        String orderId = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> order = new HashMap<>(body);
        order.put("orderId", orderId);
        order.put("status", "CREATED");
        orders.put(orderId, order);

        log.info("[Order Service] Order created: {}", orderId);

        response.put("orderId", orderId);
        response.put("status", "SUCCESS");
        response.put("message", "Order created successfully");
        saveToDb(body, response, "SUCCESS");

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/order/{orderId}")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable String orderId) {
        orders.remove(orderId);

        log.info("[Order Service] Order cancelled: {}", orderId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Order cancelled: " + orderId);
        saveToDb(Map.of("action", "cancel", "orderId", orderId), response, "SUCCESS");

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
