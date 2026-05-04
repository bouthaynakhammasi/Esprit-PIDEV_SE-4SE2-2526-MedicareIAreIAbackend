package com.aziz.demosec.Mapper;

import com.aziz.demosec.Entities.Comment;
import com.aziz.demosec.dto.CommentRequest;
import com.aziz.demosec.dto.CommentResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CommentMapper {

    public Comment toEntity(CommentRequest dto) {
        return Comment.builder()
                .content(dto.getContent())
                .build();
    }

    // Méthode simplifiée pour éviter la dépendance cyclique
    public CommentResponse toDto(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .authorName(comment.getAuthor().getFullName())
                .authorRole(comment.getAuthor().getRole().name())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    public List<Comment> toEntities(List<CommentRequest> dtos) {
        return dtos == null ? null : dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    public List<CommentResponse> toDtos(List<Comment> entities) {
        return entities == null ? null : entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public void updateFromDto(CommentRequest dto, Comment entity) {
        if (dto == null || entity == null) return;
        entity.setContent(dto.getContent());
    }
}