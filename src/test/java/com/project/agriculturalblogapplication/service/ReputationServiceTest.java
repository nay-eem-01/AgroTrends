package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.entities.ExpertBadge;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.ReputationEvent;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.Badge;
import com.project.agriculturalblogapplication.enums.ReputationReason;
import com.project.agriculturalblogapplication.enums.VoteDirection;
import com.project.agriculturalblogapplication.model.AuthorStanding;
import com.project.agriculturalblogapplication.model.TopicTotals;
import com.project.agriculturalblogapplication.model.response.ReputationResponse;
import com.project.agriculturalblogapplication.model.response.TopicReputation;
import com.project.agriculturalblogapplication.repositories.ReputationEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReputationServiceTest {

    private final ReputationEventRepository repository = mock(ReputationEventRepository.class);
    private final ExpertService expertService = mock(ExpertService.class);
    private final ReputationService reputation = new ReputationService(repository, expertService);
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
    void aProfileListsTopicsHighestFirstWithTheTopContributorBadgeOnlyWhereBothBarsAreMet() {
        when(repository.topicTotals(List.of(2L))).thenReturn(List.of(
                new TopicTotals(2L, 3L, "Fish", 400, 4),
                new TopicTotals(2L, null, null, 600, 9),
                new TopicTotals(2L, 1L, "Rice", 250, 5)));

        ReputationResponse response = reputation.of(2L);

        assertEquals(1250, response.total());
        assertEquals(Arrays.asList(null, 3L, 1L), response.topics().stream().map(TopicReputation::categoryId).toList());
        assertEquals(List.of(List.of(), List.of(), List.of(Badge.TOP_CONTRIBUTOR)),
                response.topics().stream().map(TopicReputation::badges).toList());
    }

    @Test
    void standingsForAListComeFromOneQueryAndShowBadgesOnlyInTheirTopic() {
        when(repository.topicTotals(List.of(1L, 2L))).thenReturn(List.of(
                new TopicTotals(2L, 1L, "Rice", 300, 6),
                new TopicTotals(2L, 3L, "Fish", 15, 1)));

        Map<Long, AuthorStanding> standings = reputation.standings(List.of(1L, 2L));

        assertEquals(315, standings.get(2L).reputation());
        assertEquals(List.of(Badge.TOP_CONTRIBUTOR), standings.get(2L).badgesIn(1L));
        assertEquals(List.of(), standings.get(2L).badgesIn(3L));
        assertEquals(List.of(), standings.get(2L).badgesIn(null));
        assertFalse(standings.containsKey(1L));
        assertEquals(Map.of(), reputation.standings(List.of()));
        verify(repository, never()).topicTotals(List.of());
    }

    @Test
    void aVerifiedExpertShowsTheBadgeInTheirTopicEvenWithoutPoints() {
        when(repository.topicTotals(List.of(1L, 2L))).thenReturn(List.of(new TopicTotals(2L, 1L, "Rice", 300, 6)));
        when(expertService.verifiedTopics(List.of(1L, 2L))).thenReturn(Map.of(1L, Set.of(3L), 2L, Set.of(1L)));

        Map<Long, AuthorStanding> standings = reputation.standings(List.of(1L, 2L));

        assertEquals(List.of(Badge.VERIFIED_EXPERT), standings.get(1L).badgesIn(3L));
        assertEquals(0, standings.get(1L).reputation());
        assertEquals(List.of(Badge.VERIFIED_EXPERT, Badge.TOP_CONTRIBUTOR), standings.get(2L).badgesIn(1L));
    }

    @Test
    void aProfileListsExpertTopicsWithTheirFieldIncludingTopicsWithNoPointsYet() {
        Category fish = new Category();
        fish.setId(3L);
        fish.setCategoryName("Fish");
        Category riceTopic = new Category();
        riceTopic.setId(1L);
        when(repository.topicTotals(List.of(2L))).thenReturn(List.of(new TopicTotals(2L, 1L, "Rice", 40, 1)));
        when(expertService.activeBadges(2L)).thenReturn(List.of(badge(fish, "Fisheries officer"), badge(riceTopic, "Agronomist")));

        ReputationResponse response = reputation.of(2L);

        assertEquals(40, response.total());
        assertEquals(List.of(
                        new TopicReputation(1L, "Rice", 40, 1, List.of(Badge.VERIFIED_EXPERT), "Agronomist"),
                        new TopicReputation(3L, "Fish", 0, 0, List.of(Badge.VERIFIED_EXPERT), "Fisheries officer")),
                response.topics());
    }

    private static ExpertBadge badge(Category topic, String field) {
        ExpertBadge badge = new ExpertBadge();
        badge.setTopic(topic);
        badge.setFieldTitle(field);
        return badge;
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
