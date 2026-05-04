package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChannelIdOrderByCreatedAtAsc(Long channelId);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.channel.id = :channelId AND m.isRead = false AND m.author.id <> :userId")
    long countUnread(@Param("channelId") Long channelId, @Param("userId") Long userId);

    List<ChatMessage> findTop50ByChannelIdOrderByCreatedAtDesc(Long channelId);
}
