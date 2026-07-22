package com.kelompok5.orchestrator.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/mock3")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);
    private static final String COLLECTION = "mock3_inventory";

    private final Map<String, Map<String, Object>> reservations = new LinkedHashMap<>();
    private final MongoTemplate mongoTemplate;

    public InventoryController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostMapping("/inventory")
    public ResponseEntity<Map<String, Object>> reserveInventory(@RequestBody Map<String, Object> body) {
        String orderId = (String) body.get("orderId");
        Map<String, Object> response = new HashMap<>();

        if (Boolean.TRUE.equals(body.get("forceError"))) {
            log.error("[Inventory Service] Forced error for inventory reservation");
            response.put("status", "ERROR");
            response.put("message", "Inventory reservation failed (forced)");
            saveToDb(body, response, "ERROR");
            return ResponseEntity.internalServerError().body(response);
        }

        String reservationId = UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> reservation = new HashMap<>();
        reservation.put("reservationId", reservationId);
        reservation.put("orderId", orderId);
        reservation.put("productName", body.get("productName"));
        reservation.put("quantity", body.get("quantity"));
        reservation.put("status", "RESERVED");
        reservations.put(orderId, reservation);

        log.info("[Inventory Service] Inventory reserved: {} for order: {}", reservationId, orderId);

        response.put("reservationId", reservationId);
        response.put("status", "SUCCESS");
        response.put("message", "Inventory reserved successfully");
        saveToDb(body, response, "SUCCESS");

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/inventory/{orderId}")
    public ResponseEntity<Map<String, Object>> releaseInventory(@PathVariable String orderId) {
        reservations.remove(orderId);

        log.info("[Inventory Service] Inventory released for order: {}", orderId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Inventory released for order: " + orderId);
        saveToDb(Map.of("action", "release", "orderId", orderId), response, "SUCCESS");

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
