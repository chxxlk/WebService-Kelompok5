package com.kelompok5.orchestrator.controller;

import com.kelompok5.orchestrator.repository.ServiceLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceLogRepository serviceLogRepository;

    @Test
    void reserveInventoryReturnsReservationIdAndSuccess() throws Exception {
        String body = """
                {
                    "orderId": "order001",
                    "productName": "Laptop ASUS",
                    "quantity": 1
                }
                """;

        mockMvc.perform(post("/mock3/inventory")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Inventory reserved successfully"));
    }

    @Test
    void releaseInventoryReturnsSuccess() throws Exception {
        mockMvc.perform(delete("/mock3/inventory/order001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Inventory released for order: order001"));
    }
}
