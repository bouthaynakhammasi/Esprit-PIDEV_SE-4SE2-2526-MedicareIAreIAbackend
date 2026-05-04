package com.aziz.demosec.service;

import com.aziz.demosec.exception.ContentViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class ImageModerationService {

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB

    private static final Set<String> ALLOWED_MIME = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final Set<String> ALLOWED_EXT = Set.of(
            "jpg", "jpeg", "png", "gif", "webp"
    );

    // Groq vision model — separate from the text model
    private static final String VISION_MODEL = "meta-llama/llama-4-scout-17b-16e-instruct";

    @Value("${groq.api-key}")
    private String groqApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Validates a MultipartFile image:
     * 1. Size limit (5 MB)
     * 2. MIME type whitelist
     * 3. Extension whitelist
     * 4. Groq vision: only medical images are accepted (fail-open if API is down)
     */
    public void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) return;

        // ── 1. Size ───────────────────────────────────────────
        if (image.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "Image is too large. Maximum allowed size is 5 MB.");
        }

        // ── 2. MIME type ──────────────────────────────────────
        String mime = image.getContentType();
        if (mime == null || !ALLOWED_MIME.contains(mime.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Invalid image format. Only JPEG, PNG, GIF and WEBP are accepted.");
        }

        // ── 3. Extension ──────────────────────────────────────
        String originalName = image.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            String ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
            if (!ALLOWED_EXT.contains(ext)) {
                throw new IllegalArgumentException(
                        "Invalid file extension. Only .jpg, .jpeg, .png, .gif, .webp are accepted.");
            }
        }

        // ── 4. Groq vision: medical domain check ──────────────
        checkIsMedicalImage(image, mime);
    }

    private void checkIsMedicalImage(MultipartFile image, String mime) {
        try {
            byte[] bytes = image.getBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String dataUri = "data:" + mime + ";base64," + base64;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            List<Map<String, Object>> content = List.of(
                    Map.of(
                            "type", "image_url",
                            "image_url", Map.of("url", dataUri)
                    ),
                    Map.of(
                            "type", "text",
                            "text", "You are a strict image classifier for a medical professional forum. " +
                                    "Examine this image and decide if it belongs to the medical domain. " +
                                    "MEDICAL images include: X-rays, MRI, CT scans, ultrasounds, ECG/EKG, " +
                                    "anatomical diagrams, histology slides, lab results, clinical photos of wounds/skin conditions, " +
                                    "surgical procedures, medical equipment, drug packaging, medical charts or reports. " +
                                    "NOT_MEDICAL images include: selfies, landscapes, food, animals, screenshots, " +
                                    "memes, social content, cars, buildings, or any non-medical subject. " +
                                    "Answer with a SINGLE word only: MEDICAL or NOT_MEDICAL."
                    )
            );

            Map<String, Object> body = Map.of(
                    "model", VISION_MODEL,
                    "messages", List.of(Map.of("role", "user", "content", content)),
                    "max_tokens", 10,
                    "temperature", 0
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.groq.com/openai/v1/chat/completions", request, Map.class);

            if (response.getBody() != null) {
                List<?> choices = (List<?>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                    Map<?, ?> msg = (Map<?, ?>) choice.get("message");
                    String answer = ((String) msg.get("content")).trim().toUpperCase();
                    log.info("Image domain classification: {}", answer);
                    if (!answer.contains("MEDICAL")) {
                        throw new ContentViolationException(
                                "Only medical images are allowed in this forum (X-rays, scans, clinical photos, medical diagrams, etc.).",
                                ""
                        );
                    }
                }
            }

        } catch (ContentViolationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Image classification API unavailable: {} — fail-open (allowing image)", e.getMessage());
        }
    }
}
