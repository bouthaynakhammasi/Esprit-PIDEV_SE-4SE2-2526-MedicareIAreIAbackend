package com.aziz.demosec.service;

import com.aziz.demosec.Entities.PharmacyOrder;
import com.aziz.demosec.Entities.PharmacyOrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private PharmacyOrderRepository orderRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    @Test
    void generateInvoice_ShouldReturnByteArray() {
        // Arrange
        PharmacyOrder order = new PharmacyOrder();
        order.setId(10L);
        order.setStatus(PharmacyOrderStatus.PAID);
        order.setCreatedAt(LocalDateTime.now());
        order.setTotalPrice(BigDecimal.valueOf(150.0));
        order.setDeliveryAddress("123 Test St");
        order.setItems(Collections.emptyList());

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        // Act
        byte[] pdf = invoiceService.generateInvoice(10L);

        // Assert
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        // Basic PDF header check
        String header = new String(pdf, 0, 4);
        assertTrue(header.startsWith("%PDF"));
    }
}
