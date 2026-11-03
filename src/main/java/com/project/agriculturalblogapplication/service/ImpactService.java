package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.model.response.UserImpactResponse;
import com.project.agriculturalblogapplication.repositories.QuestionAiAnswerRepository;
import com.project.agriculturalblogapplication.repositories.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** A member's impact for their profile; it need not be live, so each user's numbers are kept for an hour. */
@Service
@RequiredArgsConstructor
public class ImpactService {

    static final Duration CACHE_FOR = Duration.ofHours(1);

    private final UserService userService;

    private final AnswerService answerService;

    private final ExpertService expertService;

    private final QuestionRepository questionRepository;

    private final QuestionAiAnswerRepository questionAiAnswerRepository;

    // shortcut: per instance and unbounded (one small entry per profile viewed); use Spring's cache with a size limit
    // if profiles reach the tens of thousands.
    private final Map<Long, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(UserImpactResponse impact, Instant until) {
    }

    @Transactional(readOnly = true)
    public UserImpactResponse of(Long userId, String lang) {
        Cached cached = cache.get(userId);
        if (cached != null && cached.until().isAfter(Instant.now())) {
            return cached.impact();
        }
        userService.findByIdWithException(userId, lang);
        Set<Long> vetted = new HashSet<>(questionRepository.idsWithAnswerAcceptedFrom(userId));
        Set<Long> expertTopics = expertService.verifiedTopics(Set.of(userId)).getOrDefault(userId, Set.of());
        if (!expertTopics.isEmpty()) {
            vetted.addAll(questionRepository.idsAnsweredInTopics(userId, expertTopics));
        }
        UserImpactResponse impact = new UserImpactResponse(
                answerService.countPublicByUser(userId),
                answerService.countAcceptedByUser(userId),
                questionRepository.sumViewsOfQuestionsAnsweredBy(userId),
                vetted.isEmpty() ? 0 : questionAiAnswerRepository.countCitingQuestions(vetted),
                questionAiAnswerRepository.countStoryCitations(userId));
        cache.put(userId, new Cached(impact, Instant.now().plus(CACHE_FOR)));
        return impact;
    }
}
