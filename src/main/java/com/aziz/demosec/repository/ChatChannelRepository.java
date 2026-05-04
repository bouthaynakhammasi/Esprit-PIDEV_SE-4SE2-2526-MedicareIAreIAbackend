package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.ChatChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatChannelRepository extends JpaRepository<ChatChannel, Long> {
    Optional<ChatChannel> findByName(String name);
    boolean existsByName(String name);
}
