package com.aziz.demosec.controller;

import com.aziz.demosec.Entities.PaymentMethod;
import com.aziz.demosec.Entities.PaymentStatus;
import com.aziz.demosec.dto.pharmacy.PaymentRequestDTO;
import com.aziz.demosec.dto.pharmacy.PaymentResponseDTO;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void initiatePayment_ShouldReturnCreated() throws Exception {
        PaymentRequestDTO request = PaymentRequestDTO.builder().orderId(1L).method(PaymentMethod.STRIPE).build();
        PaymentResponseDTO response = PaymentResponseDTO.builder().id(10L).status(PaymentStatus.PENDING).build();

        when(paymentService.initiatePayment(any(PaymentRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/pharmacy/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    void getByOrderId_ShouldReturnPayment() throws Exception {
        PaymentResponseDTO response = PaymentResponseDTO.builder().id(10L).orderId(1L).build();
        when(paymentService.getByOrderId(1L)).thenReturn(response);

        mockMvc.perform(get("/api/pharmacy/payments/order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    void createPaymentIntent_ShouldReturnIntent() throws Exception {
        PaymentResponseDTO response = PaymentResponseDTO.builder().id(10L).clientSecret("secret").build();
        when(paymentService.createPaymentIntent(1L)).thenReturn(response);

        mockMvc.perform(post("/api/pharmacy/payments/create-payment-intent/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientSecret").value("secret"));
    }
}
