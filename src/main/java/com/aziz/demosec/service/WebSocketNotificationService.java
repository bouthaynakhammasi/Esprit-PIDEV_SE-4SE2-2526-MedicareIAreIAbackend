package com.aziz.demosec.service;

import com.aziz.demosec.Entities.Notification;
import com.aziz.demosec.dto.pharmacy.NotificationResponseDTO;
import com.aziz.demosec.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service pour envoyer les notifications via WebSocket en temps réel.
 * 
 * Utilise convertAndSendToUser pour envoyer UNIQUEMENT à un utilisateur spécifique.
 */
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /**
     * Envoie une notification à un utilisateur spécifique via WebSocket.
     * 
     * @param userId L'ID de l'utilisateur destinataire
     * @param notification L'entité Notification à envoyer
     */
    public void notifyUser(Long userId, Notification notification) {
        if (userId == null) {
            System.err.println("❌ Cannot send notification - userId is null");
            return;
        }

        String email = userRepository.findById(userId)
                .map(u -> u.getEmail())
                .orElse(null);
        if (email == null) {
            System.err.println("❌ Cannot send notification - user not found: " + userId);
            return;
        }

        NotificationResponseDTO dto = convertToDTO(notification);
        messagingTemplate.convertAndSendToUser(email, "/queue/notifications", dto);
        System.out.println("✅ Notification sent to " + email + ": " + notification.getTitle());
    }

    /**
     * Broadcast une notification à tous les utilisateurs connectés (optional - pour les annonces générales).
     * 
     * @param notification L'entité Notification à broadcaster
     */
    public void broadcastNotification(Notification notification) {
        NotificationResponseDTO dto = convertToDTO(notification);

        // Envoyer à TOUS les utilisateurs abonnés à ce topic
        messagingTemplate.convertAndSend(
                "/topic/notifications",
                dto
        );

        System.out.println("📢 Notification broadcasted: " + notification.getTitle());
    }

    /**
     * Envoie une notification de mise à jour de statut à un utilisateur.
     * 
     * @param userId L'ID de l'utilisateur
     * @param orderId L'ID de la commande
     * @param status Le nouveau statut
     * @param message Le message à afficher
     */
    public void notifyOrderStatusUpdate(Long userId, Long orderId, String status, String message) {
        if (userId == null) return;

        String email = userRepository.findById(userId).map(u -> u.getEmail()).orElse(null);
        if (email == null) return;

        var update = new java.util.HashMap<String, Object>();
        update.put("orderId", orderId);
        update.put("status", status);
        update.put("message", message);
        update.put("timestamp", java.time.LocalDateTime.now());

        messagingTemplate.convertAndSendToUser(email, "/queue/order-updates", update);
        System.out.println("✅ Order update sent to " + email + " - Order #" + orderId + " status: " + status);
    }

    /**
     * Envoie une notification de paiement à un utilisateur.
     * 
     * @param userId L'ID de l'utilisateur
     * @param orderId L'ID de la commande
     * @param message Le message de paiement
     */
    public void notifyPayment(Long userId, Long orderId, String message) {
        if (userId == null) return;

        String email = userRepository.findById(userId).map(u -> u.getEmail()).orElse(null);
        if (email == null) return;

        var payment = new java.util.HashMap<String, Object>();
        payment.put("orderId", orderId);
        payment.put("message", message);
        payment.put("timestamp", java.time.LocalDateTime.now());

        messagingTemplate.convertAndSendToUser(email, "/queue/payments", payment);
        System.out.println("💰 Payment notification sent to " + email);
    }

    // ─────────────────────────────────────────────
    // Helper Method
    // ─────────────────────────────────────────────

    private NotificationResponseDTO convertToDTO(Notification notification) {
        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipient() != null ? notification.getRecipient().getId() : null)
                .orderId(notification.getOrder() != null ? notification.getOrder().getId() : null)
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}

