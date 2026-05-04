package com.aziz.demosec.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
@Slf4j
public class PurgoMalumClient {

    private static final String API_URL = "https://www.purgomalum.com/service/json";
    private final RestTemplate restTemplate = new RestTemplate();

    public record Result(boolean hasBadWords, String cleanedText) {}

    @SuppressWarnings("unchecked")
    public Result check(String text) {
        if (text == null || text.isBlank()) return new Result(false, text);
        try {
            String url = UriComponentsBuilder.fromHttpUrl(API_URL)
                    .queryParam("text", text)
                    .toUriString();

            Map<String, Object> body = restTemplate.getForObject(url, Map.class);
            if (body == null) return new Result(false, text);

            String cleaned = (String) body.get("result");
            if (cleaned == null) return new Result(false, text);

            boolean hasBadWords = cleaned.contains("*");
            log.debug("PurgoMalum: hasBadWords={} original='{}' cleaned='{}'", hasBadWords, text, cleaned);
            return new Result(hasBadWords, cleaned);

        } catch (Exception e) {
            log.warn("PurgoMalum API unavailable: {} — fallback: allow", e.getMessage());
            return new Result(false, text);
        }
    }
}
