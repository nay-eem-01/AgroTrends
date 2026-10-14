package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.util.CommonUtils;
import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.model.request.CreateAnswerRequest;
import com.project.agriculturalblogapplication.model.request.ReplyToAnswerRequest;
import com.project.agriculturalblogapplication.model.request.UpdateAnswerRequest;
import com.project.agriculturalblogapplication.model.AuthorStanding;
import com.project.agriculturalblogapplication.model.response.AnswerResponse;
import com.project.agriculturalblogapplication.repositories.AnswerRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final QuestionService questionService;

    private final UserService userService;

    private final AuthorizationService authorizationService;

    private final AnswerRepository answerRepository;

    private final ReputationService reputationService;

    public AnswerResponse create(CreateAnswerRequest request, String lang) {
        Question question = questionService.findByIdWithException(request.getQuestionId());

        User user = userService.findByIdWithException(authorizationService.currentUserId(lang), lang);

        Answer answer = new Answer();
        answer.setContent(request.getContent());
        answer.setUser(user);
        answer.setQuestion(question);
        answer = answerRepository.save(answer);

        return mapToAnswerResponse(answer);
    }

    public AnswerResponse update(UpdateAnswerRequest request, String lang) {
        Answer answer = findByIdWithException(request.getAnswerId());
        authorizationService.assertOwnerOrAdmin(answer.getUser().getId(), lang);
        answer.setContent(request.getContent());
        answer = answerRepository.save(answer);

        return mapToAnswerResponse(answer);
    }

    @Transactional
    public void delete(Long answerId, String lang) {
        Answer answer = findByIdWithException(answerId);
        authorizationService.assertOwnerOrAdmin(answer.getUser().getId(), lang);
        // The loaded question must not point at a deleted row when the session flushes. The answer's reputation
        // events go with it (ON DELETE CASCADE), so nothing is written to the ledger here.
        if (isAccepted(answer)) {
            answer.getQuestion().setAcceptedAnswer(null);
        }
        answerRepository.delete(answer);
    }

    /** Direct answers (replies come from viewReplies): the accepted one first, then highest score, then oldest. */
    public List<AnswerResponse> getAllByQuestionId(Long questionId) {
        return mapToAnswerResponses(answerRepository.findByQuestionIdAndParentAnswerIsNull(questionId).stream()
                .sorted(Comparator.comparing((Answer answer) -> !isAccepted(answer))
                        .thenComparing(Answer::getScore, Comparator.reverseOrder())
                        .thenComparing(Answer::getId))
                .toList());
    }

    /** The asker marks an answer as the one that solved the question, replacing any earlier pick. */
    @Transactional
    public AnswerResponse accept(Long answerId, String lang) {
        Answer answer = findByIdWithException(answerId);
        assertAsker(answer.getQuestion(), lang);
        if (answer.getParentAnswer() != null) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_CANNOT_ACCEPT_REPLY, lang);
        }
        Answer previous = answer.getQuestion().getAcceptedAnswer();
        if (!isAccepted(answer)) {
            if (previous != null) {
                reputationService.unaccepted(previous);
            }
            answer.getQuestion().setAcceptedAnswer(answer);
            reputationService.accepted(answer);
        }
        return mapToAnswerResponse(answer);
    }

    /** The asker takes back their accept; a no-op when this answer is not the accepted one. */
    @Transactional
    public AnswerResponse unaccept(Long answerId, String lang) {
        Answer answer = findByIdWithException(answerId);
        assertAsker(answer.getQuestion(), lang);
        if (isAccepted(answer)) {
            answer.getQuestion().setAcceptedAnswer(null);
            reputationService.unaccepted(answer);
        }
        return mapToAnswerResponse(answer);
    }

    private void assertAsker(Question question, String lang) {
        if (!question.getUser().getId().equals(authorizationService.currentUserId(lang))) {
            throw new ApplicationException(HttpStatus.FORBIDDEN, ErrorCode.ERROR_ONLY_ASKER_CAN_ACCEPT, lang);
        }
    }

    private static boolean isAccepted(Answer answer) {
        Answer accepted = answer.getQuestion().getAcceptedAnswer();
        return accepted != null && accepted.getId().equals(answer.getId());
    }

    public AnswerResponse replyToAnswer(ReplyToAnswerRequest request, String lang) {
        Question question = questionService.findByIdWithException(request.getQuestionId());

        User user = userService.findByIdWithException(authorizationService.currentUserId(lang), lang);

        Answer parentAnswer = findByIdWithException(request.getParentAnswerId());

        if (!parentAnswer.getQuestion().getId().equals(request.getQuestionId())) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_ANSWER_QUESTION_MISMATCH);
        }

        Answer reply = new Answer();
        reply.setContent(request.getContent());
        reply.setUser(user);
        reply.setQuestion(question);
        reply.setParentAnswer(parentAnswer);
        reply = answerRepository.save(reply);

        return mapToAnswerResponse(reply);
    }

    // Replies are a lazy collection: read them inside a transaction (open-in-view is off).
    @Transactional(readOnly = true)
    public List<AnswerResponse> viewReplies(Long parentAnswerId) {
        Answer parentAnswer = findByIdWithException(parentAnswerId);

        return mapToAnswerResponses(parentAnswer.getReplies());
    }

    public Answer findByIdWithException(Long answerId) {
        return answerRepository.findById(answerId).orElseThrow(() ->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_ANSWER_NOT_FOUND));
    }

    public AnswerResponse findById(Long answerId) {
        return mapToAnswerResponse(findByIdWithException(answerId));
    }


    private AnswerResponse mapToAnswerResponse(Answer answer) {
        return mapToAnswerResponse(answer, reputationService.standings(List.of(answer.getUser().getId())));
    }

    /** One reputation query for the whole list. */
    private List<AnswerResponse> mapToAnswerResponses(List<Answer> answers) {
        Map<Long, AuthorStanding> standings = reputationService.standings(answers.stream().map(a -> a.getUser().getId()).toList());
        return answers.stream().map(answer -> mapToAnswerResponse(answer, standings)).toList();
    }

    private AnswerResponse mapToAnswerResponse(Answer answer, Map<Long, AuthorStanding> standings) {
        AnswerResponse response = new AnswerResponse();
        response.setAnswerId(answer.getId());
        response.setUserId(answer.getUser().getId());
        response.setQuestionId(answer.getQuestion().getId());
        response.setContent(answer.getContent());
        response.setAuthorName(answer.getUser().getName());
        AuthorStanding author = standings.getOrDefault(answer.getUser().getId(), AuthorStanding.NONE);
        Category topic = answer.getQuestion().getCategory();
        response.setAuthorReputation(author.reputation());
        response.setAuthorBadges(author.badgesIn(topic == null ? null : topic.getId()));
        response.setCreatedAt(CommonUtils.toInstant(answer.getCreationDate()));
        response.setUpdatedAt(CommonUtils.toInstant(answer.getLastModifiedDate()));
        response.setScore(answer.getScore());
        response.setAccepted(isAccepted(answer));

        return response;
    }
}
