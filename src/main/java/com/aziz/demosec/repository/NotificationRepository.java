package com.aziz.demosec.repository;

import com.aziz.demosec.domain.User;
import com.aziz.demosec.Entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Version oussema
    List<Notification> findByRecipientEmailAndIsReadFalseOrderByCreatedAtDesc(String email);
    List<Notification> findByRecipientEmailOrderByCreatedAtDesc(String email);

    @Modifying
    @Transactional
    void deleteAllByRecipient(User recipient);

    // Version main
    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);
    long countByRecipientAndIsReadFalse(User recipient);

    List<Notification> findByRecipient_IdOrderByCreatedAtDesc(Long recipientId);
    List<Notification> findByRecipient_IdAndIsReadFalse(Long recipientId);
    long countByRecipient_IdAndIsReadFalse(Long recipientId);
}