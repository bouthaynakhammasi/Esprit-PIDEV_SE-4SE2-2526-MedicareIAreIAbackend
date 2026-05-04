package com.aziz.demosec.service;

import com.aziz.demosec.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WsNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Envoie une notification à tous les admins connectés.
     * Topic : /topic/admin/notifications
     */
    public void notifyAdmin(String title, String message, String type) {
        NotificationMessage notif = NotificationMessage.of(title, message, type, null);
        log.info("[WS] → Admin : {}", title);
        messagingTemplate.convertAndSend("/topic/admin/notifications", notif);
    }

    /**
     * Envoie une notification à un patient spécifique.
     * Topic : /topic/patient/{patientId}/notifications
     */
    public void notifyPatient(Long patientId, String title, String message, String type) {
        NotificationMessage notif = NotificationMessage.of(title, message, type, patientId);
        log.info("[WS] → Patient #{} : {}", patientId, title);
        messagingTemplate.convertAndSend("/topic/patient/" + patientId + "/notifications", notif);
    }

    /**
     * Diffuse une notification à toutes les cliniques connectées.
     * Topic : /topic/clinic/notifications
     */
    public void notifyClinic(String title, String message, String type) {
        NotificationMessage notif = NotificationMessage.of(title, message, type, null);
        log.info("[WS] → Clinic : {}", title);
        messagingTemplate.convertAndSend("/topic/clinic/notifications", notif);
    }
}
