package com.aziz.demosec.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponse {
    private Long id;
    private Long authorId;
    private String authorName;
    private String authorRole;       // Rôle de l'auteur (ex: DOCTOR)
    private String title;
    private String content;
    private String category;
    private String postType;         // DISCUSSION | ALERT | CLINICAL_CASE
    private String imageUrl;
    private LocalDateTime createdAt;
    private int commentsCount;       // Nombre de commentaires
    private int likesCount;          // Nombre de likes
    private List<CommentResponse> comments; // Liste des commentaires
    private boolean isLikedByUser;
    private String status;
}