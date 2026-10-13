package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.ReputationEvent;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.ReputationReason;
import com.project.agriculturalblogapplication.enums.VoteDirection;
import com.project.agriculturalblogapplication.repositories.ReputationEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReputationServiceTest {

    private final ReputationEventRepository repository = mock(ReputationEventRepository.class);
    private final ReputationService reputation = new ReputationService(repository);
    private final List<ReputationEvent> ledger = new ArrayList<>();

    private final User asker = user(1L);
    private final User answerer = user(2L);
    private final Category rice = new Category();
    private Question question;
    private Answer answer;

    @BeforeEach
    void setUp() {
        when(repository.save(any(ReputationEvent.class))).thenAnswer(call -> {
            ledger.add(call.getArgument(0));
            return call.getArgument(0);
        });
        question = new Question();
        question.setUser(asker);
        question.setCategory(rice);
        answer = new Answer();
        answer.setUser(answerer);
        answer.setQuestion(question);
    }

    @Test
    void anAnswerUpvoteEarnsTenInTheQuestionsTopic() {
        reputation.answerVoteChanged(answer, null, VoteDirection.UP);

        ReputationEvent event = ledger.get(0);
        assertSame(answerer, event.getUser());
        assertSame(rice, event.getTopic());
        assertSame(answer, event.getAnswer());
        assertEquals(ReputationReason.ANSWER_UPVOTED, event.getReason());
        assertEquals(10, event.getPoints());
    }

    @Test
    void switchingAQuestionUpvoteToADownvoteReversesThenAppends() {
        reputation.questionVoteChanged(question, VoteDirection.UP, VoteDirection.DOWN);

        assertEquals(List.of(-5, -2), points());
        assertEquals(List.of(ReputationReason.QUESTION_UPVOTED, ReputationReason.DOWNVOTED),
                ledger.stream().map(ReputationEvent::getReason).toList());
        assertSame(question, ledger.get(0).getQuestion());
    }

    @Test
    void withdrawingADownvoteGivesTheTwoPointsBackAndRepeatingAVoteWritesNothing() {
        reputation.answerVoteChanged(answer, VoteDirection.DOWN, null);
        reputation.answerVoteChanged(answer, VoteDirection.UP, VoteDirection.UP);

        assertEquals(List.of(2), points());
    }

    @Test
    void acceptingGivesFifteenToTheAnswererAndTwoToTheAskerAndUnacceptingTakesThemBack() {
        reputation.accepted(answer);
        reputation.unaccepted(answer);

        assertEquals(List.of(15, 2, -15, -2), points());
        assertSame(answerer, ledger.get(0).getUser());
        assertSame(asker, ledger.get(1).getUser());
        assertEquals(0, points().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void acceptingYourOwnAnswerEarnsNothing() {
        answer.setUser(asker);

        reputation.accepted(answer);

        assertTrue(ledger.isEmpty());
    }

    private List<Integer> points() {
        return ledger.stream().map(ReputationEvent::getPoints).toList();
    }

    private static User user(long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
