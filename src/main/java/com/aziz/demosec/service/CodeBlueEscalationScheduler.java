package com.aziz.demosec.service;

import com.aziz.demosec.Entities.Post;
import com.aziz.demosec.domain.Role;
import com.aziz.demosec.domain.User;
import com.aziz.demosec.repository.CodeBluePresenceRepository;
import com.aziz.demosec.repository.PostRepository;
import com.aziz.demosec.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CodeBlueEscalationScheduler {

    private final CodeBluePresenceRepository codeBluePresenceRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // Every 30 seconds: escalate if no DOCTOR confirmed within 5 minutes
    @Scheduled(fixedRate = 30_000)
    public void escalateUnrespondedCodeBlues() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(5);
        List<Post> needEscalation = codeBluePresenceRepository.findCodeBlueNeedingEscalation(deadline);

        for (Post post : needEscalation) {
            log.warn("CODE BLUE ESCALATION — Post ID: {}, no doctor confirmed after 5 min", post.getId());
            post.setEscalated(true);
            postRepository.save(post);

            List<User> doctors = userRepository.findByRole(Role.DOCTOR);
            for (User doc : doctors) {
                try {
                    emailService.sendCodeBlueEscalation(
                            doc.getEmail(), doc.getFullName(), post.getId(), post.getTitle());
                } catch (Exception e) {
                    log.warn("Escalation email failed for {}: {}", doc.getEmail(), e.getMessage());
                }
            }
        }

        if (!needEscalation.isEmpty()) {
            log.info("CODE BLUE ESCALATION — {} posts escalated", needEscalation.size());
        }
    }
}
