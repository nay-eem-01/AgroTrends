package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.config.AppProperties;
import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.QuestionAiAnswer;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.AiAnswerStatus;
import com.project.agriculturalblogapplication.enums.MailKind;
import com.project.agriculturalblogapplication.enums.NotificationTarget;
import com.project.agriculturalblogapplication.enums.NotificationType;
import com.project.agriculturalblogapplication.mail.Mail;
import com.project.agriculturalblogapplication.mail.MailService;
import com.project.agriculturalblogapplication.notification.NotificationMessage;
import com.project.agriculturalblogapplication.repositories.AnswerRepository;
import com.project.agriculturalblogapplication.repositories.QuestionAiAnswerRepository;
import com.project.agriculturalblogapplication.repositories.QuestionRepository;
import com.project.agriculturalblogapplication.security.service.UnsubscribeTokens;
import com.project.agriculturalblogapplication.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * E-mails the asker when their question gets an answer or an expert checks its AI answer, at most once an hour per
 * question: what arrives within the hour folds into the next mail. The mail is built from the database at send
 * time (what is new since the last one), so a burst of answers is one mail and a deleted answer is never mailed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerMailService {

    static final Duration MAIL_INTERVAL = Duration.ofHours(1);
    static final int EXCERPT_LENGTH = 300;
    private static final Set<NotificationType> TRIGGERS =
            Set.of(NotificationType.ANSWER_ON_YOUR_QUESTION, NotificationType.AI_ANSWER_REVIEWED);

    private final QuestionRepository questionRepository;

    private final AnswerRepository answerRepository;

    private final QuestionAiAnswerRepository questionAiAnswerRepository;

    private final MailService mailService;

    private final UnsubscribeTokens unsubscribeTokens;

    private final AppProperties appProperties;

    private final TransactionTemplate transactionTemplate;

    @Value("${app.frontendUrl}")
    private String frontendUrl;

    // After the answer's transaction commits, off the request thread; a mail failure never fails the answer.
    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void onNotification(NotificationMessage message) {
        if (!TRIGGERS.contains(message.type()) || message.targetType() != NotificationTarget.QUESTION) {
            return;
        }
        questionRepository.markAnswerMailPending(message.targetId());
        sendIfDue(message.targetId());
    }

    /** Mails that waited for their hour. */
    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT1M")
    public void sendWaiting() {
        questionRepository.answerMailsDue(LocalDateTime.now().minus(MAIL_INTERVAL)).forEach(this::sendIfDue);
    }

    void sendIfDue(Long questionId) {
        try {
            // Built in a transaction (the asker and answers are lazy), sent after it so SMTP holds no connection.
            Optional<Mail> mail = transactionTemplate.execute(status -> claim(questionId));
            if (mail != null) {
                mail.ifPresent(mailService::send);
            }
        } catch (RuntimeException e) {
            log.warn("Answer e-mail for question {} failed", questionId, e);
        }
    }

    private Optional<Mail> claim(Long questionId) {
        Optional<Question> found = questionRepository.findById(questionId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Question question = found.get();
        LocalDateTime previous = question.getAnswerMailedAt();
        LocalDateTime now = LocalDateTime.now();
        if (previous != null && previous.isAfter(now.minus(MAIL_INTERVAL))) {
            return Optional.empty(); // sendWaiting picks it up when the hour is over
        }
        int claimed = previous == null ? questionRepository.claimFirstAnswerMail(questionId, now)
                : questionRepository.claimNextAnswerMail(questionId, previous, now);
        return claimed == 1 ? mailFor(question, previous == null ? question.getCreationDate() : previous) : Optional.empty();
    }

    /** What is new since the last mail, for an asker who wants it at a confirmed address; empty otherwise. */
    Optional<Mail> mailFor(Question question, LocalDateTime since) {
        User asker = question.getUser();
        if (asker.getEmail() == null || asker.getEmailVerifiedAt() == null || !asker.isEmailOnAnswer()
                || asker.getBannedAt() != null || question.isHidden()) {
            return Optional.empty();
        }
        List<Answer> answers = answerRepository.newAnswersSince(question.getId(), since);
        AiAnswerStatus checked = questionAiAnswerRepository.findByQuestionId(question.getId())
                .filter(ai -> ai.getReviewedAt() != null && ai.getReviewedAt().isAfter(since))
                .map(QuestionAiAnswer::getStatus)
                .filter(status -> status == AiAnswerStatus.CONFIRMED || status == AiAnswerStatus.CORRECTED)
                .orElse(null);
        if (answers.isEmpty() && checked == null) {
            return Optional.empty();
        }
        boolean bn = !"en".equals(asker.getPreferredLang());
        StringBuilder text = new StringBuilder();
        if (answers.size() == 1) {
            Answer answer = answers.get(0);
            text.append(bn ? answer.getUser().getName() + " আপনার প্রশ্নের উত্তর দিয়েছেন:"
                            : answer.getUser().getName() + " answered your question:")
                    .append("\n\n").append(CommonUtils.excerpt(answer.getContent(), EXCERPT_LENGTH));
        } else if (answers.size() > 1) {
            text.append(bn ? "আপনার প্রশ্নে " + answers.size() + "টি নতুন উত্তর এসেছে।"
                    : answers.size() + " new answers to your question.");
        }
        if (checked != null) {
            if (!text.isEmpty()) {
                text.append("\n\n");
            }
            text.append(checked == AiAnswerStatus.CONFIRMED
                    ? (bn ? "একজন বিশেষজ্ঞ এআই উত্তরটি যাচাই করে সঠিক বলেছেন।" : "An expert checked the AI answer and confirmed it.")
                    : (bn ? "একজন বিশেষজ্ঞ এআই উত্তরটি সংশোধন করেছেন।" : "An expert corrected the AI answer."));
        }
        String unsubscribe = UriComponentsBuilder.fromUriString(appProperties.getBackendUrl())
                .path("/api/user/unsubscribe")
                .queryParam("token", unsubscribeTokens.tokenFor(asker.getId(), MailKind.answer))
                .queryParam("kind", MailKind.answer.name())
                .toUriString();
        return Optional.of(new Mail(asker.getEmail(), question.getTitle(), text.toString(),
                bn ? "উত্তর দেখুন" : "See the answer", frontendUrl + "/questions/" + question.getId(),
                bn ? "উত্তরের ই-মেইল বন্ধ করুন" : "Stop e-mails about answers", unsubscribe));
    }
}
