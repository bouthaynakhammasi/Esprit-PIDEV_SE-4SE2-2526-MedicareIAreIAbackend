package com.aziz.demosec.service;

import com.aziz.demosec.Entities.*;
import com.aziz.demosec.dto.pharmacy.PaymentRequestDTO;
import com.aziz.demosec.dto.pharmacy.PaymentResponseDTO;
import com.aziz.demosec.payment.PaymentWebSocketService;
import com.aziz.demosec.payment.gateway.PaymentGatewayFactory;
import com.aziz.demosec.payment.gateway.PaymentGatewayProvider;
import com.aziz.demosec.payment.gateway.PaymentGatewayResponse;
import com.aziz.demosec.repository.PaymentRepository;
import com.aziz.demosec.repository.PharmacyOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PharmacyOrderRepository orderRepository;
    @Mock private PharmacyOrderService orderService;
    @Mock private PaymentGatewayFactory gatewayFactory;
    @Mock private PaymentWebSocketService paymentWebSocketService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private PharmacyOrder order;
    private Payment payment;

    @BeforeEach
    void setUp() {
        order = new PharmacyOrder();
        order.setId(1L);
        order.setTotalPrice(BigDecimal.valueOf(100.0));
        order.setStatus(PharmacyOrderStatus.VALIDATED);

        payment = Payment.builder()
                .id(10L)
                .order(order)
                .amount(BigDecimal.valueOf(100.0))
                .status(PaymentStatus.PENDING)
                .method(PaymentMethod.CASH_ON_DELIVERY)
                .build();
    }

    @Test
    void initiatePayment_ShouldHandleCOD() {
        // Arrange
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .orderId(1L)
                .method(PaymentMethod.CASH_ON_DELIVERY)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setId(10L);
            return p;
        });

        // Act
        PaymentResponseDTO result = paymentService.initiatePayment(request);

        // Assert
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        assertTrue(result.getTransactionId().startsWith("COD-"));
        assertEquals(PharmacyOrderStatus.PAID, order.getStatus());
        verify(paymentWebSocketService).notifyPaymentCompleted(any(Payment.class));
    }

    @Test
    void createPaymentIntent_ShouldUseStripeProvider() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrder_Id(1L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentGatewayProvider stripeProvider = mock(PaymentGatewayProvider.class);
        when(gatewayFactory.getProvider(PaymentMethod.STRIPE)).thenReturn(stripeProvider);
        when(stripeProvider.isConfigured()).thenReturn(true);
        
        PaymentGatewayResponse gatewayResponse = PaymentGatewayResponse.builder()
                .success(true)
                .transactionId("pi_test")
                .status("requires_payment_method")
                .metadata(java.util.Map.of("clientSecret", "secret_123"))
                .build();
        when(stripeProvider.createIntent(any(Payment.class))).thenReturn(gatewayResponse);

        // Act
        PaymentResponseDTO result = paymentService.createPaymentIntent(1L);

        // Assert
        assertNotNull(result);
        assertEquals("pi_test", result.getTransactionId());
        assertEquals("secret_123", result.getClientSecret());
    }

    @Test
    void getByOrderId_ShouldReturnPayment() {
        // Arrange
        when(paymentRepository.findByOrder_Id(1L)).thenReturn(Optional.of(payment));

        // Act
        PaymentResponseDTO result = paymentService.getByOrderId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.getId());
    }
}
