package com.aziz.demosec.service;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class VoiceRssService {

    @Value("${voicerss.api.key}")
    private String apiKey;

    @Value("${voicerss.api.url}")
    private String apiUrl;

    @Value("${voicerss.api.language}")
    private String language;

    @Value("${voicerss.api.codec}")
    private String codec;

    @Value("${voicerss.api.format}")
    private String format;

    private final RestTemplate restTemplate = createNonSslRestTemplate();
    
    private RestTemplate createNonSslRestTemplate() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {}
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {}
                public X509Certificate[] getAcceptedIssuers() { return null; }
            }}, new java.security.SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            return new RestTemplate();
        } catch (Exception e) {
            return new RestTemplate();
        }
    }

    public byte[] convertTextToSpeech(String text) {
        // VoiceRSS supports POST for longer texts
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = UriComponentsBuilder.newInstance()
                .queryParam("key", apiKey)
                .queryParam("hl", language)
                .queryParam("src", text)
                .queryParam("c", codec)
                .queryParam("f", format)
                .build()
                .getQuery();

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.postForEntity(apiUrl, request, byte[].class);
            byte[] audioData = response.getBody();

            // VoiceRSS returns errors as text starting with "ERROR:"
            if (audioData != null && audioData.length < 100) {
                String possibleError = new String(audioData);
                if (possibleError.startsWith("ERROR:")) {
                    throw new RuntimeException("VoiceRSS API error: " + possibleError);
                }
            }

            return audioData;
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert text to speech: " + e.getMessage());
        }
    }
}
