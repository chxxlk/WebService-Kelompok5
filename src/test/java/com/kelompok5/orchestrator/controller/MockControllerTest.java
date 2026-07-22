package com.kelompok5.orchestrator.controller;

import com.kelompok5.orchestrator.repository.ServiceLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MockController.class)
class MockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceLogRepository serviceLogRepository;

    @Test
    void createOrderReturnsOrderIdAndSuccess() throws Exception {
        String body = """
                {
                    "productName": "Laptop ASUS",
                    "quantity": 1,
                    "totalPrice": 12000000
                }
                """;

        mockMvc.perform(post("/mock1/order")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Order created successfully"));
    }

    @Test
    void cancelOrderReturnsSuccess() throws Exception {
        mockMvc.perform(delete("/mock1/order/test123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Order cancelled: test123"));
    }
}
