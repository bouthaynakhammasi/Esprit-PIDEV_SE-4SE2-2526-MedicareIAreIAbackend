package com.aziz.demosec.controller;

import com.aziz.demosec.domain.User;
import com.aziz.demosec.dto.WhatsAppAlertRequest;
import com.aziz.demosec.dto.WhatsAppAlertResponse;
import com.aziz.demosec.repository.UserRepository;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/forum/whatsapp")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppController {

    private final UserRepository userRepository;

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.whatsapp.from}")
    private String fromNumber;

    @PostConstruct
    public void initTwilio() {
        Twilio.init(accountSid, authToken);
        log.info("✅ Twilio initialisé");
    }

    // POST /api/forum/whatsapp/alert
    @PostMapping("/alert")
    public ResponseEntity<WhatsAppAlertResponse> sendAlert(@RequestBody WhatsAppAlertRequest request) {
        log.info("📱 WhatsApp alert — Post: '{}', Rôles: {}", request.getPostTitle(), request.getTargetRoles());

        List<User> recipients = userRepository.findAll().stream()
                .filter(u -> request.getTargetRoles() != null &&
                             request.getTargetRoles().contains(u.getRole().name()))
                .filter(u -> u.getPhone() != null && !u.getPhone().isBlank())
                .collect(Collectors.toList());

        String messageBody = String.format(
            "🚨 *ALERTE MÉDICALE — MediCareAI*\n\n" +
            "📋 *%s*\n\n" +
            "%s\n\n" +
            "👤 Publié par : %s (%s)\n" +
            "🔗 Connectez-vous sur MediCareAI pour plus de détails.",
            request.getPostTitle(),
            request.getPostContent().length() > 200
                ? request.getPostContent().substring(0, 200) + "..."
                : request.getPostContent(),
            request.getAuthorName(),
            request.getAuthorRole()
        );

        int sentCount = 0;
        for (User user : recipients) {
            try {
                String toNumber = user.getPhone().startsWith("whatsapp:")
                        ? user.getPhone()
                        : "whatsapp:" + user.getPhone();

                Message.creator(
                        new PhoneNumber(toNumber),
                        new PhoneNumber(fromNumber),
                        messageBody
                ).create();

                log.info("✅ WhatsApp envoyé → {} ({})", user.getFullName(), user.getPhone());
                sentCount++;
            } catch (Exception e) {
                log.error("❌ Échec envoi WhatsApp → {} : {}", user.getPhone(), e.getMessage());
            }
        }

        return ResponseEntity.ok(WhatsAppAlertResponse.builder()
                .sent(sentCount > 0)
                .recipientCount(sentCount)
                .messageId(UUID.randomUUID().toString())
                .build());
    }

    // GET /api/forum/whatsapp/status/{postId}
    @GetMapping("/status/{postId}")
    public ResponseEntity<WhatsAppAlertResponse> getAlertStatus(@PathVariable Long postId) {
        return ResponseEntity.ok(WhatsAppAlertResponse.builder()
                .sent(false)
                .recipientCount(0)
                .messageId(null)
                .build());
    }
}
