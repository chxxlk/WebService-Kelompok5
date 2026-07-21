package com.kelompok5.orchestrator.service;

import com.kelompok5.orchestrator.dto.OrderRequest;
import com.kelompok5.orchestrator.dto.OrderResponse;
import com.kelompok5.orchestrator.model.SagaStatus;
import com.kelompok5.orchestrator.repository.OrderRepository;
import com.kelompok5.orchestrator.repository.SagaLogRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OrderSagaOrchestratorTest {

    private MockWebServer mockWebServer;
    private OrderSagaOrchestrator orchestrator;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SagaLogRepository sagaLogRepository;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("").toString().replaceAll("/$", "");
        WebClient webClient = WebClient.create();

        orchestrator = new OrderSagaOrchestrator(
                webClient, orderRepository, sagaLogRepository,
                baseUrl, baseUrl, baseUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    private OrderRequest createSampleRequest() {
        return new OrderRequest("Laptop ASUS", 1, new BigDecimal("12000000"));
    }

    @Test
    void execute_allStepsSuccess_returnsCompleted() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"orderId\":\"ord123\",\"status\":\"SUCCESS\",\"message\":\"Order created successfully\"}")
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"paymentId\":\"pay123\",\"status\":\"SUCCESS\",\"message\":\"Payment processed\"}")
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"reservationId\":\"inv123\",\"status\":\"SUCCESS\",\"message\":\"Inventory reserved\"}")
                .addHeader("Content-Type", "application/json"));

        OrderResponse result = orchestrator.execute(createSampleRequest());

        assertEquals(SagaStatus.COMPLETED, result.getStatus());
        assertEquals("Order placed successfully", result.getMessage());
        assertNotNull(result.getSagaId());
        assertEquals("ord123", result.getOrderId());
        assertEquals(3, result.getExecutedSteps().size());
        assertEquals("CREATE_ORDER", result.getExecutedSteps().get(0));
        assertEquals("PROCESS_PAYMENT", result.getExecutedSteps().get(1));
        assertEquals("RESERVE_INVENTORY", result.getExecutedSteps().get(2));
        assertNotNull(result.getStep1CreateOrder());
        assertNotNull(result.getStep2ProcessPayment());
        assertNotNull(result.getStep3ReserveInventory());
        assertEquals(3, mockWebServer.getRequestCount());
    }

    @Test
    void execute_step2Fails_compensatesAndReturnsCompensated() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"orderId\":\"ord456\",\"status\":\"SUCCESS\"}")
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("{\"error\":\"Payment service unavailable\"}"));
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"status\":\"SUCCESS\",\"message\":\"Order cancelled\"}")
                .addHeader("Content-Type", "application/json"));

        OrderResponse result = orchestrator.execute(createSampleRequest());

        assertEquals(SagaStatus.COMPENSATED, result.getStatus());
        assertTrue(result.getMessage().contains("Payment failed"));
        assertEquals("ord456", result.getOrderId());
        assertEquals(1, result.getExecutedSteps().size());
        assertEquals("CREATE_ORDER", result.getExecutedSteps().get(0));
        assertNotNull(result.getStep1CreateOrder());
        assertNull(result.getStep2ProcessPayment());
        assertNull(result.getStep3ReserveInventory());
        assertEquals(3, mockWebServer.getRequestCount());
    }

    @Test
    void execute_step3Fails_compensatesAndReturnsCompensated() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"orderId\":\"ord789\",\"status\":\"SUCCESS\"}")
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"paymentId\":\"pay789\",\"status\":\"SUCCESS\"}")
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(503)
                .setBody("{\"error\":\"Inventory service unavailable\"}"));
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"status\":\"SUCCESS\",\"message\":\"Payment refunded\"}")
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"status\":\"SUCCESS\",\"message\":\"Order cancelled\"}")
                .addHeader("Content-Type", "application/json"));

        OrderResponse result = orchestrator.execute(createSampleRequest());

        assertEquals(SagaStatus.COMPENSATED, result.getStatus());
        assertTrue(result.getMessage().contains("Inventory failed"));
        assertEquals("ord789", result.getOrderId());
        assertEquals(2, result.getExecutedSteps().size());
        assertEquals("CREATE_ORDER", result.getExecutedSteps().get(0));
        assertEquals("PROCESS_PAYMENT", result.getExecutedSteps().get(1));
        assertNotNull(result.getStep1CreateOrder());
        assertNotNull(result.getStep2ProcessPayment());
        assertNull(result.getStep3ReserveInventory());
        assertEquals(5, mockWebServer.getRequestCount());
    }

    @Test
    void execute_step1Fails_returnsFailedNoCompensation() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("{\"error\":\"Order service unavailable\"}"));

        OrderResponse result = orchestrator.execute(createSampleRequest());

        assertEquals(SagaStatus.FAILED, result.getStatus());
        assertTrue(result.getMessage().contains("Order creation failed"));
        assertNull(result.getOrderId());
        assertTrue(result.getExecutedSteps().isEmpty());
        assertNull(result.getStep1CreateOrder());
        assertNull(result.getStep2ProcessPayment());
        assertNull(result.getStep3ReserveInventory());
        assertEquals(1, mockWebServer.getRequestCount());
    }
}
