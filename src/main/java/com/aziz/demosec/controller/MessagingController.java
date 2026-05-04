package com.aziz.demosec.controller;

import com.aziz.demosec.dto.ChatChannelResponse;
import com.aziz.demosec.dto.ChatMessageRequest;
import com.aziz.demosec.dto.ChatMessageResponse;
import com.aziz.demosec.service.MessagingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forum/messaging")
@RequiredArgsConstructor
@Slf4j
public class MessagingController {

    private final MessagingService messagingService;

    // GET /api/forum/messaging/channels
    @GetMapping("/channels")
    public ResponseEntity<List<ChatChannelResponse>> getChannels() {
        log.info("📢 GET channels");
        return ResponseEntity.ok(messagingService.getChannels());
    }

    // GET /api/forum/messaging/channels/{channelId}/messages
    @GetMapping("/channels/{channelId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(@PathVariable Long channelId) {
        log.info("💬 GET messages - channel {}", channelId);
        return ResponseEntity.ok(messagingService.getMessages(channelId));
    }

    // POST /api/forum/messaging/channels/{channelId}/messages
    @PostMapping("/channels/{channelId}/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable Long channelId,
            @Valid @RequestBody ChatMessageRequest request) {
        log.info("📨 SEND message - channel {}", channelId);
        request.setChannelId(channelId);
        ChatMessageResponse msg = messagingService.sendMessage(channelId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(msg);
    }

    // GET /api/forum/messaging/channels/{channelId}/unread
    @GetMapping("/channels/{channelId}/unread")
    public ResponseEntity<Long> getUnreadCount(@PathVariable Long channelId) {
        return ResponseEntity.ok(messagingService.getUnreadCount(channelId));
    }
}
