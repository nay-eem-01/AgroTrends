package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.events.QuestionChanged;
import com.project.agriculturalblogapplication.events.QuestionCreated;
import com.project.agriculturalblogapplication.events.QuestionDeleted;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.CreateQuestionRequest;
import com.project.agriculturalblogapplication.model.request.UpdateQuestionRequest;
import com.project.agriculturalblogapplication.model.response.QuestionResponse;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.Tag;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.model.AgriInfo;
import com.project.agriculturalblogapplication.model.AuthorStanding;
import com.project.agriculturalblogapplication.model.response.CategoryResponse;
import com.project.agriculturalblogapplication.repositories.AgriSpecifications;
import com.project.agriculturalblogapplication.repositories.QuestionRepository;
import com.project.agriculturalblogapplication.util.CommonUtils;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class QuestionService {

    static final Set<String> SORTABLE_FIELDS = Set.of("creationDate", "lastModifiedDate", "title");

    private final QuestionRepository questionRepository;

    private final UserService userService;

    private final AuthorizationService authorizationService;

    private final CategoryService categoryService;

    private final TagService tagService;

    private final ImageService imageService;

    private final ReputationService reputationService;

    private final ApplicationEventPublisher eventPublisher;

    /** Saves the question; the AI answers it after the commit (AiService.answerQuestion). */
    @Transactional
    public QuestionResponse create(CreateQuestionRequest request, String lang) {
        User user = userService.findByIdWithException(authorizationService.currentUserId(lang), lang);

        Question question = new Question();
        question.setTitle(request.getTitle());
        question.setContent(request.getContent());
        question.setUser(user);
        question.setCategory(categoryService.findByIdWithException(request.getCategoryId()));
        question.setTags(tagService.resolve(request.getTags() == null ? List.of() : request.getTags()));
        question.setImageUrls(imageService.requireUploaded(
                request.getImageUrls() == null ? List.of() : request.getImageUrls(), lang));
        if (request.getAgri() != null) {
            question.setAgri(request.getAgri().toMetadata());
        }
        question = questionRepository.save(question);
        eventPublisher.publishEvent(new QuestionCreated(question.getId()));

        return mapToQuestionResponse(question);
    }

    /** All questions, optionally filtered by topic, tag, no answers yet, crop, season, region and soil. */
    public Page<QuestionResponse> getAll(PaginationArgs paginationArgs, Long categoryId, String tag, boolean unanswered,
                                         AgriInfo filter, String lang) {
        Pageable pageable = CommonUtils.getPageable(paginationArgs, SORTABLE_FIELDS, lang);
        Specification<Question> spec = AgriSpecifications.<Question>matches(filter)
                .and(inTopicWithTag(categoryId, tag, unanswered));
        return mapToQuestionResponses(questionRepository.findAll(spec, pageable));
    }

    static Specification<Question> inTopicWithTag(Long categoryId, String tag, boolean unanswered) {
        String tagName = TagService.normalize(tag);
        return (root, query, cb) -> cb.and(
                cb.isFalse(root.get("hidden")),
                categoryId == null ? cb.conjunction() : cb.equal(root.get("category").get("id"), categoryId),
                tagName.isEmpty() ? cb.conjunction() : cb.equal(root.join("tags").get("name"), tagName),
                unanswered ? cb.equal(root.get("answerCount"), 0L) : cb.conjunction());
    }

    /** Full-text search over titles and text, best match first ('simple' config: works for Bangla and English). */
    public Page<QuestionResponse> search(String query, int pageNo, int pageSize, String lang) {
        if (query == null || query.isBlank()) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_SEARCH_QUERY_REQUIRED, lang);
        }
        return mapToQuestionResponses(
                questionRepository.search(query.trim(), CommonUtils.clampedPageable(pageNo, pageSize, Sort.unsorted())));
    }

    public Page<QuestionResponse> getAllByUser(PaginationArgs paginationArgs, Long userId, String lang) {
        Pageable pageable = CommonUtils.getPageable(paginationArgs, SORTABLE_FIELDS, lang);
        User user = userService.findByIdWithException(userId, lang);
        Page<Question> questions = questionRepository.findAllByUserAndHiddenFalse(user, pageable);
        return mapToQuestionResponses(questions);
    }

    @Transactional
    public QuestionResponse update(UpdateQuestionRequest request, String lang) {
        Question question = findByIdWithException(request.getQuestionId());
        authorizationService.assertOwnerOrAdmin(question.getUser().getId(), lang);

        question.setTitle(request.getTitle());
        question.setContent(request.getContent());
        question.setCategory(categoryService.findByIdWithException(request.getCategoryId()));
        // Omitted tags, photos or agri info keep the current values.
        if (request.getTags() != null) {
            question.setTags(tagService.resolve(request.getTags()));
        }
        if (request.getImageUrls() != null) {
            question.setImageUrls(imageService.requireUploaded(request.getImageUrls(), lang));
        }
        if (request.getAgri() != null) {
            question.setAgri(request.getAgri().toMetadata());
        }
        question = questionRepository.save(question);
        eventPublisher.publishEvent(new QuestionChanged(question.getId()));

        return mapToQuestionResponse(question);
    }

    /** Hidden questions can still be deleted, by their author or a moderator confirming the reports. */
    @Transactional
    public void delete(Long id, String lang) {
        Question question = findIncludingHidden(id);
        authorizationService.assertOwnerOrAdmin(question.getUser().getId(), lang);
        remove(question);
    }

    /**
     * Deletes after a moderator confirmed reports about it; the authorization is the moderation endpoint's.
     * Package-private: only ModerationService may skip the owner-or-admin check.
     */
    @Transactional
    void removeForModeration(Long id) {
        remove(findIncludingHidden(id));
    }

    private Question findIncludingHidden(Long id) {
        return questionRepository.findById(id).orElseThrow(() ->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_QUESTION_NOT_FOUND));
    }

    private void remove(Question question) {
        questionRepository.delete(question);
        eventPublisher.publishEvent(new QuestionDeleted(question.getId()));
    }

    /** A hidden question is a 404 here, so it cannot be read, edited, answered or voted on while under review. */
    public Question findByIdWithException(Long questionId) {
        return questionRepository.findById(questionId).filter(question -> !question.isHidden()).orElseThrow(() ->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_QUESTION_NOT_FOUND));
    }

    public QuestionResponse findById(Long questionId) {
        return mapToQuestionResponse(findByIdWithException(questionId));
    }

    private QuestionResponse mapToQuestionResponse(Question question) {
        return mapToQuestionResponse(question, reputationService.standings(List.of(question.getUser().getId())));
    }

    /** One reputation query for the whole page. */
    private Page<QuestionResponse> mapToQuestionResponses(Page<Question> questions) {
        Map<Long, AuthorStanding> standings = reputationService.standings(questions.map(q -> q.getUser().getId()).toSet());
        return questions.map(question -> mapToQuestionResponse(question, standings));
    }

    private QuestionResponse mapToQuestionResponse(Question question, Map<Long, AuthorStanding> standings) {
        QuestionResponse response = new QuestionResponse();
        response.setQuestionId(question.getId());
        response.setUserId(question.getUser().getId());
        response.setTitle(question.getTitle());
        response.setContent(question.getContent());
        response.setAuthorName(question.getUser().getName());
        AuthorStanding author = standings.getOrDefault(question.getUser().getId(), AuthorStanding.NONE);
        response.setAuthorReputation(author.reputation());
        response.setAuthorBadges(author.badgesIn(question.getCategory() == null ? null : question.getCategory().getId()));
        response.setAgri(AgriInfo.from(question.getAgri()));
        response.setCategory(question.getCategory() == null ? null : CategoryResponse.from(question.getCategory()));
        response.setTags(question.getTags().stream().map(Tag::getName).toList());
        response.setImageUrls(List.copyOf(question.getImageUrls()));
        response.setAnswerCount(question.getAnswerCount());
        response.setScore(question.getScore());
        // Regression: reading acceptedAnswer.hidden here hit a lazy proxy outside a session (500 on every read).
        response.setAcceptedAnswerId(question.getVisibleAcceptedAnswerId());
        response.setCreatedAt(CommonUtils.toInstant(question.getCreationDate()));
        response.setUpdatedAt(CommonUtils.toInstant(question.getLastModifiedDate()));

        return response;
    }
}
