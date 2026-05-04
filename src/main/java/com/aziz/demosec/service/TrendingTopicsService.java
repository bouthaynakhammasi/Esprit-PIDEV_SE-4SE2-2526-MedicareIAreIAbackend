package com.aziz.demosec.service;

import com.aziz.demosec.Entities.Post;
import com.aziz.demosec.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendingTopicsService {

    private final PostRepository postRepository;

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "the","a","an","is","in","of","and","or","to","for","with","this","that",
        "it","be","was","are","on","at","by","from","have","has","had","not","but",
        "as","if","we","i","you","he","she","they","my","your","his","her","our",
        "their","can","will","would","could","should","may","might","do","does",
        "did","been","being","get","got","am","its","about","than","more","also",
        "no","so","up","out","were","shall","just","very","too","only","even",
        "de","la","le","les","du","des","un","une","et","ou","en","dans","sur",
        "pour","par","avec","qui","que","ne","pas","est","sont","au","aux","je",
        "il","elle","nous","vous","ils","elles","me","te","se","lui","leur","y","on",
        "post","patient","case","medical","health","care"
    ));

    private volatile List<Map<String, Object>> cachedTrending = new ArrayList<>();

    // Runs every 2 minutes
    @Scheduled(fixedRate = 120_000, initialDelay = 10_000)
    public void refreshTrendingTopics() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔥 [TRENDING] Running at {}", LocalDateTime.now());
        try {
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

            // JPQL with JOIN on comments + likes → returns Object[]{Post, commentCount, likeCount}
            List<Object[]> rows = postRepository.findRecentPostsWithEngagement(sevenDaysAgo);

            log.info("📄 [TRENDING] Analyzing {} post(s) from last 7 days", rows.size());

            // word → weighted score (freq × engagement boost)
            Map<String, Double> wordScore = new HashMap<>();
            Map<String, Long>   wordCount = new HashMap<>();

            for (Object[] row : rows) {
                Post post         = (Post) row[0];
                long commentCount = ((Number) row[1]).longValue();
                long likeCount    = ((Number) row[2]).longValue();

                // engagement boost: more likes/comments = keywords rank higher
                double boost = 1.0 + (likeCount * 0.5) + (commentCount * 1.0);

                List<String> words = new ArrayList<>();
                words.addAll(extractWords(post.getTitle()));
                words.addAll(extractWords(post.getContent()));

                for (String word : words) {
                    wordScore.merge(word, boost, Double::sum);
                    wordCount.merge(word, 1L, Long::sum);
                }
            }

            cachedTrending = wordScore.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Map<String, Object> kw = new LinkedHashMap<>();
                    kw.put("word",  e.getKey());
                    kw.put("count", wordCount.getOrDefault(e.getKey(), 1L));
                    kw.put("score", Math.round(e.getValue() * 10.0) / 10.0);
                    return kw;
                })
                .collect(Collectors.toList());

            if (cachedTrending.isEmpty()) {
                log.info("⚠️  [TRENDING] No keywords found.");
            } else {
                log.info("✅ [TRENDING] Top keywords (word | mentions | score):");
                cachedTrending.forEach(kw ->
                    log.info("   → #{} | {} mentions | score {}", kw.get("word"), kw.get("count"), kw.get("score"))
                );
            }

        } catch (Exception e) {
            log.error("❌ [TRENDING] Failed: {}", e.getMessage(), e);
        }
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    public List<Map<String, Object>> getTrending() {
        return cachedTrending;
    }

    private List<String> extractWords(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.toLowerCase().replaceAll("[^a-zà-ÿ\\s]", " ").split("\\s+"))
            .filter(w -> w.length() > 3)
            .filter(w -> !STOP_WORDS.contains(w))
            .collect(Collectors.toList());
    }
}
