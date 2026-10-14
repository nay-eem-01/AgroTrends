package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.ExpertApplication;
import com.project.agriculturalblogapplication.enums.ApplicationStatus;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.ExpertApplicationRequest;
import com.project.agriculturalblogapplication.model.response.ExpertApplicationResponse;
import com.project.agriculturalblogapplication.repositories.ExpertApplicationRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import com.project.agriculturalblogapplication.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/** Expert applications: a user sends proof for some topics; an admin decides (roadmap 7.1, 7.2). */
@Service
@RequiredArgsConstructor
public class ExpertService {

    private final ExpertApplicationRepository applicationRepository;

    private final UserService userService;

    private final CategoryService categoryService;

    private final ImageService imageService;

    private final AuthorizationService authorizationService;

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
}
