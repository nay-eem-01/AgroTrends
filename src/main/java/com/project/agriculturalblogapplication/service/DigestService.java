package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.config.AppProperties;
import com.project.agriculturalblogapplication.constatnt.AppConstants;
import com.project.agriculturalblogapplication.entities.DigestSent;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.QuestionAiAnswer;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.AiAnswerStatus;
import com.project.agriculturalblogapplication.enums.MailKind;
import com.project.agriculturalblogapplication.mail.Mail;
import com.project.agriculturalblogapplication.mail.MailService;
import com.project.agriculturalblogapplication.repositories.DigestSentRepository;
import com.project.agriculturalblogapplication.repositories.QuestionAiAnswerRepository;
import com.project.agriculturalblogapplication.repositories.QuestionRepository;
import com.project.agriculturalblogapplication.repositories.TagFollowRepository;
import com.project.agriculturalblogapplication.repositories.UserRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import com.project.agriculturalblogapplication.security.service.UnsubscribeTokens;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Monday's digest for answerers and experts: unanswered questions from the past week in the tags they follow, and for
 * Verified Experts the AI answers waiting in their review queue. Users with nothing to show get nothing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DigestService {

    static final int MAX_QUESTIONS = 5;

    private final UserRepository userRepository;

    private final TagFollowRepository tagFollowRepository;

    private final QuestionRepository questionRepository;

    private final QuestionAiAnswerRepository questionAiAnswerRepository;

    private final DigestSentRepository digestSentRepository;

    private final ExpertService expertService;

    private final AuthorizationService authorizationService;

    private final MailService mailService;

    private final UnsubscribeTokens unsubscribeTokens;

    private final AppProperties appProperties;

    private final TransactionTemplate transactionTemplate;

    @Value("${app.frontendUrl}")
    private String frontendUrl;

    // shortcut: one user at a time on the scheduler thread; fine for thousands of members, batch it beyond that.
    @Scheduled(cron = "0 0 8 * * MON", zone = "Asia/Dhaka")
    public void sendWeekly() {
        LocalDate week = LocalDate.now(AppConstants.APP_ZONE);
        int sent = 0;
        for (Long userId : userRepository.digestRecipientIds()) {
            try {
                Optional<Mail> mail = transactionTemplate.execute(status -> claim(userId, week));
                if (mail != null && mail.isPresent()) {
                    mailService.send(mail.get());
                    sent++;
                }
            } catch (DataIntegrityViolationException e) {
                // Another run already took this user's week.
            } catch (RuntimeException e) {
                log.warn("Weekly digest for user {} failed", userId, e);
            }
        }
        log.info("Weekly digest: {} sent", sent);
    }

    /** The caller's own digest as HTML, not sent, so the team can check the copy. */
    public String preview(String lang) {
        Optional<Mail> mail = transactionTemplate.execute(status -> userRepository
                .findById(authorizationService.currentUserId(lang)).flatMap(this::digestFor));
        return mail != null && mail.isPresent() ? mail.get().html()
                : "<p>Nothing to send you this week: follow tags with recent unanswered questions, or hold an expert badge.</p>";
    }

    private Optional<Mail> claim(Long userId, LocalDate week) {
        if (digestSentRepository.existsByUserIdAndWeekStart(userId, week)) {
            return Optional.empty();
        }
        Optional<Mail> mail = userRepository.findById(userId).flatMap(this::digestFor);
        if (mail.isPresent()) {
            digestSentRepository.saveAndFlush(new DigestSent(userId, week)); // the unique pair fails a second sender
        }
        return mail;
    }

    Optional<Mail> digestFor(User user) {
        boolean bn = !"en".equals(user.getPreferredLang());
        List<Long> tagIds = tagFollowRepository.followedTagIds(user.getId());
        List<Question> questions = tagIds.isEmpty() ? List.of() : questionRepository.unansweredWithTagsSince(tagIds,
                LocalDateTime.now().minus(Duration.ofDays(7)), user.getId(), PageRequest.of(0, MAX_QUESTIONS));
        Set<Long> expertTopics = expertService.verifiedTopics(Set.of(user.getId())).getOrDefault(user.getId(), Set.of());
        long waiting = expertTopics.isEmpty() ? 0 : questionAiAnswerRepository
                .countByStatusAndQuestionCategoryIdInAndQuestionHiddenFalse(AiAnswerStatus.UNCHECKED, expertTopics);
        if (questions.isEmpty() && waiting == 0) {
            return Optional.empty();
        }

        StringBuilder text = new StringBuilder();
        if (!questions.isEmpty()) {
            text.append(bn ? "আপনার অনুসরণ করা বিষয়ে এই প্রশ্নগুলোর এখনো উত্তর নেই:" : "These questions in your topics have no answer yet:");
            for (Question question : questions) {
                text.append("\n• ").append(question.getTitle()).append("\n  ").append(frontendUrl).append("/questions/").append(question.getId());
            }
        }
        if (waiting > 0) {
            long oldestDays = questionAiAnswerRepository
                    .findFirstByStatusAndQuestionCategoryIdInAndQuestionHiddenFalseOrderByCreationDateAsc(AiAnswerStatus.UNCHECKED, expertTopics)
                    .map(QuestionAiAnswer::getCreationDate)
                    .map(created -> Duration.between(created, LocalDateTime.now()).toDays())
                    .orElse(0L);
            if (!text.isEmpty()) {
                text.append("\n\n");
            }
            text.append(bn ? waiting + "টি এআই উত্তর আপনার যাচাইয়ের অপেক্ষায়; সবচেয়ে পুরনোটি " + oldestDays + " দিনের।"
                    : waiting + " AI answers are waiting for your review; the oldest is " + oldestDays + " days old.");
        }
        boolean reviewFirst = questions.isEmpty();
        String unsubscribe = UriComponentsBuilder.fromUriString(appProperties.getBackendUrl())
                .path("/api/user/unsubscribe")
                .queryParam("token", unsubscribeTokens.tokenFor(user.getId(), MailKind.digest))
                .queryParam("kind", MailKind.digest.name())
                .toUriString();
        return Optional.of(new Mail(user.getEmail(),
                bn ? "এই সপ্তাহে AgroTrends-এ আপনার জন্য" : "This week on AgroTrends",
                text.toString(),
                reviewFirst ? (bn ? "যাচাই শুরু করুন" : "Open your review queue") : (bn ? "উত্তর দিন" : "Answer a question"),
                frontendUrl + (reviewFirst ? "/review" : "/questions"),
                bn ? "সাপ্তাহিক ই-মেইল বন্ধ করুন" : "Stop the weekly e-mail", unsubscribe));
    }
}
