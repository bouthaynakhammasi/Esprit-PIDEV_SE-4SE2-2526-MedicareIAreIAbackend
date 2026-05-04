package com.aziz.demosec.controller;

import com.aziz.demosec.dto.CommentRequest;
import com.aziz.demosec.dto.CommentResponse;
import com.aziz.demosec.dto.PostRequest;
import com.aziz.demosec.dto.PostResponse;
import com.aziz.demosec.dto.PostUploadRequest;
import com.aziz.demosec.service.ForumService;
import com.aziz.demosec.service.ImageModerationService;
import com.aziz.demosec.service.TrendingTopicsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
@Slf4j
public class ForumController {

    private final ForumService forumService;
    private final ImageModerationService imageModerationService;
    private final TrendingTopicsService trendingTopicsService;

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("ForumController is working!");
    }

    @GetMapping("/posts")
    public ResponseEntity<List<PostResponse>> getAllPosts() {
        return ResponseEntity.ok(forumService.getAllPosts());
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(forumService.getPostById(id));
    }

    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('DOCTOR', 'CLINIC', 'PHARMACIST', 'LABORATORY_STAFF', 'NUTRITIONIST', 'HOME_CARE_PROVIDER', 'ADMIN')")
    public ResponseEntity<PostResponse> createPost(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("category") String category,
            @RequestParam("authorId") String authorId,
            @RequestParam(value = "postType", required = false) String postType,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        log.info("📝 CREATE POST - title: {}, type: {}, author: {}", title, postType, authorId);

        if (title == null || title.trim().isEmpty()) return ResponseEntity.badRequest().build();
        if (content == null || content.trim().isEmpty()) return ResponseEntity.badRequest().build();
        if (category == null || category.trim().isEmpty()) return ResponseEntity.badRequest().build();
        if (authorId == null) return ResponseEntity.badRequest().build();

        imageModerationService.validateImage(image);

        PostUploadRequest uploadRequest = new PostUploadRequest();
        uploadRequest.setTitle(title.trim());
        uploadRequest.setContent(content.trim());
        uploadRequest.setCategory(category.trim());
        uploadRequest.setPostType(postType != null ? postType.trim() : "DISCUSSION");
        try {
            uploadRequest.setAuthorId(Long.parseLong(authorId));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }

        PostResponse created = forumService.createPostWithImage(uploadRequest, image);
        log.info("✅ Post created - ID: {}", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/posts/json")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CLINIC', 'PHARMACIST', 'LABORATORY_STAFF', 'NUTRITIONIST', 'HOME_CARE_PROVIDER', 'ADMIN')")
    public ResponseEntity<PostResponse> createPostJson(@Valid @RequestBody PostRequest request) {
        PostResponse created = forumService.createPost(request, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping(value = "/posts/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('DOCTOR', 'CLINIC', 'PHARMACIST', 'LABORATORY_STAFF', 'NUTRITIONIST', 'HOME_CARE_PROVIDER', 'ADMIN')")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "postType", required = false) String postType,
            @RequestParam(value = "authorId", required = false) String authorId,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        log.info("📝 UPDATE POST - ID: {}, title: {}", id, title);

        imageModerationService.validateImage(image);

        PostRequest request = new PostRequest();
        request.setTitle(title.trim());
        request.setContent(content.trim());
        request.setCategory(category != null ? category.trim() : "General Health");
        request.setPostType(postType != null ? postType.trim() : null);
        if (authorId != null) {
            try {
                request.setAuthorId(Long.parseLong(authorId));
            } catch (NumberFormatException ignored) {}
        }

        PostResponse updated = forumService.updatePost(id, request, image);
        log.info("✅ Post updated - ID: {}", updated.getId());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/posts/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CLINIC', 'PHARMACIST', 'LABORATORY_STAFF', 'NUTRITIONIST', 'HOME_CARE_PROVIDER', 'ADMIN')")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        log.info("🗑️ DELETE POST - ID: {}", id);
        forumService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{postId}/like")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CLINIC', 'PHARMACIST', 'LABORATORY_STAFF', 'NUTRITIONIST', 'HOME_CARE_PROVIDER', 'ADMIN', 'PATIENT')")
    public ResponseEntity<Void> likePost(@PathVariable Long postId) {
        forumService.likePost(postId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/posts/{postId}/like")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CLINIC', 'PHARMACIST', 'LABORATORY_STAFF', 'NUTRITIONIST', 'HOME_CARE_PROVIDER', 'ADMIN', 'PATIENT')")
    public ResponseEntity<Void> unlikePost(@PathVariable Long postId) {
        forumService.unlikePost(postId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommentResponse>> getPostComments(@PathVariable Long postId) {
        return ResponseEntity.ok(forumService.getCommentsByPost(postId));
    }

    @PostMapping("/posts/{postId}/comments")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CLINIC', 'PHARMACIST', 'LABORATORY_STAFF', 'NUTRITIONIST', 'HOME_CARE_PROVIDER', 'ADMIN', 'PATIENT')")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest request) {
        CommentResponse created = forumService.createComment(postId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/comments/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CLINIC', 'PHARMACIST', 'LABORATORY_STAFF', 'NUTRITIONIST', 'HOME_CARE_PROVIDER', 'ADMIN', 'PATIENT')")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.ok(forumService.updateComment(id, request));
    }

    @DeleteMapping("/comments/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CLINIC', 'PHARMACIST', 'LABORATORY_STAFF', 'NUTRITIONIST', 'HOME_CARE_PROVIDER', 'ADMIN')")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        forumService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trending-categories")
    public ResponseEntity<List<Map<String, Object>>> getTrendingCategories() {
        return ResponseEntity.ok(forumService.getTrendingCategories());
    }

    @GetMapping("/trending-keywords")
    public ResponseEntity<List<Map<String, Object>>> getTrendingKeywords() {
        return ResponseEntity.ok(trendingTopicsService.getTrending());
    }
}
