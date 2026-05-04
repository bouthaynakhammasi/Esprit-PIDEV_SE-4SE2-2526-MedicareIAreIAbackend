package com.aziz.demosec.service;

import com.aziz.demosec.Entities.Post;
import com.aziz.demosec.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostArchiveScheduler {

    private final PostRepository postRepository;

    // Runs every 2 minutes
    @Scheduled(fixedRate = 120_000, initialDelay = 5_000)
    @Transactional
    public void archiveInactivePosts() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🧹 [AUTO-ARCHIVE] Running at {}", LocalDateTime.now());
        try {
            // cutoff = 2 minutes ago — matches scheduler interval
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(2);

            // JPQL: LEFT JOIN comments + likes
            // Archive if: no comment after cutoff AND total likes < 2
            List<Post> inactivePosts = postRepository.findInactiveDiscussions(cutoff);

            log.info("🔍 [AUTO-ARCHIVE] Found {} inactive post(s) (no recent comment + < 2 likes)", inactivePosts.size());

            if (inactivePosts.isEmpty()) {
                log.info("✅ [AUTO-ARCHIVE] Nothing to archive.");
            } else {
                inactivePosts.forEach(post -> {
                    int likes    = post.getLikes()    != null ? post.getLikes().size()    : 0;
                    int comments = post.getComments() != null ? post.getComments().size() : 0;
                    log.info("   → Archiving post ID={} | '{}' | likes={} | comments={}",
                        post.getId(), post.getTitle(), likes, comments);
                    post.setStatus("ARCHIVED");
                });
                postRepository.saveAll(inactivePosts);
                log.info("✅ [AUTO-ARCHIVE] {} post(s) archived.", inactivePosts.size());
            }

        } catch (Exception e) {
            log.error("❌ [AUTO-ARCHIVE] Failed: {}", e.getMessage(), e);
        }
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}
