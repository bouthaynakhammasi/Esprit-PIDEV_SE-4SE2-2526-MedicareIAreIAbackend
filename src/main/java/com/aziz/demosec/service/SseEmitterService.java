package com.aziz.demosec.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SseEmitterService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendToUser(String userEmail, Object data) {
        try {
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/notifications", data);
            log.debug("WS notification → {}", userEmail);
        } catch (Exception e) {
            log.warn("WS send failed for {}: {}", userEmail, e.getMessage());
        }
    }

    public void sendToAll(Object data) {
        try {
            messagingTemplate.convertAndSend("/topic/notifications", data);
            log.info("WS broadcast → all users");
        } catch (Exception e) {
            log.warn("WS broadcast failed: {}", e.getMessage());
        }
    }
}
