package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.entities.ExpertBadge;
import com.project.agriculturalblogapplication.entities.Role;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.AiAnswerStatus;
import com.project.agriculturalblogapplication.enums.ApplicationStatus;
import com.project.agriculturalblogapplication.enums.BlogStatus;
import com.project.agriculturalblogapplication.enums.ReportStatus;
import com.project.agriculturalblogapplication.enums.ReportTarget;
import com.project.agriculturalblogapplication.model.response.AdminStatsResponse;
import com.project.agriculturalblogapplication.model.response.AdminUserDetailResponse;
import com.project.agriculturalblogapplication.model.response.AdminUserDetailResponse.CategoryRef;
import com.project.agriculturalblogapplication.model.response.AdminUserResponse;
import com.project.agriculturalblogapplication.repositories.AnswerRepository;
import com.project.agriculturalblogapplication.repositories.BlogRepositories;
import com.project.agriculturalblogapplication.repositories.ExpertApplicationRepository;
import com.project.agriculturalblogapplication.repositories.ExpertBadgeRepository;
import com.project.agriculturalblogapplication.repositories.QuestionAiAnswerRepository;
import com.project.agriculturalblogapplication.repositories.QuestionRepository;
import com.project.agriculturalblogapplication.repositories.ReportRepository;
import com.project.agriculturalblogapplication.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminServiceTest {

    private static final String LANG = "en";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserService userService = mock(UserService.class);
    private final AuthorService authorService = mock(AuthorService.class);
    private final QuestionRepository questionRepository = mock(QuestionRepository.class);
    private final AnswerRepository answerRepository = mock(AnswerRepository.class);
    private final BlogRepositories blogRepositories = mock(BlogRepositories.class);
    private final ReportRepository reportRepository = mock(ReportRepository.class);
    private final ExpertBadgeRepository badgeRepository = mock(ExpertBadgeRepository.class);
    private final ExpertApplicationRepository applicationRepository = mock(ExpertApplicationRepository.class);
    private final QuestionAiAnswerRepository aiAnswerRepository = mock(QuestionAiAnswerRepository.class);
    private final AdminService admin = new AdminService(userRepository, userService, authorService, questionRepository,
            answerRepository, blogRepositories, reportRepository, badgeRepository, applicationRepository,
            aiAnswerRepository);

    @Test
    void statsCountOnlyWhatIsWaitingAndTheLastSevenDays() {
        when(userRepository.count()).thenReturn(50L);
        when(userRepository.countByCreationDateAfter(any())).thenReturn(6L);
        when(userRepository.countByBannedAtIsNotNull()).thenReturn(2L);
        when(questionRepository.count()).thenReturn(40L);
        when(questionRepository.countByCreationDateAfter(any())).thenReturn(8L);
        when(answerRepository.count()).thenReturn(90L);
        when(blogRepositories.countByStatus(BlogStatus.PUBLISHED)).thenReturn(12L);
        when(reportRepository.countByStatus(ReportStatus.OPEN)).thenReturn(3L);
        when(applicationRepository.countByStatus(ApplicationStatus.PENDING)).thenReturn(4L);
        when(aiAnswerRepository.countByStatusAndQuestionHiddenFalse(AiAnswerStatus.UNCHECKED)).thenReturn(7L);

        assertEquals(new AdminStatsResponse(50, 6, 2, 40, 8, 90, 12, 3, 4, 7), admin.stats());

        ArgumentCaptor<LocalDateTime> since = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userRepository).countByCreationDateAfter(since.capture());
        long days = Duration.between(since.getValue(), LocalDateTime.now()).toDays();
        assertEquals(7, days);
    }

    @Test
    void theSearchTermIsTrimmedAndLowerCasedAndBanStateIsShown() {
        User banned = user(7L);
        banned.setBannedAt(LocalDateTime.now());
        banned.setBanReason("spam");
        when(userRepository.search(eq("rahim@"), eq(true), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(banned)));

        Page<AdminUserResponse> page = admin.users("  Rahim@ ", true, 0, 20);

        AdminUserResponse row = page.getContent().getFirst();
        assertTrue(row.banned());
        assertEquals("spam", row.banReason());
        assertEquals(Set.of("USER"), row.roles());
    }

    @Test
    void noSearchTermMatchesEveryone() {
        when(userRepository.search(eq(""), isNull(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(user(7L))));

        assertFalse(admin.users(null, null, 0, 20).getContent().getFirst().banned());
        verify(userRepository).search(eq(""), isNull(), any(Pageable.class));
    }

    @Test
    void theDetailCountsPostsReportsAboutTheAccountAndActiveExpertTopics() {
        when(userService.findByIdWithException(7L, LANG)).thenReturn(user(7L));
        when(authorService.findAuthorIdByUserId(7L)).thenReturn(Optional.of(70L));
        when(blogRepositories.countByAuthorIdAndStatus(70L, BlogStatus.PUBLISHED)).thenReturn(3L);
        when(questionRepository.countByUserId(7L)).thenReturn(5L);
        when(answerRepository.countByUserId(7L)).thenReturn(9L);
        when(reportRepository.countByTargetTypeAndTargetIdAndStatus(ReportTarget.USER, 7L, ReportStatus.OPEN)).thenReturn(2L);
        Category rice = new Category();
        rice.setId(4L);
        rice.setCategoryName("Rice");
        ExpertBadge badge = new ExpertBadge();
        badge.setTopic(rice);
        when(badgeRepository.findAllByUserIdAndRevokedAtIsNull(7L)).thenReturn(List.of(badge));

        AdminUserDetailResponse detail = admin.user(7L, LANG);

        assertEquals(5L, detail.questions());
        assertEquals(9L, detail.answers());
        assertEquals(3L, detail.publishedStories());
        assertEquals(2L, detail.openReports());
        assertEquals(List.of(new CategoryRef(4L, "Rice")), detail.expertTopics());
    }

    @Test
    void aUserWhoNeverWroteAStoryHasNone() {
        when(userService.findByIdWithException(7L, LANG)).thenReturn(user(7L));
        when(authorService.findAuthorIdByUserId(7L)).thenReturn(Optional.empty());

        assertEquals(0L, admin.user(7L, LANG).publishedStories());
    }

    private static User user(Long id) {
        Role role = new Role();
        role.setRoleName("USER");
        User user = new User();
        user.setId(id);
        user.setName("Rahim");
        user.setEmail("rahim@agrotrends.test");
        user.setRoles(Set.of(role));
        return user;
    }
}
