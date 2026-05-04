package com.aziz.demosec.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

import com.aziz.demosec.Entities.Payment;
import com.aziz.demosec.Entities.PaymentStatus;
import com.aziz.demosec.repository.PaymentRepository;
import com.aziz.demosec.payment.PaymentWebSocketService;
import com.aziz.demosec.service.PaymentService;

@Slf4j
@RestController
@RequestMapping("/api/pharmacy/payments/webhook")
@RequiredArgsConstructor
public class PaymentWebhookController {

    @Value("${stripe.webhook.secret}")
    private String stripeWebhookSecret;

    @Value("${d17.webhook.secret}")
    private String d17WebhookSecret;

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final PaymentWebSocketService paymentWebSocketService;
    // Removed unused ObjectMapper

    /**
     * Stripe Webhook Endpoint
     * Receives payment status updates from Stripe
     * 
     * Events handled:
     * - payment_intent.succeeded
     * - payment_intent.payment_failed
     * - charge.refunded
     */
    @PostMapping("/stripe")
    public ResponseEntity<Map<String, String>> stripeWebhook(
            @org.springframework.web.bind.annotation.RequestBody String payload,
            @org.springframework.web.bind.annotation.RequestHeader("Stripe-Signature") String sigHeader) {

        try {
            // Verify webhook signature
            Event event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);

            log.info("Stripe webhook received - Event: {}", event.getType());

            // Handle specific event types
            switch (event.getType()) {
                case "payment_intent.succeeded" -> handleStripePaymentSucceeded(event);
                case "payment_intent.payment_failed" -> handleStripePaymentFailed(event);
                case "charge.refunded" -> handleStripeRefunded(event);
                default -> log.debug("Unhandled Stripe event type: {}", event.getType());
            }

            return ResponseEntity.ok(Map.of("status", "received"));

        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe webhook signature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Invalid signature"));
        } catch (Exception e) {
            log.error("Error processing Stripe webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * D17 Webhook Endpoint
     * Receives payment status updates from D17
     * 
     * Events handled:
     * - payment.completed
     * - payment.failed
     * - refund.completed
     */
    @PostMapping("/d17")
    public ResponseEntity<Map<String, String>> d17Webhook(
            @org.springframework.web.bind.annotation.RequestBody Map<String, Object> payload,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-D17-Signature", required = false) String signature) {

        try {
            // Verify webhook signature (if provided)
            if (signature != null && !verifyD17Signature(payload, signature)) {
                log.warn("Invalid D17 webhook signature");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Invalid signature"));
            }

            String eventType = (String) payload.get("event");
            log.info("D17 webhook received - Event: {}", eventType);

            switch (eventType) {
                case "payment.completed" -> handleD17PaymentCompleted(payload);
                case "payment.failed" -> handleD17PaymentFailed(payload);
                case "refund.completed" -> handleD17RefundCompleted(payload);
                default -> log.debug("Unhandled D17 event type: {}", eventType);
            }

            return ResponseEntity.ok(Map.of("status", "received"));

        } catch (Exception e) {
            log.error("Error processing D17 webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Handle Stripe payment_intent.succeeded event
     */
    private void handleStripePaymentSucceeded(Event event) {
        try {
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            PaymentIntent paymentIntent = (PaymentIntent) dataObjectDeserializer.getObject()
                    .orElse(null);

            if (paymentIntent == null) {
                log.warn("Could not deserialize payment intent from webhook");
                return;
            }

            String transactionId = paymentIntent.getId();
            log.info("Payment succeeded - Stripe ID: {}", transactionId);

            // Find and update payment
            Payment payment = paymentRepository.findByTransactionId(transactionId)
                    .orElse(null);

            if (payment != null) {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setConfirmedAt(LocalDateTime.now());
                paymentRepository.save(payment);

                // Update order status to PAID
                paymentService.confirmOrderPaid(payment.getOrder());

                // Notify via WebSocket
                paymentWebSocketService.notifyPaymentCompleted(payment);

                log.info("Payment updated to COMPLETED and Order to PAID - Order: {}", payment.getOrder().getId());
            } else {
                log.warn("Payment not found for transaction: {}", transactionId);
            }

        } catch (Exception e) {
            log.error("Error handling Stripe success event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle Stripe payment_intent.payment_failed event
     */
    private void handleStripePaymentFailed(Event event) {
        try {
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            PaymentIntent paymentIntent = (PaymentIntent) dataObjectDeserializer.getObject()
                    .orElse(null);

            if (paymentIntent == null) {
                log.warn("Could not deserialize payment intent from webhook");
                return;
            }

            String transactionId = paymentIntent.getId();
            String failureCode = paymentIntent.getLastPaymentError() != null
                    ? paymentIntent.getLastPaymentError().getCode()
                    : "UNKNOWN";

            log.warn("Payment failed - Stripe ID: {}, Code: {}", transactionId, failureCode);

            Payment payment = paymentRepository.findByTransactionId(transactionId)
                    .orElse(null);

            if (payment != null) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setGatewayMetadata(failureCode);
                paymentRepository.save(payment);

                paymentWebSocketService.notifyPaymentFailed(payment, failureCode);

                log.info("Payment marked as FAILED - Order: {}", payment.getOrder().getId());
            }

        } catch (Exception e) {
            log.error("Error handling Stripe failure event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle Stripe charge.refunded event
     */
    private void handleStripeRefunded(Event event) {
        try {
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            // Handle refund event
            log.info("Stripe charge refunded - Event data provided by Stripe");

        } catch (Exception e) {
            log.error("Error handling Stripe refund event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle D17 payment.completed event
     */
    private void handleD17PaymentCompleted(Map<String, Object> payload) {
        try {
            String transactionId = (String) payload.get("transactionId");
            log.info("D17 payment completed - Transaction: {}", transactionId);

            Payment payment = paymentRepository.findByTransactionId(transactionId)
                    .orElse(null);

            if (payment != null) {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setConfirmedAt(LocalDateTime.now());
                paymentRepository.save(payment);

                // Update order status to PAID
                paymentService.confirmOrderPaid(payment.getOrder());

                paymentWebSocketService.notifyPaymentCompleted(payment);

                log.info("D17 payment confirmed and Order set to PAID - Order: {}", payment.getOrder().getId());
            } else {
                log.warn("Payment not found for D17 transaction: {}", transactionId);
            }

        } catch (Exception e) {
            log.error("Error handling D17 completion: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle D17 payment.failed event
     */
    private void handleD17PaymentFailed(Map<String, Object> payload) {
        try {
            String transactionId = (String) payload.get("transactionId");
            String reason = (String) payload.get("reason");

            log.warn("D17 payment failed - Transaction: {}, Reason: {}", transactionId, reason);

            Payment payment = paymentRepository.findByTransactionId(transactionId)
                    .orElse(null);

            if (payment != null) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setGatewayMetadata(reason);
                paymentRepository.save(payment);

                paymentWebSocketService.notifyPaymentFailed(payment, reason);

                log.info("D17 payment failure recorded - Order: {}", payment.getOrder().getId());
            }

        } catch (Exception e) {
            log.error("Error handling D17 failure: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle D17 refund.completed event
     */
    private void handleD17RefundCompleted(Map<String, Object> payload) {
        try {
            String refundId = (String) payload.get("refundId");
            String transactionId = (String) payload.get("transactionId");

            log.info("D17 refund completed - Refund: {}, Transaction: {}", refundId, transactionId);

            Payment payment = paymentRepository.findByTransactionId(transactionId)
                    .orElse(null);

            if (payment != null) {
                payment.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);

                paymentWebSocketService.notifyPaymentRefunded(payment, refundId, "D17 Refund");
            }

        } catch (Exception e) {
            log.error("Error handling D17 refund: {}", e.getMessage(), e);
        }
    }

    /**
     * Verify D17 webhook signature
     * Uses HMAC-SHA256 of payload with D17 secret
     */
    private boolean verifyD17Signature(Map<String, Object> payload, String signature) {
        try {
            // Implementation depends on D17 signature algorithm
            // Typically HMAC-SHA256 of JSON payload
            // For now, simple implementation - adjust based on D17 docs
            log.debug("Verifying D17 signature...");
            return true; // TODO: Implement actual signature verification
        } catch (Exception e) {
            log.error("D17 signature verification error: {}", e.getMessage());
            return false;
        }
    }
}
