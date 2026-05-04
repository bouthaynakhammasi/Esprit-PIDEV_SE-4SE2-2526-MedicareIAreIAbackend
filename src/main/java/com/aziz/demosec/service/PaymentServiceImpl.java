package com.aziz.demosec.service;

import com.aziz.demosec.Entities.Payment;
import com.aziz.demosec.Entities.PharmacyOrder;
import com.aziz.demosec.Entities.PaymentMethod;
import com.aziz.demosec.Entities.PaymentStatus;
import com.aziz.demosec.Entities.PharmacyOrderStatus;
import com.aziz.demosec.dto.pharmacy.PaymentRequestDTO;
import com.aziz.demosec.dto.pharmacy.PaymentResponseDTO;
import com.aziz.demosec.payment.PaymentWebSocketService;
import com.aziz.demosec.payment.gateway.PaymentGatewayFactory;
import com.aziz.demosec.payment.gateway.PaymentGatewayProvider;
import com.aziz.demosec.payment.gateway.PaymentGatewayResponse;
import com.aziz.demosec.repository.PaymentRepository;
import com.aziz.demosec.repository.PharmacyOrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Enhanced Payment Service with Real Gateway Integration
 * Handles payment processing via Stripe, D17, or Cash on Delivery
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PharmacyOrderRepository orderRepository;
    private final PharmacyOrderService orderService;
    private final PaymentGatewayFactory gatewayFactory;
    private final PaymentWebSocketService paymentWebSocketService;

    /**
     * Initiate payment for an order
     * Routes to appropriate payment provider based on payment method
     * 
     * @param dto PaymentRequestDTO with order ID, method, and token (for
     *            card/wallet payments)
     * @return PaymentResponseDTO with transaction details
     */
    @Override
    public PaymentResponseDTO initiatePayment(PaymentRequestDTO dto) {
        // Validate order exists
        PharmacyOrder order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + dto.getOrderId()));

        // Create payment entity
        Payment payment = Payment.builder()
                .order(order)
                .method(dto.getMethod())
                .amount(order.getTotalPrice())
                .currency("TND") // Default to Tunisian Dinar
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        payment = paymentRepository.save(payment);
        log.info("Payment created - Order: {}, Amount: {}, Method: {}",
                order.getId(), payment.getAmount(), payment.getMethod());

        // Notify via WebSocket that payment is initiated
        paymentWebSocketService.notifyPaymentInitiated(payment);

        // Process based on payment method
        if (payment.getMethod() == PaymentMethod.CASH_ON_DELIVERY) {
            return handleCashOnDelivery(payment);
        } else {
            return handleGatewayPayment(payment, dto);
        }
    }

    /**
     * Handle Cash on Delivery payments
     * No external gateway needed, auto-confirm
     */
    private PaymentResponseDTO handleCashOnDelivery(Payment payment) {
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId("COD-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        payment.setConfirmedAt(LocalDateTime.now());

        payment = paymentRepository.save(payment);

        // Update order status
        confirmOrderPaid(payment.getOrder());

        // Notify via WebSocket
        paymentWebSocketService.notifyPaymentCompleted(payment);

        log.info("Cash on Delivery payment confirmed - Order: {}, TxnId: {}",
                payment.getOrder().getId(), payment.getTransactionId());

        return toDTO(payment);
    }

    /**
     * Handle card/wallet payments via external gateway (Stripe or D17)
     */
    private PaymentResponseDTO handleGatewayPayment(Payment payment, PaymentRequestDTO request) {
        try {
            // Get appropriate payment gateway provider
            PaymentGatewayProvider provider = gatewayFactory.getProvider(payment.getMethod());

            if (provider == null) {
                throw new IllegalArgumentException("No payment provider for method: " + payment.getMethod());
            }

            // Check if provider is configured
            if (!provider.isConfigured()) {
                log.warn("Payment provider {} not configured, using mock mode", provider.getProviderName());
                return handleMockPayment(payment);
            }

            // Notify WebSocket: payment processing started
            paymentWebSocketService.notifyPaymentProcessing(payment);

            // Call real payment gateway
            PaymentGatewayResponse gatewayResponse = provider.processPayment(payment, request);

            // Handle gateway response
            if (gatewayResponse.getSuccess()) {
                // Payment succeeded
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTransactionId(gatewayResponse.getTransactionId());
                payment.setConfirmedAt(LocalDateTime.now());

                if (gatewayResponse.getMetadata() != null) {
                    payment.setGatewayMetadata(gatewayResponse.getMetadata().toString());
                }

                payment = paymentRepository.save(payment);

                // Update order status
                confirmOrderPaid(payment.getOrder());

                // Notify via WebSocket
                paymentWebSocketService.notifyPaymentCompleted(payment);

                log.info("Payment completed - Provider: {}, Order: {}, TxnId: {}",
                        provider.getProviderName(), payment.getOrder().getId(),
                        payment.getTransactionId());

                return toDTO(payment);
            } else if ("REQUIRES_AUTH".equals(gatewayResponse.getStatus())) {
                // Payment requires additional authentication (3DS)
                payment.setStatus(PaymentStatus.PENDING);
                payment.setTransactionId(gatewayResponse.getTransactionId());
                payment.setGatewayMetadata(gatewayResponse.getMetadata().toString());

                payment = paymentRepository.save(payment);

                log.info("Payment requires authentication (3DS) - Order: {}",
                        payment.getOrder().getId());

                return toDTO(payment);
            } else {
                // Payment failed
                String errorReason = gatewayResponse.getErrorMessage() != null ? gatewayResponse.getErrorMessage()
                        : "Payment processing failed";

                payment.setStatus(PaymentStatus.FAILED);
                payment.setGatewayMetadata(gatewayResponse.getErrorMessage());
                payment = paymentRepository.save(payment);

                // Notify via WebSocket
                paymentWebSocketService.notifyPaymentFailed(payment, errorReason);

                log.warn("Payment failed - Provider: {}, Order: {}, Error: {}",
                        provider.getProviderName(), payment.getOrder().getId(), errorReason);

                return toDTO(payment);
            }

        } catch (Exception e) {
            log.error("Gateway payment processing error - Order: {}",
                    payment.getOrder().getId(), e);

            payment.setStatus(PaymentStatus.FAILED);
            payment.setGatewayMetadata(e.getMessage());
            payment = paymentRepository.save(payment);

            // Notify via WebSocket
            paymentWebSocketService.notifyPaymentFailed(payment, e.getMessage());

            // Return failure response but don't throw - allow caller to handle
            return toDTO(payment);
        }
    }

    /**
     * Mock payment processing (for development/testing when gateway not configured)
     */
    private PaymentResponseDTO handleMockPayment(Payment payment) {
        log.warn("Using mock payment processing for Order: {}", payment.getOrder().getId());

        // Simulate processing delay
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId("MOCK-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        payment.setConfirmedAt(LocalDateTime.now());

        payment = paymentRepository.save(payment);

        confirmOrderPaid(payment.getOrder());
        paymentWebSocketService.notifyPaymentCompleted(payment);

        return toDTO(payment);
    }

    @Override
    public PaymentResponseDTO createPaymentIntent(Long orderId) {
        PharmacyOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        // Create or get existing payment
        Payment payment = paymentRepository.findByOrder_Id(orderId)
                .orElseGet(() -> {
                    Payment p = Payment.builder()
                            .order(order)
                            .method(PaymentMethod.STRIPE)
                            .amount(order.getTotalPrice())
                            .currency("EUR") // Changed to EUR for better Stripe Test Account compatibility
                            .status(PaymentStatus.PENDING)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return paymentRepository.save(p);
                });

        PaymentGatewayProvider provider = gatewayFactory.getProvider(PaymentMethod.STRIPE);

        // Guard: avoid calling Stripe with an unconfigured (placeholder) API key
        if (!provider.isConfigured()) {
            log.warn("Stripe provider is not configured (missing real API key). Cannot create payment intent for order: {}", orderId);
            throw new RuntimeException("Stripe payment gateway is not configured. Please set a valid STRIPE_API_KEY in application.properties.");
        }

        PaymentGatewayResponse response = provider.createIntent(payment);

        if (response.getSuccess()) {
            payment.setTransactionId(response.getTransactionId());
            if (response.getMetadata() != null) {
                payment.setGatewayMetadata(response.getMetadata().toString());
            }
            payment = paymentRepository.save(payment);

            PaymentResponseDTO dto = toDTO(payment);
            if (response.getMetadata() != null && response.getMetadata().containsKey("clientSecret")) {
                dto.setClientSecret((String) response.getMetadata().get("clientSecret"));
            }
            return dto;
        } else {
            throw new RuntimeException("Failed to create Stripe intent: " + response.getErrorMessage());
        }
    }

    /**
     * Get payment details by order ID
     */
    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDTO getByOrderId(Long orderId) {
        return toDTO(paymentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found for order: " + orderId)));
    }

    /**
     * Get payment details by payment ID
     */
    @Transactional(readOnly = true)
    public PaymentResponseDTO getById(Long paymentId) {
        return toDTO(paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found: " + paymentId)));
    }

    /**
     * Verify payment with gateway (for webhooks or manual verification)
     */
    public PaymentResponseDTO verifyPaymentWithGateway(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found: " + paymentId));

        if (payment.getTransactionId() == null) {
            throw new IllegalStateException("Payment has no transaction ID: " + paymentId);
        }

        PaymentGatewayProvider provider = gatewayFactory.getProvider(payment.getMethod());

        if (provider == null) {
            log.warn("No gateway provider available for method: {}", payment.getMethod());
            return toDTO(payment);
        }

        PaymentGatewayResponse response = provider.verifyPayment(payment.getTransactionId());

        // Update payment status based on verification
        if (response.getSuccess() && "COMPLETED".equals(response.getStatus())) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment = paymentRepository.save(payment);
            confirmOrderPaid(payment.getOrder());
            paymentWebSocketService.notifyPaymentCompleted(payment);
        }

        return toDTO(payment);
    }

    /**
     * Refund a completed payment
     */
    @Transactional
    public PaymentResponseDTO refundPayment(Long paymentId, Double refundAmount, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found: " + paymentId));

        if (!PaymentStatus.COMPLETED.equals(payment.getStatus())) {
            throw new IllegalStateException("Can only refund completed payments");
        }

        if (payment.getTransactionId() == null) {
            throw new IllegalStateException("Payment has no transaction ID");
        }

        PaymentGatewayProvider provider = gatewayFactory.getProvider(payment.getMethod());

        if (provider == null) {
            throw new IllegalArgumentException("No gateway provider for method: " + payment.getMethod());
        }

        PaymentGatewayResponse response = provider.refundPayment(payment.getTransactionId(), refundAmount);

        if (response.getSuccess()) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment = paymentRepository.save(payment);

            paymentWebSocketService.notifyPaymentRefunded(payment, response.getRefundId(), reason);

            log.info("Payment refunded - Order: {}, RefundId: {}",
                    payment.getOrder().getId(), response.getRefundId());
        } else {
            log.warn("Refund failed - Payment: {}, Reason: {}", paymentId, response.getErrorMessage());
        }

        return toDTO(payment);
    }

    /**
     * Update order status to PAID
     */
    @Override
    public void confirmOrderPaid(PharmacyOrder order) {
        try {
            order.setStatus(PharmacyOrderStatus.PAID);
            orderRepository.save(order);
            log.debug("Order status updated to PAID - Order: {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to update order status - Order: {}", order.getId(), e);
        }
    }

    /**
     * Convert Payment entity to DTO
     */
    private PaymentResponseDTO toDTO(Payment payment) {
        PaymentResponseDTO dto = PaymentResponseDTO.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .transactionId(payment.getTransactionId())
                .createdAt(payment.getCreatedAt())
                .confirmedAt(payment.getConfirmedAt())
                .gatewayMetadata(payment.getGatewayMetadata())
                .build();

        // Handle extraction of clientSecret if stored in metadata (for Stripe)
        if (payment.getGatewayMetadata() != null && payment.getMethod() == PaymentMethod.STRIPE) {
            String metadata = payment.getGatewayMetadata();
            // This assumes metadata is a string representation of a map or contains
            // "clientSecret="
            // Example: "{clientSecret=pi_..._secret_..., otherKey=value}"
            if (metadata.contains("clientSecret=")) {
                String secret = metadata.substring(metadata.indexOf("clientSecret=") + "clientSecret=".length());
                if (secret.contains(","))
                    secret = secret.substring(0, secret.indexOf(","));
                if (secret.contains("}"))
                    secret = secret.substring(0, secret.indexOf("}"));
                dto.setClientSecret(secret.trim());
            }
        }

        return dto;
    }
}
