package com.aziz.demosec.controller;

import com.aziz.demosec.Entities.CodeBluePresence;
import com.aziz.demosec.Entities.Post;
import com.aziz.demosec.repository.CodeBluePresenceRepository;
import com.aziz.demosec.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/code-blue")
@RequiredArgsConstructor
@Slf4j
public class CodeBlueController {

    private final CodeBluePresenceRepository codeBluePresenceRepository;
    private final PostRepository postRepository;

    // GET /api/code-blue/active — active CODE_BLUE posts (simple DTO, no circular refs)
    @GetMapping("/active")
    @Transactional
    public ResponseEntity<List<Map<String, Object>>> getActiveCodeBlues() {
        List<Post> posts = postRepository.findByPostTypeAndStatus("CODE_BLUE", "ACTIVE");
        List<Map<String, Object>> result = posts.stream().map(p -> Map.<String, Object>of(
                "id",                   p.getId(),
                "title",                p.getTitle(),
                "content",              p.getContent(),
                "authorName",           p.getAuthor().getFullName(),
                "codeBlueTriggeredAt",  p.getCodeBlueTriggeredAt() != null
                                            ? p.getCodeBlueTriggeredAt().toString() : "",
                "status",               p.getStatus() != null ? p.getStatus() : "ACTIVE",
                "meetLink",             p.getMeetLink() != null ? p.getMeetLink() : ""
        )).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // GET /api/code-blue/{postId}/presences — presence list (simple DTO, no lazy Post)
    @GetMapping("/{postId}/presences")
    public ResponseEntity<List<Map<String, Object>>> getPresences(@PathVariable Long postId) {
        List<CodeBluePresence> presences = codeBluePresenceRepository.findByPostId(postId);
        List<Map<String, Object>> result = presences.stream().map(p -> Map.<String, Object>of(
                "id",          p.getId(),
                "staffName",   p.getStaffName(),
                "staffEmail",  p.getStaffEmail(),
                "role",        p.getRole(),
                "confirmed",   p.getConfirmed(),
                "confirmedAt", p.getConfirmedAt() != null ? p.getConfirmedAt().toString() : ""
        )).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // POST /api/code-blue/{postId}/confirm — staff member confirms presence
    @PostMapping("/{postId}/confirm")
    @PreAuthorize("hasAnyRole('DOCTOR','CLINIC','PHARMACIST','LABORATORY_STAFF','NUTRITIONIST','HOME_CARE_PROVIDER','ADMIN')")
    @Transactional
    public ResponseEntity<Void> confirmPresence(@PathVariable Long postId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<CodeBluePresence> opt = codeBluePresenceRepository.findByPostIdAndStaffEmail(postId, email);
        opt.ifPresent(p -> {
            p.setConfirmed(true);
            p.setConfirmedAt(LocalDateTime.now());
            codeBluePresenceRepository.save(p);
            log.info("CODE BLUE confirmed: postId={}, staff={}", postId, email);
        });
        return ResponseEntity.ok().build();
    }

    // PUT /api/code-blue/{postId}/resolve — doctor or admin resolves the emergency
    @PutMapping("/{postId}/resolve")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Transactional
    public ResponseEntity<Void> resolve(@PathVariable Long postId) {
        postRepository.findById(postId).ifPresent(post -> {
            post.setStatus("RESOLVED");
            postRepository.save(post);
            log.info("CODE BLUE resolved: postId={}", postId);
        });
        return ResponseEntity.ok().build();
    }
}
