package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.entities.Vote;
import com.project.agriculturalblogapplication.enums.VoteDirection;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.response.MyVotesResponse;
import com.project.agriculturalblogapplication.model.response.VoteResponse;
import com.project.agriculturalblogapplication.repositories.AnswerRepository;
import com.project.agriculturalblogapplication.repositories.QuestionRepository;
import com.project.agriculturalblogapplication.repositories.VoteRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VoteServiceTest {

    private static final String LANG = "en";
    private static final long VOTER = 7L;
    private static final long AUTHOR = 10L;

    private final VoteRepository voteRepository = mock(VoteRepository.class);
    private final QuestionService questionService = mock(QuestionService.class);
    private final AnswerService answerService = mock(AnswerService.class);
    private final QuestionRepository questionRepository = mock(QuestionRepository.class);
    private final AnswerRepository answerRepository = mock(AnswerRepository.class);
    private final UserService userService = mock(UserService.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final VoteService voteService = new VoteService(voteRepository, questionService, answerService,
            questionRepository, answerRepository, userService, authorization);

    private Question question;

    @BeforeEach
    void setUp() {
        question = new Question();
        question.setId(1L);
        question.setUser(user(AUTHOR));
        question.setScore(4);
        when(questionService.findByIdWithException(1L)).thenReturn(question);
        when(authorization.currentUserId(LANG)).thenReturn(VOTER);
        when(userService.findByIdWithException(VOTER, LANG)).thenReturn(user(VOTER));
    }

    @Test
    void aFirstUpvoteAddsOne() {
        VoteResponse response = voteService.voteOnQuestion(1L, VoteDirection.UP, LANG);

        assertEquals(new VoteResponse(5, VoteDirection.UP), response);
        verify(questionRepository).addToScore(1L, 1);
        verify(voteRepository).save(any(Vote.class));
    }

    @Test
    void switchingFromUpToDownMovesTheScoreByTwo() {
        when(voteRepository.findByUserIdAndQuestionId(VOTER, 1L)).thenReturn(Optional.of(vote(VoteDirection.UP)));

        assertEquals(new VoteResponse(2, VoteDirection.DOWN), voteService.voteOnQuestion(1L, VoteDirection.DOWN, LANG));
        verify(questionRepository).addToScore(1L, -2);
    }

    @Test
    void votingTheSameWayTwiceChangesNothing() {
        when(voteRepository.findByUserIdAndQuestionId(VOTER, 1L)).thenReturn(Optional.of(vote(VoteDirection.UP)));

        assertEquals(4, voteService.voteOnQuestion(1L, VoteDirection.UP, LANG).score());
        verify(questionRepository).addToScore(1L, 0);
    }

    @Test
    void withdrawingADownvoteGivesThePointBack() {
        Vote down = vote(VoteDirection.DOWN);
        when(voteRepository.findByUserIdAndQuestionId(VOTER, 1L)).thenReturn(Optional.of(down));

        VoteResponse response = voteService.voteOnQuestion(1L, null, LANG);

        assertEquals(new VoteResponse(5, null), response);
        verify(voteRepository).delete(down);
    }

    @Test
    void youCannotVoteOnYourOwnAnswer() {
        Answer answer = new Answer();
        answer.setId(3L);
        answer.setUser(user(VOTER));
        when(answerService.findByIdWithException(3L)).thenReturn(answer);

        ApplicationException refused = assertThrows(ApplicationException.class,
                () -> voteService.voteOnAnswer(3L, VoteDirection.UP, LANG));

        assertEquals(HttpStatus.FORBIDDEN, refused.getHttpStatus());
        verify(answerRepository, never()).addToScore(anyLong(), anyLong());
        verify(voteRepository, never()).save(any());
    }

    @Test
    void myVotesSplitsTheQuestionFromItsAnswers() {
        Answer answer = new Answer();
        answer.setId(3L);
        Vote onAnswer = vote(VoteDirection.DOWN);
        onAnswer.setAnswer(answer);
        when(voteRepository.findOnThread(VOTER, 1L)).thenReturn(List.of(vote(VoteDirection.UP), onAnswer));

        MyVotesResponse mine = voteService.myVotes(1L, LANG);

        assertEquals(VoteDirection.UP, mine.question());
        assertEquals(Map.of(3L, VoteDirection.DOWN), mine.answers());
        when(voteRepository.findOnThread(VOTER, 1L)).thenReturn(List.of());
        assertNull(voteService.myVotes(1L, LANG).question());
    }

    private static User user(long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private static Vote vote(VoteDirection direction) {
        Vote vote = new Vote();
        vote.setDirection(direction);
        return vote;
    }
}
