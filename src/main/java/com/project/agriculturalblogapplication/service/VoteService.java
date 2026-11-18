package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/** Up and down votes on questions and answers: one per user and post, never on your own post. */
@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;

    private final QuestionService questionService;

    private final AnswerService answerService;

    private final QuestionRepository questionRepository;

    private final AnswerRepository answerRepository;

    private final UserService userService;

    private final AuthorizationService authorizationService;

    private final ReputationService reputationService;

    /** Casts, changes ({@code direction} set) or withdraws ({@code direction} null) the caller's vote on a question. */
    @Transactional
    public VoteResponse voteOnQuestion(Long questionId, VoteDirection direction, String lang) {
        Question question = questionService.findByIdWithException(questionId);
        Long userId = authorizationService.currentUserId(lang);
        VoteDirection before = apply(userId, question.getUser(),
                voteRepository.findByUserIdAndQuestionId(userId, questionId), direction, vote -> vote.setQuestion(question), lang);
        int delta = scoreChange(before, direction);
        questionRepository.addToScore(questionId, delta);
        reputationService.questionVoteChanged(question, before, direction);
        return new VoteResponse(question.getScore() + delta, direction);
    }

    /** Casts, changes ({@code direction} set) or withdraws ({@code direction} null) the caller's vote on an answer. */
    @Transactional
    public VoteResponse voteOnAnswer(Long answerId, VoteDirection direction, String lang) {
        Answer answer = answerService.findByIdWithException(answerId);
        Long userId = authorizationService.currentUserId(lang);
        VoteDirection before = apply(userId, answer.getUser(),
                voteRepository.findByUserIdAndAnswerId(userId, answerId), direction, vote -> vote.setAnswer(answer), lang);
        int delta = scoreChange(before, direction);
        answerRepository.addToScore(answerId, delta);
        reputationService.answerVoteChanged(answer, before, direction);
        return new VoteResponse(answer.getScore() + delta, direction);
    }

    /** The caller's votes on a question and its answers, so the page can highlight them. */
    @Transactional(readOnly = true)
    public MyVotesResponse myVotes(Long questionId, String lang) {
        VoteDirection onQuestion = null;
        Map<Long, VoteDirection> onAnswers = new HashMap<>();
        for (Vote vote : voteRepository.findOnThread(authorizationService.currentUserId(lang), questionId)) {
            if (vote.getAnswer() == null) {
                onQuestion = vote.getDirection();
            } else {
                onAnswers.put(vote.getAnswer().getId(), vote.getDirection());
            }
        }
        return new MyVotesResponse(onQuestion, onAnswers);
    }

    /** Saves or deletes the vote and returns the caller's previous vote (null when there was none). */
    private VoteDirection apply(Long userId, User author, Optional<Vote> existing, VoteDirection direction,
                                Consumer<Vote> target, String lang) {
        if (author.getId().equals(userId)) {
            throw new ApplicationException(HttpStatus.FORBIDDEN, ErrorCode.ERROR_CANNOT_VOTE_OWN_POST, lang);
        }
        VoteDirection before = existing.map(Vote::getDirection).orElse(null);
        if (direction == VoteDirection.DOWN && before != VoteDirection.DOWN
                && reputationService.total(userId) < ReputationService.MIN_REPUTATION_TO_DOWNVOTE) {
            throw new ApplicationException(HttpStatus.FORBIDDEN, ErrorCode.ERROR_DOWNVOTE_NEEDS_REPUTATION, lang);
        }
        if (direction == null) {
            existing.ifPresent(voteRepository::delete);
            return before;
        }
        Vote vote = existing.orElseGet(() -> {
            Vote created = new Vote();
            created.setUser(userService.findByIdWithException(userId, lang));
            target.accept(created);
            return created;
        });
        vote.setDirection(direction);
        voteRepository.save(vote);
        return before;
    }

    private static int scoreChange(VoteDirection before, VoteDirection after) {
        return (after == null ? 0 : after.getValue()) - (before == null ? 0 : before.getValue());
    }
}
