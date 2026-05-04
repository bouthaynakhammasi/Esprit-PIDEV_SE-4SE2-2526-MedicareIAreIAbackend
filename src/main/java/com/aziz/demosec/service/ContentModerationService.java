package com.aziz.demosec.service;

import com.aziz.demosec.client.PurgoMalumClient;
import com.aziz.demosec.exception.ContentViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentModerationService {

    private final PurgoMalumClient purgoMalumClient;
    private final RestTemplate groqRestTemplate = new RestTemplate();

    @Value("${groq.api-key}")
    private String groqApiKey;

    @Value("${groq.model}")
    private String groqModel;

    // Normalize leetspeak before checking: f4ck → fuck, @ss → ass
    private String normalize(String text) {
        return text.toLowerCase()
                .replace("4", "a").replace("3", "e")
                .replace("0", "o").replace("1", "i")
                .replace("@", "a").replace("$", "s")
                .replace("!", "i").replace("+", "t");
    }

    public void validateText(String text) {
        if (text == null || text.isBlank()) return;

        // Level 1 — PurgoMalum on original text
        PurgoMalumClient.Result result = purgoMalumClient.check(text);

        // Level 1b — PurgoMalum on normalized text (anti-leetspeak)
        PurgoMalumClient.Result resultNorm = purgoMalumClient.check(normalize(text));

        if (!result.hasBadWords() && !resultNorm.hasBadWords()) {
            log.debug("Moderation PASS — text clean");
            return;
        }

        // Level 2 — Groq: is it a medical context?
        boolean isMedical = isMedicalContext(text);
        if (isMedical) {
            log.info("Moderation PASS — medical context accepted for flagged text");
            return;
        }

        // Blocked
        String cleaned = result.cleanedText();
        log.warn("Moderation BLOCK — inappropriate text detected");
        throw new ContentViolationException(
                "Your message contains inappropriate language. Please revise your content.",
                cleaned
        );
    }

    private boolean isMedicalContext(String text) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            String prompt = """
                You are a medical content moderator. A user submitted this text in a medical forum:
                "%s"
                Does this text contain inappropriate/offensive language in a NON-medical context?
                Answer ONLY with: INAPPROPRIATE or MEDICAL_OK
                """.formatted(text);

            Map<String, Object> body = Map.of(
                "model", groqModel,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "max_tokens", 10,
                "temperature", 0
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = groqRestTemplate.postForEntity(
                    "https://api.groq.com/openai/v1/chat/completions", request, Map.class);

            if (response.getBody() != null) {
                List<?> choices = (List<?>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                    Map<?, ?> msg = (Map<?, ?>) choice.get("message");
                    String answer = ((String) msg.get("content")).trim().toUpperCase();
                    log.debug("Groq moderation answer: {}", answer);
                    return answer.contains("MEDICAL_OK");
                }
            }
        } catch (Exception e) {
            log.warn("Groq moderation failed: {} — fallback: allow", e.getMessage());
            return true; // fail open: if Groq is down, allow
        }
        return false;
    }
}
