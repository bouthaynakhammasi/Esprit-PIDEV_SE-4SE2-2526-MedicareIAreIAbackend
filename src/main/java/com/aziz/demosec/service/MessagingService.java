package com.aziz.demosec.service;

import com.aziz.demosec.dto.ChatChannelResponse;
import com.aziz.demosec.dto.ChatMessageRequest;
import com.aziz.demosec.dto.ChatMessageResponse;

import java.util.List;

public interface MessagingService {
    List<ChatChannelResponse> getChannels();
    List<ChatMessageResponse> getMessages(Long channelId);
    ChatMessageResponse sendMessage(Long channelId, ChatMessageRequest request);
    long getUnreadCount(Long channelId);
}
