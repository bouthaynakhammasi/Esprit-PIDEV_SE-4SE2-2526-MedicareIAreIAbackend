package com.aziz.demosec.controller;

import com.aziz.demosec.Entities.Payment;
import com.aziz.demosec.Entities.PaymentStatus;
import com.aziz.demosec.Entities.PharmacyOrder;
import com.aziz.demosec.repository.PaymentRepository;
import com.aziz.demosec.security.CustomUserDetailsService;
import com.aziz.demosec.security.jwt.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class PaymentWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentRepository paymentRepository;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private PaymentWebSocketService paymentWebSocketService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void d17Webhook_ShouldUpdatePaymentOnSuccess() throws Exception {
        // Arrange
        Map<String, Object> payload = Map.of(
                "event", "payment.completed",
                "transactionId", "D17-TX-123"
        );

        PharmacyOrder order = new PharmacyOrder();
        order.setId(1L);

        Payment payment = Payment.builder()
                .id(10L)
                .transactionId("D17-TX-123")
                .order(order)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findByTransactionId("D17-TX-123")).thenReturn(Optional.of(payment));

        // Act & Assert
        mockMvc.perform(post("/api/pharmacy/payments/webhook/d17")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("received"));

        verify(paymentRepository).save(any(Payment.class));
        verify(paymentService).confirmOrderPaid(order);
        verify(paymentWebSocketService).notifyPaymentCompleted(any(Payment.class));
    }

    @Test
    void d17Webhook_ShouldHandleFailureEvent() throws Exception {
        // Arrange
        Map<String, Object> payload = Map.of(
                "event", "payment.failed",
                "transactionId", "D17-TX-FAIL",
                "reason", "Insufficient funds"
        );

        PharmacyOrder order = new PharmacyOrder();
        order.setId(2L);

        Payment payment = Payment.builder()
                .id(11L)
                .transactionId("D17-TX-FAIL")
                .order(order)
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findByTransactionId("D17-TX-FAIL")).thenReturn(Optional.of(payment));

        // Act & Assert
        mockMvc.perform(post("/api/pharmacy/payments/webhook/d17")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        verify(paymentWebSocketService).notifyPaymentFailed(any(Payment.class), eq("Insufficient funds"));
    }
}
