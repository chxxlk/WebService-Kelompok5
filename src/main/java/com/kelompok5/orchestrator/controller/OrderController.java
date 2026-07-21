package com.kelompok5.orchestrator.controller;

import com.kelompok5.orchestrator.dto.OrderRequest;
import com.kelompok5.orchestrator.dto.OrderResponse;
import com.kelompok5.orchestrator.service.OrderSagaOrchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderSagaOrchestrator orchestrator;

    public OrderController(OrderSagaOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody OrderRequest request) {
        OrderResponse response = orchestrator.execute(request);
        return ResponseEntity.ok(response);
    }
}
