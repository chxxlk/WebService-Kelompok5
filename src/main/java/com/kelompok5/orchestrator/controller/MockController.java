package com.kelompok5.orchestrator.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/")
public class MockController {

    @GetMapping("/mock1")
    public ResponseEntity<Map<String, Object>> mock1() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("step", "Mock 1 Endpoint");
        response.put("message", "Data from Mock 1 executed successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mock2")
    public ResponseEntity<Map<String, Object>> mock2() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("step", "Mock 2 Endpoint");
        response.put("message", "Data from Mock 2 executed successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mock3")
    public ResponseEntity<Map<String, Object>> mock3() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("step", "Mock 3 Endpoint");
        response.put("message", "Data from Mock 3 executed successfully");
        return ResponseEntity.ok(response);
    }
}
