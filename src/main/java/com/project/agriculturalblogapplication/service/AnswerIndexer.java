package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.enums.AiAnswerStatus;
import com.project.agriculturalblogapplication.events.AiAnswerReviewed;
import com.project.agriculturalblogapplication.events.AnswerChanged;
import com.project.agriculturalblogapplication.events.ExpertBadgeChanged;
import com.project.agriculturalblogapplication.events.QuestionChanged;
import com.project.agriculturalblogapplication.events.QuestionDeleted;
import com.project.agriculturalblogapplication.repositories.AnswerRepository;
import com.project.agriculturalblogapplication.repositories.QuestionAiAnswerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Set;

/**
 * Keeps the AI's knowledge of community answers in step with the database (roadmap 7.6). Only vetted answers are
 * indexed: an answer the asker accepted (not a self-accept), a direct answer by a Verified Expert in the question's topic, or an AI answer an
 * expert confirmed. Each listener re-reads the current state after the commit, on the single indexing thread,
 * so a rolled-back change never reaches the index, changes apply in order and every trigger uses the same rule.
 * Its transaction must not be read-only: the vector store writes through the same connection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerIndexer {

    private final AnswerRepository answerRepository;

    private final QuestionAiAnswerRepository aiAnswerRepository;

    private final ExpertService expertService;

    private final DocumentService documentService;

    @Async("indexingExecutor")
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(AnswerChanged event) {
        answerRepository.findById(event.answerId()).ifPresentOrElse(
                answer -> documentService.syncAnswer(answer, isVetted(answer)),
                () -> {
                    Answer gone = new Answer();
                    gone.setId(event.answerId());
                    documentService.syncAnswer(gone, false);
                });
    }

    @Async("indexingExecutor")
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ExpertBadgeChanged event) {
        Set<Long> expertTopics = expertTopicsOf(event.userId());
        answerRepository.findAllByUserIdAndQuestionCategoryIdAndParentAnswerIsNull(event.userId(), event.topicId())
                .forEach(answer -> documentService.syncAnswer(answer, isVetted(answer, expertTopics)));
    }

    /** A new title or topic changes what the question's answers are indexed with, and whether experts vetted them. */
    @Async("indexingExecutor")
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(QuestionChanged event) {
        answerRepository.findByQuestionIdAndParentAnswerIsNull(event.questionId())
                .forEach(answer -> documentService.syncAnswer(answer, isVetted(answer)));
        on(new AiAnswerReviewed(event.questionId()));
    }

    @Async("indexingExecutor")
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(AiAnswerReviewed event) {
        aiAnswerRepository.findByQuestionId(event.questionId()).ifPresent(aiAnswer ->
                documentService.syncAiAnswer(aiAnswer,
                        aiAnswer.getStatus() == AiAnswerStatus.CONFIRMED && !aiAnswer.getQuestion().isHidden()));
    }

    @Async("indexingExecutor")
    @TransactionalEventListener
    public void on(QuestionDeleted event) {
        documentService.deleteQuestion(event.questionId());
    }

    boolean isVetted(Answer answer) {
        return isVetted(answer, expertTopicsOf(answer.getUser().getId()));
    }

    /** Accepted by someone else (a self-accept vets nothing), or written by a Verified Expert in the topic. */
    private static boolean isVetted(Answer answer, Set<Long> authorExpertTopics) {
        Question question = answer.getQuestion();
        if (answer.getParentAnswer() != null || answer.isHidden() || question.isHidden()) {
            return false;
        }
        boolean acceptedByAsker = question.getAcceptedAnswer() != null
                && question.getAcceptedAnswer().getId().equals(answer.getId())
                && !question.getUser().getId().equals(answer.getUser().getId());
        return acceptedByAsker
                || (question.getCategory() != null && authorExpertTopics.contains(question.getCategory().getId()));
    }

    private Set<Long> expertTopicsOf(Long userId) {
        return expertService.verifiedTopics(List.of(userId)).getOrDefault(userId, Set.of());
    }
}
