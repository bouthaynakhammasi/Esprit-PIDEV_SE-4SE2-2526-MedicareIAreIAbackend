package com.aziz.demosec.Entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "code_blue_presence")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CodeBluePresence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false, length = 50)
    private String role;

    @Column(nullable = false)
    private String staffName;

    @Column(nullable = false)
    private String staffEmail;

    private String staffPhone;

    @Builder.Default
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean confirmed = false;

    private LocalDateTime confirmedAt;
}
