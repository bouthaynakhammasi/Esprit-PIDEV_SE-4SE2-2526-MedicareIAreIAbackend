package com.aziz.demosec.Mapper;

import com.aziz.demosec.Entities.Post;
import com.aziz.demosec.dto.PostRequest;
import com.aziz.demosec.dto.PostResponse;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.aziz.demosec.domain.User;
import com.aziz.demosec.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

@Component
public class PostMapper {

    private final UserRepository userRepository;

    public PostMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Post toEntity(PostRequest dto) {
        return Post.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .category(dto.getCategory())
                .postType(dto.getPostType() != null ? dto.getPostType() : "DISCUSSION")
                .build();
    }

    public PostResponse toDto(Post post) {
        // Get current user for like status
        boolean isLikedByUser = false;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String email = authentication.getName();
                Optional<User> currentUser = userRepository.findByEmail(email);
                if (currentUser.isPresent()) {
                    isLikedByUser = post.getLikes() != null && 
                        post.getLikes().stream()
                            .anyMatch(like -> like.getUser().getId().equals(currentUser.get().getId()));
                }
            }
        } catch (Exception e) {
            // User not authenticated or other error, default to false
            isLikedByUser = false;
        }

        return PostResponse.builder()
                .id(post.getId())
                .authorId(post.getAuthor().getId())
                .authorName(post.getAuthor().getFullName())
                .authorRole(post.getAuthor().getRole().name()) //
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .postType(post.getPostType() != null ? post.getPostType() : "DISCUSSION")
                .imageUrl(post.getImageUrl())
                .createdAt(post.getCreatedAt())
                .commentsCount(post.getComments() != null ? post.getComments().size() : 0) // 
                .likesCount(post.getLikes() != null ? post.getLikes().size() : 0)          // 
                .comments(null) // Éviter la dépendance cyclique pour l'instant
                .isLikedByUser(isLikedByUser)
                .status(post.getStatus())
                .build();
    }

    public List<Post> toEntities(List<PostRequest> dtos) {
        return dtos == null ? null : dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    public List<PostResponse> toDtos(List<Post> entities) {
        return entities == null ? null : entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public void updateFromDto(PostRequest dto, Post entity) {
        if (dto == null || entity == null) return;
        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setCategory(dto.getCategory());
        if (dto.getPostType() != null) entity.setPostType(dto.getPostType());
    }
}