package com.aziz.demosec.service;

import com.aziz.demosec.Entities.Post;
import com.aziz.demosec.domain.User;
import com.aziz.demosec.dto.PostRequest;
import com.aziz.demosec.dto.PostResponse;
import com.aziz.demosec.exception.ResourceNotFoundException;
import com.aziz.demosec.Mapper.PostMapper;
import com.aziz.demosec.repository.PostRepository;
import com.aziz.demosec.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;

    private static final String UPLOAD_DIR = "uploads/posts/";

    private static final List<String> BAD_WORDS = List.of(
        "spam", "idiot", "stupid", "hate", "kill", "abuse"
    );

    private String filterBadWords(String text) {
        if (text == null) return null;
        String filtered = text;
        for (String word : BAD_WORDS) {
            filtered = filtered.replaceAll("(?i)" + word, "*".repeat(word.length()));
        }
        return filtered;
    }

    @Override
    public PostResponse create(@Valid PostRequest request, MultipartFile image) {
        log.info(" Création d'un post - Titre: {}", request.getTitle());
        
        // Vérifier que l'auteur existe
        User author = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + request.getAuthorId()));

        Post post = postMapper.toEntity(request);
        post.setAuthor(author);
        post.setCreatedAt(LocalDateTime.now());

        // Catégorie
        if (request.getCategory() != null) {
            post.setCategory(request.getCategory());
        }

        // Gestion de l'image
        if (image != null && !image.isEmpty()) {
            String imageUrl = saveImage(image);
            post.setImageUrl(imageUrl);
        }

        return postMapper.toDto(postRepository.save(post));
    }

    @Override
    public PostResponse getById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Post not found with id: " + id));
        return postMapper.toDto(post);
    }

    @Override
    public List<PostResponse> getAll() {
        return postRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(postMapper::toDto)
                .collect(Collectors.toList());
    }

    // ✅ Nouveau : filtrer par catégorie
    @Override
    public List<PostResponse> getByCategory(String category) {
        return postRepository.findByCategory(category)
                .stream()
                .map(postMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PostResponse> search(String keyword) {
        return postRepository
                .findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(keyword, keyword)
                .stream()
                .map(postMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PostResponse> getByAuthorId(Long authorId) {
        return postRepository.findByAuthorId(authorId)
                .stream()
                .map(postMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public PostResponse update(Long id, @Valid PostRequest request, MultipartFile image) {
        log.info("Mise à jour du post - ID: {}, Titre: {}", id, request.getTitle());

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Post not found with id: " + id));

        checkOwnership(post);

        postMapper.updateFromDto(request, post);

        if (request.getCategory() != null) {
            post.setCategory(request.getCategory());
        }

        if (image != null && !image.isEmpty()) {
            post.setImageUrl(saveImage(image));
        }

        return postMapper.toDto(postRepository.save(post));
    }

    @Override
    public void delete(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

        checkOwnership(post);

        postRepository.deleteById(id);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + auth.getName()));
    }

    private static final java.util.Set<String> MEDICAL_ROLES = java.util.Set.of(
        "DOCTOR", "CLINIC", "PHARMACIST", "LABORATORY_STAFF",
        "NUTRITIONIST", "HOME_CARE_PROVIDER"
    );

    private void checkOwnership(Post post) {
        User currentUser = getCurrentUser();
        String role = currentUser.getRole().name();
        boolean isAdmin = role.equals("ADMIN");
        boolean isMedicalStaffAndAuthor = MEDICAL_ROLES.contains(role)
                && post.getAuthor().getId().equals(currentUser.getId());
        if (!isAdmin && !isMedicalStaffAndAuthor) {
            throw new AccessDeniedException("You are not allowed to modify this post");
        }
    }

    private String saveImage(MultipartFile image) {
        try {
            // Créer le répertoire d'upload s'il n'existe pas
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Générer un nom de fichier unique
            String originalFilename = image.getOriginalFilename();
            String extension = originalFilename != null ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
            String filename = UUID.randomUUID().toString() + extension;

            // Sauvegarder le fichier
            Path filePath = uploadPath.resolve(filename);
            Files.copy(image.getInputStream(), filePath);

            // Retourner l'URL relative
            return "/" + UPLOAD_DIR + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save image", e);
        }
    }
}