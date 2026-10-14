package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.entities.ExpertApplication;
import com.project.agriculturalblogapplication.entities.ExpertBadge;
import com.project.agriculturalblogapplication.enums.ApplicationStatus;
import com.project.agriculturalblogapplication.events.ExpertBadgeChanged;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.ExpertApplicationRequest;
import com.project.agriculturalblogapplication.model.request.ExpertDecisionRequest;
import com.project.agriculturalblogapplication.model.response.ExpertApplicationResponse;
import com.project.agriculturalblogapplication.repositories.ExpertApplicationRepository;
import com.project.agriculturalblogapplication.repositories.ExpertBadgeRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import com.project.agriculturalblogapplication.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Expert applications: a user sends proof for some topics; an admin decides (roadmap 7.1, 7.2). */
@Service
@RequiredArgsConstructor
public class ExpertService {

    private final ExpertApplicationRepository applicationRepository;

    private final ExpertBadgeRepository badgeRepository;

    private final UserService userService;

    private final CategoryService categoryService;

    private final ImageService imageService;

    private final AuthorizationService authorizationService;

    private final ApplicationEventPublisher eventPublisher;

    /** One application waits at a time; after a decision the user may apply again. */
    @Transactional
    public ExpertApplicationResponse apply(ExpertApplicationRequest request, String lang) {
        Long userId = authorizationService.currentUserId(lang);
        if (applicationRepository.existsByUserIdAndStatus(userId, ApplicationStatus.PENDING)) {
            throw new ApplicationException(HttpStatus.CONFLICT, ErrorCode.ERROR_EXPERT_APPLICATION_PENDING, lang);
        }
        List<String> images = request.getProofImageUrls() == null ? List.of() : request.getProofImageUrls();
        List<String> links = request.getLinks() == null ? List.of() : request.getLinks();
        if (images.isEmpty() && links.isEmpty()) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_EXPERT_PROOF_REQUIRED, lang);
        }

        ExpertApplication application = new ExpertApplication();
        application.setUser(userService.findByIdWithException(userId, lang));
        application.setTopics(request.getCategoryIds().stream().map(categoryService::findByIdWithException)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        application.setFieldTitle(request.getFieldTitle().strip());
        application.setCredentialType(request.getCredentialType());
        application.setInstitution(request.getInstitution().strip());
        application.setProofImageUrls(imageService.requireUploaded(images, lang));
        application.setLinks(links);
        return ExpertApplicationResponse.from(applicationRepository.save(application));
    }

    /** The caller's own applications, newest first. */
    @Transactional(readOnly = true)
    public Page<ExpertApplicationResponse> mine(int pageNo, int pageSize, String lang) {
        return applicationRepository.findAllByUserId(authorizationService.currentUserId(lang),
                        CommonUtils.clampedPageable(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "creationDate")))
                .map(ExpertApplicationResponse::from);
    }

    /** Admin queue: applications in one status, oldest first. */
    @Transactional(readOnly = true)
    public Page<ExpertApplicationResponse> applications(ApplicationStatus status, int pageNo, int pageSize) {
        return applicationRepository.findAllByStatus(status,
                        CommonUtils.clampedPageable(pageNo, pageSize, Sort.by(Sort.Direction.ASC, "creationDate")))
                .map(ExpertApplicationResponse::from);
    }

    /** Grants a badge in each approved topic: all requested topics, or the subset in {@code categoryIds}. */
    @Transactional
    public ExpertApplicationResponse approve(Long applicationId, ExpertDecisionRequest decision, String lang) {
        ExpertApplication application = pending(applicationId, lang);
        Set<Category> granted = application.getTopics();
        if (decision.getCategoryIds() != null) {
            Set<Long> wanted = Set.copyOf(decision.getCategoryIds());
            granted = granted.stream().filter(topic -> wanted.contains(topic.getId())).collect(Collectors.toSet());
            if (granted.isEmpty() || granted.size() != wanted.size()) {
                throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_EXPERT_TOPICS_NOT_REQUESTED, lang);
            }
        }
        for (Category topic : granted) {
            ExpertBadge badge = badgeRepository.findByUserIdAndTopicId(application.getUser().getId(), topic.getId())
                    .orElseGet(ExpertBadge::new);
            badge.setUser(application.getUser());
            badge.setTopic(topic);
            badge.setFieldTitle(application.getFieldTitle());
            badge.setRevokedAt(null);
            badgeRepository.save(badge);
            eventPublisher.publishEvent(new ExpertBadgeChanged(application.getUser().getId(), topic.getId()));
        }
        return decide(application, ApplicationStatus.APPROVED, decision.getNote());
    }

    @Transactional
    public ExpertApplicationResponse reject(Long applicationId, ExpertDecisionRequest decision, String lang) {
        ExpertApplication application = pending(applicationId, lang);
        if (decision.getNote() == null || decision.getNote().isBlank()) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_EXPERT_REJECT_REASON_REQUIRED, lang);
        }
        return decide(application, ApplicationStatus.REJECTED, decision.getNote());
    }

    /** Takes a Verified Expert badge away (harmful advice, false proof); the user may apply again. */
    @Transactional
    public void revoke(Long userId, Long topicId, String lang) {
        ExpertBadge badge = badgeRepository.findByUserIdAndTopicId(userId, topicId)
                .filter(found -> found.getRevokedAt() == null)
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_EXPERT_BADGE_NOT_FOUND, lang));
        badge.setRevokedAt(LocalDateTime.now());
        badgeRepository.save(badge);
        eventPublisher.publishEvent(new ExpertBadgeChanged(userId, topicId));
    }

    /** The user's active badges, with their topics loaded. */
    public List<ExpertBadge> activeBadges(Long userId) {
        return badgeRepository.findAllByUserIdAndRevokedAtIsNull(userId);
    }

    /** For each of these users who holds any, the topics they are a Verified Expert in. */
    public Map<Long, Set<Long>> verifiedTopics(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return badgeRepository.activeTopics(userIds).stream().collect(Collectors.groupingBy(row -> (Long) row[0],
                Collectors.mapping(row -> (Long) row[1], Collectors.toSet())));
    }

    private ExpertApplication pending(Long applicationId, String lang) {
        ExpertApplication application = applicationRepository.findById(applicationId).orElseThrow(() ->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_EXPERT_APPLICATION_NOT_FOUND, lang));
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new ApplicationException(HttpStatus.CONFLICT, ErrorCode.ERROR_EXPERT_APPLICATION_DECIDED, lang);
        }
        return application;
    }

    private static ExpertApplicationResponse decide(ExpertApplication application, ApplicationStatus status, String note) {
        application.setStatus(status);
        application.setReviewNote(note == null || note.isBlank() ? null : note.strip());
        return ExpertApplicationResponse.from(application);
    }
}
