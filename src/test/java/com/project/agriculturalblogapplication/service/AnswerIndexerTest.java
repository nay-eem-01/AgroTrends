package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.QuestionAiAnswer;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.AiAnswerStatus;
import com.project.agriculturalblogapplication.events.AiAnswerReviewed;
import com.project.agriculturalblogapplication.events.AnswerChanged;
import com.project.agriculturalblogapplication.events.ExpertBadgeChanged;
import com.project.agriculturalblogapplication.events.QuestionDeleted;
import com.project.agriculturalblogapplication.repositories.AnswerRepository;
import com.project.agriculturalblogapplication.repositories.QuestionAiAnswerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnswerIndexerTest {

    private static final long AUTHOR = 7L;
    private static final long TOPIC = 3L;

    private final AnswerRepository answerRepository = mock(AnswerRepository.class);
    private final QuestionAiAnswerRepository aiAnswerRepository = mock(QuestionAiAnswerRepository.class);
    private final ExpertService expertService = mock(ExpertService.class);
    private final DocumentService documentService = mock(DocumentService.class);
    private final AnswerIndexer indexer = new AnswerIndexer(answerRepository, aiAnswerRepository, expertService, documentService);

    private Question question;
    private Answer answer;

    @BeforeEach
    void setUp() {
        Category topic = new Category();
        topic.setId(TOPIC);
        question = new Question();
        question.setId(4L);
        question.setCategory(topic);
        User author = new User();
        author.setId(AUTHOR);
        answer = new Answer();
        answer.setId(8L);
        answer.setUser(author);
        answer.setQuestion(question);
    }

    @Test
    void anOrdinaryAnswerIsNotVetted() {
        assertFalse(indexer.isVetted(answer));
    }

    @Test
    void theAcceptedAnswerIsVetted() {
        question.setAcceptedAnswer(answer);

        assertTrue(indexer.isVetted(answer));
    }

    @Test
    void anAnswerByAVerifiedExpertInTheTopicIsVettedButNotInAnotherTopic() {
        when(expertService.verifiedTopics(List.of(AUTHOR))).thenReturn(Map.of(AUTHOR, Set.of(TOPIC)));
        assertTrue(indexer.isVetted(answer));

        when(expertService.verifiedTopics(List.of(AUTHOR))).thenReturn(Map.of(AUTHOR, Set.of(99L)));
        assertFalse(indexer.isVetted(answer));

        question.setCategory(null);
        assertFalse(indexer.isVetted(answer));
    }

    @Test
    void aReplyIsNeverVettedEvenByAnExpert() {
        when(expertService.verifiedTopics(List.of(AUTHOR))).thenReturn(Map.of(AUTHOR, Set.of(TOPIC)));
        answer.setParentAnswer(new Answer());

        assertFalse(indexer.isVetted(answer));
    }

    @Test
    void aChangedAnswerIsSyncedFromItsCurrentStateAndADeletedOneIsRemoved() {
        question.setAcceptedAnswer(answer);
        when(answerRepository.findById(8L)).thenReturn(Optional.of(answer));

        indexer.on(new AnswerChanged(8L));
        verify(documentService).syncAnswer(answer, true);

        indexer.on(new AnswerChanged(9L));
        ArgumentCaptor<Answer> gone = ArgumentCaptor.forClass(Answer.class);
        verify(documentService).syncAnswer(gone.capture(), eq(false));
        assertEquals(9L, gone.getValue().getId());
    }

    @Test
    void aBadgeChangeResyncsTheExpertsDirectAnswersInThatTopic() {
        when(answerRepository.findAllByUserIdAndQuestionCategoryIdAndParentAnswerIsNull(AUTHOR, TOPIC)).thenReturn(List.of(answer));

        indexer.on(new ExpertBadgeChanged(AUTHOR, TOPIC));

        verify(documentService).syncAnswer(answer, false);
    }

    @Test
    void onlyAConfirmedAiAnswerIsIndexed() {
        QuestionAiAnswer aiAnswer = new QuestionAiAnswer();
        when(aiAnswerRepository.findByQuestionId(4L)).thenReturn(Optional.of(aiAnswer));

        aiAnswer.setStatus(AiAnswerStatus.CONFIRMED);
        indexer.on(new AiAnswerReviewed(4L));
        verify(documentService).syncAiAnswer(aiAnswer, true);

        aiAnswer.setStatus(AiAnswerStatus.UNCHECKED);
        indexer.on(new AiAnswerReviewed(4L));
        verify(documentService).syncAiAnswer(aiAnswer, false);
    }

    @Test
    void listenersRunAsyncInTheirOwnWritableTransaction() {
        // Regressions: readOnly = true made Postgres refuse the vector store's DELETE, so nothing was indexed; and
        // Spring refuses to start when a transactional event listener's @Transactional is not REQUIRES_NEW.
        for (Method method : AnswerIndexer.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(TransactionalEventListener.class)) {
                assertEquals("indexingExecutor", method.getAnnotation(Async.class).value(), method.getName());
                Transactional tx = method.getAnnotation(Transactional.class);
                if (tx != null) {
                    assertFalse(tx.readOnly(), method.getName());
                    assertEquals(Propagation.REQUIRES_NEW, tx.propagation(), method.getName());
                }
            }
        }
    }

    @Test
    void aDeletedQuestionDropsItsAnswersFromTheIndex() {
        indexer.on(new QuestionDeleted(4L));

        verify(documentService).deleteQuestion(4L);
    }
}
