package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.response.AnswerResponse;
import com.project.agriculturalblogapplication.repositories.AnswerRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnswerServiceTest {

    private static final String LANG = "en";
    private static final long ASKER = 7L;

    private final AnswerRepository answerRepository = mock(AnswerRepository.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final ReputationService reputation = mock(ReputationService.class);
    private final AnswerService answerService = new AnswerService(mock(QuestionService.class), mock(UserService.class),
            authorization, answerRepository, reputation);

    private Question question;

    @BeforeEach
    void setUp() {
        question = new Question();
        question.setId(1L);
        question.setUser(user(ASKER));
    }

    @Test
    void theAskerAcceptsAnAnswerAndCanSwitchToAnother() {
        when(authorization.currentUserId(LANG)).thenReturn(ASKER);
        Answer first = answer(3L, 0);
        Answer second = answer(4L, 0);

        assertTrue(answerService.accept(3L, LANG).isAccepted());
        assertSame(first, question.getAcceptedAnswer());
        answerService.accept(4L, LANG);
        assertSame(second, question.getAcceptedAnswer());
        answerService.accept(4L, LANG);

        verify(reputation).accepted(first);
        verify(reputation).unaccepted(first);
        verify(reputation).accepted(second);
        verify(reputation, never()).unaccepted(second);
    }

    @Test
    void onlyTheAskerMayAccept() {
        when(authorization.currentUserId(LANG)).thenReturn(99L);
        answer(3L, 0);

        ApplicationException refused = assertThrows(ApplicationException.class, () -> answerService.accept(3L, LANG));

        assertEquals(HttpStatus.FORBIDDEN, refused.getHttpStatus());
        assertNull(question.getAcceptedAnswer());
    }

    @Test
    void aReplyCannotBeAccepted() {
        when(authorization.currentUserId(LANG)).thenReturn(ASKER);
        answer(3L, 0).setParentAnswer(new Answer());

        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ApplicationException.class, () -> answerService.accept(3L, LANG)).getHttpStatus());
    }

    @Test
    void unacceptingAnotherAnswerKeepsTheAcceptedOne() {
        when(authorization.currentUserId(LANG)).thenReturn(ASKER);
        Answer accepted = answer(3L, 0);
        answer(4L, 0);
        question.setAcceptedAnswer(accepted);

        assertFalse(answerService.unaccept(4L, LANG).isAccepted());
        assertSame(accepted, question.getAcceptedAnswer());
        answerService.unaccept(3L, LANG);
        assertNull(question.getAcceptedAnswer());
        verify(reputation).unaccepted(accepted);
    }

    @Test
    void deletingTheAcceptedAnswerClearsTheAccept() {
        // Regression: the question still pointed at the deleted answer and the flush failed with a 500.
        when(authorization.currentUserId(LANG)).thenReturn(ASKER);
        Answer accepted = answer(3L, 0);
        question.setAcceptedAnswer(accepted);

        answerService.delete(3L, LANG);

        assertNull(question.getAcceptedAnswer());
        verify(answerRepository).delete(accepted);
        verify(reputation, never()).unaccepted(any());
    }

    @Test
    void theAcceptedAnswerComesFirstThenHighestScoreThenOldest() {
        Answer oldLow = answer(1L, 1);
        Answer high = answer(2L, 9);
        Answer accepted = answer(3L, -2);
        Answer newLow = answer(4L, 1);
        question.setAcceptedAnswer(accepted);
        when(answerRepository.findByQuestionIdAndParentAnswerIsNull(1L)).thenReturn(List.of(newLow, oldLow, high, accepted));

        assertEquals(List.of(3L, 2L, 1L, 4L),
                answerService.getAllByQuestionId(1L).stream().map(AnswerResponse::getAnswerId).toList());
    }

    private Answer answer(long id, long score) {
        Answer answer = new Answer();
        answer.setId(id);
        answer.setScore(score);
        answer.setQuestion(question);
        answer.setUser(user(20L + id));
        when(answerRepository.findById(id)).thenReturn(Optional.of(answer));
        return answer;
    }

    private static User user(long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
