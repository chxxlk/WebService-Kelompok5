package com.kelompok5.orchestrator.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void processPaymentReturnsPaymentIdAndSuccess() throws Exception {
        String body = """
                {
                    "orderId": "order001",
                    "amount": 12000000
                }
                """;

        mockMvc.perform(post("/mock2/payment")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Payment processed successfully"));
    }

    @Test
    void refundPaymentReturnsSuccess() throws Exception {
        mockMvc.perform(post("/mock2/payment/refund/order001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Payment refunded for order: order001"));
    }
}
