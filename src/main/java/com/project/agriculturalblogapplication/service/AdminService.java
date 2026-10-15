package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.BlogStatus;
import com.project.agriculturalblogapplication.enums.ReportStatus;
import com.project.agriculturalblogapplication.enums.ReportTarget;
import com.project.agriculturalblogapplication.model.response.AdminUserDetailResponse;
import com.project.agriculturalblogapplication.model.response.AdminUserDetailResponse.CategoryRef;
import com.project.agriculturalblogapplication.model.response.AdminUserResponse;
import com.project.agriculturalblogapplication.repositories.AnswerRepository;
import com.project.agriculturalblogapplication.repositories.BlogRepositories;
import com.project.agriculturalblogapplication.repositories.ExpertBadgeRepository;
import com.project.agriculturalblogapplication.repositories.QuestionRepository;
import com.project.agriculturalblogapplication.repositories.ReportRepository;
import com.project.agriculturalblogapplication.repositories.UserRepository;
import com.project.agriculturalblogapplication.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Read side of the admin dashboard (roadmap 9.x). Acting on users stays in {@link ModerationService}. */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    private final UserService userService;

    private final AuthorService authorService;

    private final QuestionRepository questionRepository;

    private final AnswerRepository answerRepository;

    private final BlogRepositories blogRepositories;

    private final ReportRepository reportRepository;

    private final ExpertBadgeRepository expertBadgeRepository;

    /** Users whose name or e-mail contains {@code query}, optionally only (un)banned ones, newest first. */
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> users(String query, Boolean banned, int pageNo, int pageSize) {
        String needle = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        return userRepository.search(needle, banned,
                        CommonUtils.clampedPageable(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "creationDate")))
                .map(AdminUserResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse user(Long userId, String lang) {
        User user = userService.findByIdWithException(userId, lang);
        long stories = authorService.findAuthorIdByUserId(userId)
                .map(authorId -> blogRepositories.countByAuthorIdAndStatus(authorId, BlogStatus.PUBLISHED))
                .orElse(0L);
        return new AdminUserDetailResponse(AdminUserResponse.from(user),
                questionRepository.countByUserId(userId),
                answerRepository.countByUserId(userId),
                stories,
                reportRepository.countByTargetTypeAndTargetIdAndStatus(ReportTarget.USER, userId, ReportStatus.OPEN),
                expertBadgeRepository.findAllByUserIdAndRevokedAtIsNull(userId).stream()
                        .map(badge -> new CategoryRef(badge.getTopic().getId(), badge.getTopic().getCategoryName()))
                        .toList());
    }
}
