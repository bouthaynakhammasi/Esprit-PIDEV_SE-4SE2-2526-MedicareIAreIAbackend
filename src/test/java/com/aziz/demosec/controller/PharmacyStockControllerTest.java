package com.aziz.demosec.controller;

import com.aziz.demosec.dto.pharmacy.PharmacyStockRequestDTO;
import com.aziz.demosec.dto.pharmacy.PharmacyStockResponseDTO;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PharmacyStockController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class PharmacyStockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PharmacyStockService stockService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void create_ShouldReturnCreatedStock() throws Exception {
        PharmacyStockRequestDTO request = PharmacyStockRequestDTO.builder()
                .pharmacyId(1L).productId(2L).totalQuantity(100)
                .minQuantityThreshold(10).unitPrice(BigDecimal.valueOf(10.0))
                .build();
        PharmacyStockResponseDTO response = PharmacyStockResponseDTO.builder()
                .id(1L).pharmacyName("Test Pharma").productName("Test Product")
                .totalQuantity(100).build();

        when(stockService.create(any(PharmacyStockRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/pharmacy/stocks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.totalQuantity").value(100));
    }

    @Test
    void getById_ShouldReturnStock() throws Exception {
        PharmacyStockResponseDTO response = PharmacyStockResponseDTO.builder().id(1L).totalQuantity(50).build();
        when(stockService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/pharmacy/stocks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.totalQuantity").value(50));
    }

    @Test
    void getByPharmacy_ShouldReturnList() throws Exception {
        PharmacyStockResponseDTO response = PharmacyStockResponseDTO.builder().id(1L).build();
        when(stockService.getByPharmacy(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/pharmacy/stocks/pharmacy/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void findAvailability_ShouldReturnList() throws Exception {
        PharmacyStockResponseDTO response = PharmacyStockResponseDTO.builder().id(1L).build();
        when(stockService.findPharmaciesWithProduct(2L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/pharmacy/stocks/availability/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void update_ShouldReturnUpdatedStock() throws Exception {
        PharmacyStockRequestDTO request = PharmacyStockRequestDTO.builder()
                .pharmacyId(1L)
                .productId(2L)
                .totalQuantity(200)
                .minQuantityThreshold(10)
                .unitPrice(BigDecimal.valueOf(12.5))
                .build();
        PharmacyStockResponseDTO response = PharmacyStockResponseDTO.builder().id(1L).totalQuantity(200).build();

        when(stockService.update(eq(1L), any(PharmacyStockRequestDTO.class))).thenReturn(response);

        mockMvc.perform(put("/api/pharmacy/stocks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(200));
    }
}
