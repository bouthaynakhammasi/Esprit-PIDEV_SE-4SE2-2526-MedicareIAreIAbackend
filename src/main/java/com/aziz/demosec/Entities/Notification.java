package com.aziz.demosec.Entities;

import com.aziz.demosec.domain.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private PharmacyOrder order;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private NotificationType type;

    @Column(nullable = false)



    @JsonProperty("isRead")


    private boolean isRead;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Optional: ID of related entity (e.g. post for forum notifications)
    private Long relatedId;
}

