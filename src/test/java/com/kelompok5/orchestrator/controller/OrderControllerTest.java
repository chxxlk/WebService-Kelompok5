package com.kelompok5.orchestrator.controller;

import com.kelompok5.orchestrator.dto.OrderRequest;
import com.kelompok5.orchestrator.dto.OrderResponse;
import com.kelompok5.orchestrator.model.SagaStatus;
import com.kelompok5.orchestrator.service.OrderSagaOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderSagaOrchestrator orchestrator;

    @Test
    void placeOrderReturnsAggregatedResponse() throws Exception {
        OrderResponse mockResponse = new OrderResponse();
        mockResponse.setSagaId("saga-001");
        mockResponse.setOrderId("order-001");
        mockResponse.setStatus(SagaStatus.COMPLETED);
        mockResponse.setMessage("Order placed successfully");
        mockResponse.setExecutedSteps(List.of("CREATE_ORDER", "PROCESS_PAYMENT", "RESERVE_INVENTORY"));
        mockResponse.setStep1CreateOrder(Map.of("orderId", "order-001", "status", "SUCCESS"));
        mockResponse.setStep2ProcessPayment(Map.of("paymentId", "pay-001", "status", "SUCCESS"));
        mockResponse.setStep3ReserveInventory(Map.of("reservationId", "inv-001", "status", "SUCCESS"));

        when(orchestrator.execute(any(OrderRequest.class))).thenReturn(mockResponse);

        String body = """
                {
                    "productName": "Laptop ASUS",
                    "quantity": 1,
                    "totalPrice": 12000000
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sagaId").value("saga-001"))
                .andExpect(jsonPath("$.orderId").value("order-001"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.message").value("Order placed successfully"))
                .andExpect(jsonPath("$.executedSteps").isArray())
                .andExpect(jsonPath("$.executedSteps.length()").value(3))
                .andExpect(jsonPath("$.step1CreateOrder.status").value("SUCCESS"))
                .andExpect(jsonPath("$.step2ProcessPayment.status").value("SUCCESS"))
                .andExpect(jsonPath("$.step3ReserveInventory.status").value("SUCCESS"));
    }
}
