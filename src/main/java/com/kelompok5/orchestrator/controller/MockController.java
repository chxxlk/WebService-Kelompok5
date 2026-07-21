package com.kelompok5.orchestrator.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/mock1")
public class MockController {

    private static final Logger log = LoggerFactory.getLogger(MockController.class);

    private final Map<String, Map<String, Object>> orders = new LinkedHashMap<>();

    @PostMapping("/order")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> body) {
        String orderId = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> order = new HashMap<>(body);
        order.put("orderId", orderId);
        order.put("status", "CREATED");
        orders.put(orderId, order);

        log.info("[Order Service] Order created: {}", orderId);

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("status", "SUCCESS");
        response.put("message", "Order created successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/order/{orderId}")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable String orderId) {
        orders.remove(orderId);

        log.info("[Order Service] Order cancelled: {}", orderId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Order cancelled: " + orderId);
        return ResponseEntity.ok(response);
    }
}
