package com.aziz.demosec.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {
    private Long id;
    private Long postId;
    private Long authorId;
    private String authorName;
    private String authorRole;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;
}