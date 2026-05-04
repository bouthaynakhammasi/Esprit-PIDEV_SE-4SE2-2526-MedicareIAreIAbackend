package com.aziz.demosec.controller;

import com.aziz.demosec.Entities.DeliveryType;
import com.aziz.demosec.Entities.PharmacyOrder;
import com.aziz.demosec.Entities.PharmacyOrderStatus;
import com.aziz.demosec.dto.pharmacy.OrderItemRequestDTO;
import com.aziz.demosec.dto.pharmacy.PharmacyOrderRequestDTO;
import com.aziz.demosec.dto.pharmacy.PharmacyOrderResponseDTO;
import com.aziz.demosec.dto.pharmacy.RejectOrderRequestDTO;
import com.aziz.demosec.dto.pharmacy.UpdateOrderStatusRequestDTO;
import com.aziz.demosec.security.CustomUserDetailsService;
import com.aziz.demosec.security.jwt.JwtService;
import com.aziz.demosec.service.PharmacyOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PharmacyOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class PharmacyOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PharmacyOrderService orderService;

    @MockBean
    private InvoiceService invoiceService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void create_ShouldReturnCreatedOrder() throws Exception {
        OrderItemRequestDTO item = OrderItemRequestDTO.builder()
                .productId(1L)
                .quantity(2)
                .build();

        PharmacyOrderRequestDTO request = PharmacyOrderRequestDTO.builder()
                .patientId(1L)
                .pharmacyId(2L)
                .deliveryAddress("Address")
                .prescriptionImageUrl("img")
                .items(List.of(item))
                .deliveryType(DeliveryType.HOME_DELIVERY)
                .build();
        PharmacyOrderResponseDTO response = PharmacyOrderResponseDTO.builder().id(10L).status(PharmacyOrderStatus.PENDING).build();

        when(orderService.createOrder(any(PharmacyOrderRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/pharmacy/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getById_ShouldReturnOrder() throws Exception {
        PharmacyOrderResponseDTO response = PharmacyOrderResponseDTO.builder().id(10L).build();
        when(orderService.getById(10L)).thenReturn(response);

        mockMvc.perform(get("/api/pharmacy/orders/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    void updateStatus_ShouldReturnUpdatedOrder() throws Exception {
        UpdateOrderStatusRequestDTO request = UpdateOrderStatusRequestDTO.builder().status(PharmacyOrderStatus.VALIDATED).build();
        PharmacyOrderResponseDTO response = PharmacyOrderResponseDTO.builder().id(10L).status(PharmacyOrderStatus.VALIDATED).build();

        when(orderService.updateStatus(eq(10L), any(UpdateOrderStatusRequestDTO.class))).thenReturn(response);

        mockMvc.perform(patch("/api/pharmacy/orders/10/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATED"));
    }

    @Test
    void downloadInvoice_ShouldReturnPdf() throws Exception {
        byte[] pdfContent = "fake pdf content".getBytes();
        when(invoiceService.generateInvoice(10L)).thenReturn(pdfContent);

        mockMvc.perform(get("/api/pharmacy/orders/10/invoice"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"invoice-order-10.pdf\""))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfContent));
    }

    @Test
    void rejectOrder_WithTooLongNote_ShouldReturnBadRequest() throws Exception {
        String longNote = "a".repeat(1001);
        RejectOrderRequestDTO request = RejectOrderRequestDTO.builder()
                .note(longNote)
                .build();

        mockMvc.perform(patch("/api/pharmacy/orders/10/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_WithTooLongAddress_ShouldReturnBadRequest() throws Exception {
        OrderItemRequestDTO item = OrderItemRequestDTO.builder().productId(1L).quantity(1).build();
        PharmacyOrderRequestDTO request = PharmacyOrderRequestDTO.builder()
                .patientId(1L)
                .pharmacyId(2L)
                .deliveryAddress("a".repeat(256))
                .prescriptionImageUrl("img")
                .items(List.of(item))
                .deliveryType(DeliveryType.HOME_DELIVERY)
                .build();

        mockMvc.perform(post("/api/pharmacy/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
