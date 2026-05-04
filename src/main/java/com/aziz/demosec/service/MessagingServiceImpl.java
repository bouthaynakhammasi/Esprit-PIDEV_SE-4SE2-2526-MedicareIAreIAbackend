package com.aziz.demosec.service;

import com.aziz.demosec.Entities.ChatChannel;
import com.aziz.demosec.Entities.ChatMessage;
import com.aziz.demosec.domain.User;
import com.aziz.demosec.dto.ChatChannelResponse;
import com.aziz.demosec.dto.ChatMessageRequest;
import com.aziz.demosec.dto.ChatMessageResponse;
import com.aziz.demosec.exception.ResourceNotFoundException;
import com.aziz.demosec.repository.ChatChannelRepository;
import com.aziz.demosec.repository.ChatMessageRepository;
import com.aziz.demosec.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessagingServiceImpl implements MessagingService {

    private final ChatChannelRepository channelRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;

    @Override
    public List<ChatChannelResponse> getChannels() {
        User currentUser = getCurrentUser();
        return channelRepository.findAll().stream()
                .map(ch -> ChatChannelResponse.builder()
                        .id(ch.getId())
                        .name(ch.getName())
                        .description(ch.getDescription())
                        .unreadCount((int) messageRepository.countUnread(ch.getId(), currentUser.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessageResponse> getMessages(Long channelId) {
        if (!channelRepository.existsById(channelId)) {
            throw new ResourceNotFoundException("Channel not found: " + channelId);
        }
        return messageRepository.findByChannelIdOrderByCreatedAtAsc(channelId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(Long channelId, ChatMessageRequest request) {
        ChatChannel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel not found: " + channelId));

        User author = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getAuthorId()));

        ChatMessage msg = ChatMessage.builder()
                .channel(channel)
                .author(author)
                .content(request.getContent().trim())
                .build();

        return toDto(messageRepository.save(msg));
    }

    @Override
    public long getUnreadCount(Long channelId) {
        User currentUser = getCurrentUser();
        return messageRepository.countUnread(channelId, currentUser.getId());
    }

    private ChatMessageResponse toDto(ChatMessage msg) {
        return ChatMessageResponse.builder()
                .id(msg.getId())
                .channelId(msg.getChannel().getId())
                .content(msg.getContent())
                .authorName(msg.getAuthor().getFullName())
                .authorRole(msg.getAuthor().getRole().name())
                .createdAt(msg.getCreatedAt())
                .build();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }
}
