package com.aziz.demosec.payment.gateway.impl;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import com.aziz.demosec.Entities.Payment;
import com.aziz.demosec.dto.pharmacy.PaymentRequestDTO;
import com.aziz.demosec.payment.gateway.PaymentGatewayProvider;
import com.aziz.demosec.payment.gateway.PaymentGatewayResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * D17 Payment Gateway Implementation (Tunisian Payment Solution)
 * Handles D17 payment processing for Tunisian merchants
 * 
 * Supports:
 * - Electronic wallet payments
 * - Bank cards
 * - SMS/USSD payments
 * - Invoicing and receipts
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class D17PaymentProvider implements PaymentGatewayProvider {

    @Value("${d17.api.key}")
    private String d17ApiKey;

    @Value("${d17.merchant.id}")
    private String d17MerchantId;

    @Value("${d17.api.url:https://api.d17.tn/}")
    private String d17ApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public PaymentGatewayResponse processPayment(Payment payment, PaymentRequestDTO request) {
        try {
            String transactionId = "D17-" + UUID.randomUUID().toString().substring(0, 12);

            // Prepare D17 API request payload
            Map<String, Object> d17Request = prepareD17Request(payment, request, transactionId);

            // Call D17 API
            PaymentGatewayResponse response = callD17Api("/api/payment/initiate", d17Request);

            if (response.getSuccess()) {
                response.setTransactionId(transactionId);
                response.setMethod("D17_WALLET");
                log.info("D17 payment initiated - Transaction: {}", transactionId);
                return response;
            } else {
                log.warn("D17 payment initiation failed - Reason: {}", response.getErrorMessage());
                return response;
            }

        } catch (Exception e) {
            log.error("D17 payment processing error: ", e);
            return PaymentGatewayResponse.failure("D17_ERROR", "D17 payment processing failed: " + e.getMessage());
        }
    }

    @Override
    public PaymentGatewayResponse verifyPayment(String transactionId) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("transactionId", transactionId);
            request.put("merchantId", d17MerchantId);
            request.put("timestamp", System.currentTimeMillis());

            return callD17Api("/api/payment/verify", request);

        } catch (Exception e) {
            log.error("D17 payment verification error: ", e);
            return PaymentGatewayResponse.failure("D17_ERROR", "D17 verification failed: " + e.getMessage());
        }
    }

    @Override
    public PaymentGatewayResponse refundPayment(String transactionId, Double amount) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("transactionId", transactionId);
            request.put("refundId", "REF-" + UUID.randomUUID().toString().substring(0, 12));
            request.put("merchantId", d17MerchantId);

            if (amount != null) {
                request.put("amount", amount);
                request.put("refundType", "PARTIAL");
            } else {
                request.put("refundType", "FULL");
            }

            request.put("timestamp", System.currentTimeMillis());

            PaymentGatewayResponse response = callD17Api("/api/payment/refund", request);

            if (response.getSuccess()) {
                log.info("D17 refund successful - TxnId: {}", transactionId);
            }

            return response;

        } catch (Exception e) {
            log.error("D17 refund error: ", e);
            return PaymentGatewayResponse.failure("D17_ERROR", "D17 refund failed: " + e.getMessage());
        }
    }

    @Override
    public PaymentGatewayResponse cancelPayment(String transactionId) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("transactionId", transactionId);
            request.put("merchantId", d17MerchantId);
            request.put("reason", "MERCHANT_CANCEL");
            request.put("timestamp", System.currentTimeMillis());

            return callD17Api("/api/payment/cancel", request);

        } catch (Exception e) {
            log.error("D17 cancel payment error: ", e);
            return PaymentGatewayResponse.failure("D17_ERROR", "D17 cancel failed: " + e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "D17";
    }

    @Override
    public boolean isConfigured() {
        return d17ApiKey != null && !d17ApiKey.isEmpty() && !d17ApiKey.contains("YOUR_") &&
                d17MerchantId != null && !d17MerchantId.isEmpty();
    }

    /**
     * Prepare D17 API request with authentication
     */
    private Map<String, Object> prepareD17Request(Payment payment, PaymentRequestDTO request,
            String transactionId) {
        Map<String, Object> d17Request = new HashMap<>();

        // Transaction details
        d17Request.put("transactionId", transactionId);
        d17Request.put("merchantId", d17MerchantId);
        d17Request.put("orderId", payment.getOrder().getId());
        d17Request.put("amount", payment.getAmount());
        d17Request.put("currency", payment.getCurrency());
        d17Request.put("description", "Pharmacy Order #" + payment.getOrder().getId());

        // Customer details
        d17Request.put("customerPhone", payment.getOrder().getPatient().getPhone());
        d17Request.put("customerEmail", payment.getOrder().getPatient().getEmail());
        d17Request.put("customerName", payment.getOrder().getPatient().getFullName());

        // Payment method
        d17Request.put("paymentMethod", mapToD17PaymentMethod(request.getMethod()));
        d17Request.put("walletToken", request.getPaymentToken());

        // Additional metadata
        d17Request.put("timestamp", Instant.now().toEpochMilli());
        d17Request.put("notificationUrl", "http://localhost:8081/springsecurity/api/pharmacy/payments/webhook/d17");
        d17Request.put("returnUrl", "http://localhost:3000/payment/complete");

        // Add signature/authentication
        d17Request.put("signature", generateD17Signature(d17Request));

        return d17Request;
    }

    /**
     * Map application payment method to D17 payment method
     */
    private String mapToD17PaymentMethod(com.aziz.demosec.Entities.PaymentMethod method) {
        return switch (method.toString()) {
            case "D17_WALLET" -> "WALLET";
            case "D17_CARD" -> "CARD";
            case "D17_SMS" -> "SMS";
            case "D17_USSD" -> "USSD";
            default -> "WALLET";
        };
    }

    /**
     * Generate D17 request signature using JWT
     */
    private String generateD17Signature(Map<String, Object> request) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(d17ApiKey);
            return JWT.create()
                    .withIssuer("PHARMACY_SYSTEM")
                    .withClaim("merchantId", d17MerchantId)
                    .withClaim("transactionId", (String) request.get("transactionId"))
                    .withClaim("amount", ((Number) request.get("amount")).doubleValue())
                    .withIssuedAt(new java.util.Date())
                    .sign(algorithm);
        } catch (Exception e) {
            log.error("Error generating D17 signature: ", e);
            return "";
        }
    }

    /**
     * Call D17 API endpoint
     */
    private PaymentGatewayResponse callD17Api(String endpoint, Map<String, Object> payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + d17ApiKey);
            headers.set("X-Merchant-Id", d17MerchantId);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(
                    d17ApiUrl.replaceAll("/$", "") + endpoint,
                    request,
                    (Class<Map<String, Object>>) (Class<?>) Map.class);


            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                return PaymentGatewayResponse.builder()
                        .success((Boolean) body.getOrDefault("success", false))
                        .transactionId((String) body.get("transactionId"))
                        .status((String) body.getOrDefault("status", "FAILED"))
                        .errorCode((String) body.get("errorCode"))
                        .errorMessage((String) body.get("errorMessage"))
                        .metadata(body)
                        .build();
            } else {
                return PaymentGatewayResponse.failure("D17_API_ERROR",
                        "D17 API returned: " + response.getStatusCode());
            }

        } catch (RestClientException e) {
            log.error("D17 API call error: ", e);
            return PaymentGatewayResponse.failure("D17_CONNECTION_ERROR",
                    "Failed to connect to D17: " + e.getMessage());
        }
    }
}
