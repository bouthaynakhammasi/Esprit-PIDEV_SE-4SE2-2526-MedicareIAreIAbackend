package com.aziz.demosec.controller;

import com.aziz.demosec.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final WebSocketNotificationService webSocketNotificationService;

    /**
     * Endpoint pour vérifier la connexion WebSocket.
     * Le client envoie un message à /app/health et reçoit une réponse.
     */
    @MessageMapping("/health")
    @SendTo("/queue/health")
    public Map<String, String> health(Principal principal) {
        var response = new HashMap<String, String>();
        response.put("status", "ok");
        response.put("user", principal != null ? principal.getName() : "anonymous");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        System.out.println("✅ WebSocket connection established for user: " + 
                          (principal != null ? principal.getName() : "anonymous"));
        return response;
    }

    /**
     * Endpoint pour tester l'envoi d'une notification personnalisée.
     * (À utiliser principalement pour les tests)
     */
    @MessageMapping("/test-notification")
    public void testNotification(String message, Principal principal) {
        if (principal != null) {
            System.out.println("Test notification received from: " + principal.getName() + " - " + message);
        }
    }
}

