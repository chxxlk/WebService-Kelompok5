package com.kelompok5.orchestrator.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/mock3")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    private final Map<String, Map<String, Object>> reservations = new LinkedHashMap<>();

    @PostMapping("/inventory")
    public ResponseEntity<Map<String, Object>> reserveInventory(@RequestBody Map<String, Object> body) {
        String orderId = (String) body.get("orderId");
        String reservationId = UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> reservation = new HashMap<>();
        reservation.put("reservationId", reservationId);
        reservation.put("orderId", orderId);
        reservation.put("productName", body.get("productName"));
        reservation.put("quantity", body.get("quantity"));
        reservation.put("status", "RESERVED");
        reservations.put(orderId, reservation);

        log.info("[Inventory Service] Inventory reserved: {} for order: {}", reservationId, orderId);

        Map<String, Object> response = new HashMap<>();
        response.put("reservationId", reservationId);
        response.put("status", "SUCCESS");
        response.put("message", "Inventory reserved successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/inventory/{orderId}")
    public ResponseEntity<Map<String, Object>> releaseInventory(@PathVariable String orderId) {
        reservations.remove(orderId);

        log.info("[Inventory Service] Inventory released for order: {}", orderId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Inventory released for order: " + orderId);
        return ResponseEntity.ok(response);
    }
}
