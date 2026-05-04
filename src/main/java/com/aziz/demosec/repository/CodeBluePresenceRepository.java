package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.CodeBluePresence;
import com.aziz.demosec.Entities.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CodeBluePresenceRepository extends JpaRepository<CodeBluePresence, Long> {

    // ── KEYWORD 1 ───────────────────────────────────────────────
    // All presences for a Code Blue post — used to display live list
    List<CodeBluePresence> findByPostId(Long postId);

    // ── KEYWORD 2 ───────────────────────────────────────────────
    // Staff who have NOT confirmed yet — used by escalation scheduler
    List<CodeBluePresence> findByPostIdAndConfirmedFalse(Long postId);

    // ── KEYWORD 3 ───────────────────────────────────────────────
    // Check if a specific role already confirmed — stops duplicate escalations
    boolean existsByPostIdAndRoleAndConfirmedTrue(Long postId, String role);

    // ── KEYWORD 4 ───────────────────────────────────────────────
    // Find presence for a specific staff email — used on confirm button click
    Optional<CodeBluePresence> findByPostIdAndStaffEmail(Long postId, String staffEmail);

    // ── JPQL 1 ──────────────────────────────────────────────────
    // Active CODE_BLUE posts where DOCTOR hasn't confirmed yet
    // AND triggered more than X minutes ago → needs escalation
    // Uses NOT EXISTS subquery to detect missing DOCTOR confirmation
    @Query("""
            SELECT DISTINCT p FROM Post p
            WHERE p.postType = 'CODE_BLUE'
              AND p.status = 'ACTIVE'
              AND p.escalated = false
              AND p.codeBlueTriggeredAt <= :deadline
              AND NOT EXISTS (
                  SELECT cbp FROM CodeBluePresence cbp
                  WHERE cbp.post = p
                    AND cbp.role = 'DOCTOR'
                    AND cbp.confirmed = true
              )
            """)
    List<Post> findCodeBlueNeedingEscalation(@Param("deadline") LocalDateTime deadline);

    // ── Native SQL 2 ────────────────────────────────────────────
    // Average response time in seconds per role — used in weekly report
    // Native query: TIMESTAMPDIFF is MySQL-specific, not supported in HQL/Hibernate 6 FUNCTION()
    @Query(value = """
            SELECT cbp.role,
                   COUNT(cbp.id),
                   AVG(TIMESTAMPDIFF(SECOND, p.code_blue_triggered_at, cbp.confirmed_at))
            FROM code_blue_presence cbp
            JOIN posts p ON cbp.post_id = p.id
            WHERE cbp.confirmed = true
              AND cbp.confirmed_at BETWEEN :from AND :to
            GROUP BY cbp.role
            ORDER BY AVG(TIMESTAMPDIFF(SECOND, p.code_blue_triggered_at, cbp.confirmed_at)) ASC
            """, nativeQuery = true)
    List<Object[]> findAvgResponseTimeByRole(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);
}
