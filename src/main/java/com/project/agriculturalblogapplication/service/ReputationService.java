package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.ReputationEvent;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.ReputationReason;
import com.project.agriculturalblogapplication.enums.VoteDirection;
import com.project.agriculturalblogapplication.model.response.ReputationResponse;
import com.project.agriculturalblogapplication.model.response.TopicReputation;
import com.project.agriculturalblogapplication.repositories.ReputationEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Writes the reputation ledger as votes and accepts change. Nothing is updated in place: taking something back
 * appends the same reason with negated points, so every point can be traced to a post.
 */
// shortcut: totals are summed on every read, fine for thousands of events per user; cache per user and topic when
// profile or list reads get slow.
@Service
@RequiredArgsConstructor
public class ReputationService {

    public static final int MIN_REPUTATION_TO_DOWNVOTE = 50;

    private final ReputationEventRepository reputationEventRepository;

    public long total(Long userId) {
        return reputationEventRepository.totalFor(userId);
    }

    /** Totals for several users in one query, for lists; users with no events are absent (read them as 0). */
    public Map<Long, Long> totals(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return reputationEventRepository.totalsFor(userIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    public ReputationResponse of(Long userId) {
        List<TopicReputation> topics = reputationEventRepository.byTopic(userId);
        return new ReputationResponse(topics.stream().mapToLong(TopicReputation::points).sum(), topics);
    }

    public void questionVoteChanged(Question question, VoteDirection before, VoteDirection after) {
        voteChanged(question.getUser(), question, null, before, after, ReputationReason.QUESTION_UPVOTED);
    }

    public void answerVoteChanged(Answer answer, VoteDirection before, VoteDirection after) {
        voteChanged(answer.getUser(), answer.getQuestion(), answer, before, after, ReputationReason.ANSWER_UPVOTED);
    }

    /** +15 to the answerer and +2 to the asker; nothing for accepting your own answer. */
    public void accepted(Answer answer) {
        acceptance(answer, 1);
    }

    public void unaccepted(Answer answer) {
        acceptance(answer, -1);
    }

    private void voteChanged(User author, Question question, Answer answer, VoteDirection before, VoteDirection after,
                             ReputationReason upvoted) {
        if (before == after) {
            return;
        }
        if (before != null) {
            record(author, question, answer, before == VoteDirection.UP ? upvoted : ReputationReason.DOWNVOTED, -1);
        }
        if (after != null) {
            record(author, question, answer, after == VoteDirection.UP ? upvoted : ReputationReason.DOWNVOTED, 1);
        }
    }

    private void acceptance(Answer answer, int sign) {
        Question question = answer.getQuestion();
        if (answer.getUser().getId().equals(question.getUser().getId())) {
            return;
        }
        record(answer.getUser(), question, answer, ReputationReason.ANSWER_ACCEPTED, sign);
        record(question.getUser(), question, answer, ReputationReason.ACCEPTED_AN_ANSWER, sign);
    }

    /** {@code answer} null means the points belong to the question itself. */
    private void record(User user, Question question, Answer answer, ReputationReason reason, int sign) {
        ReputationEvent event = new ReputationEvent();
        event.setUser(user);
        event.setTopic(question.getCategory());
        if (answer == null) {
            event.setQuestion(question);
        } else {
            event.setAnswer(answer);
        }
        event.setReason(reason);
        event.setPoints(sign * reason.getPoints());
        reputationEventRepository.save(event);
    }
}
