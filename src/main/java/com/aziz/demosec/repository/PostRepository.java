package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // ✅ Filtrer par catégorie
    List<Post> findByCategory(String category);

    // ✅ Filtrer par auteur
    List<Post> findByAuthorId(Long authorId);

    // ✅ Trier par date décroissante
    List<Post> findAllByOrderByCreatedAtDesc();

    // ✅ Recherche full-text dans titre et contenu
    List<Post> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(String title, String content);

    @org.springframework.data.jpa.repository.Query("SELECT p.category, COUNT(p) as count FROM Post p GROUP BY p.category ORDER BY count DESC")
    List<Object[]> findTrendingCategories();

    // ── Code Blue ────────────────────────────────────────────────
    List<Post> findByPostTypeAndStatus(String postType, String status);

    // ── Trending Topics — JOIN comments + likes pour score engagement ──
    @org.springframework.data.jpa.repository.Query(
        "SELECT p, COUNT(DISTINCT c) AS commentCount, COUNT(DISTINCT lk) AS likeCount " +
        "FROM Post p " +
        "LEFT JOIN p.comments c " +
        "LEFT JOIN p.likes lk " +
        "WHERE p.createdAt > :since " +
        "AND (p.status IS NULL OR p.status <> 'ARCHIVED') " +
        "GROUP BY p " +
        "ORDER BY (COUNT(DISTINCT lk) * 2 + COUNT(DISTINCT c) * 3) DESC")
    List<Object[]> findRecentPostsWithEngagement(
        @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);

    // ── Auto-Archive — LEFT JOIN comments + likes, inactif si 0 commentaire récent ET < 2 likes ──
    @org.springframework.data.jpa.repository.Query(
        "SELECT p FROM Post p " +
        "LEFT JOIN p.comments c " +
        "LEFT JOIN p.likes lk " +
        "WHERE p.postType = 'DISCUSSION' " +
        "AND (p.status IS NULL OR p.status = 'ACTIVE') " +
        "AND p.createdAt < :cutoff " +
        "GROUP BY p " +
        "HAVING COALESCE(MAX(c.createdAt), p.createdAt) < :cutoff " +
        "AND COUNT(lk) < 2")
    List<Post> findInactiveDiscussions(
        @org.springframework.data.repository.query.Param("cutoff") java.time.LocalDateTime cutoff);
}