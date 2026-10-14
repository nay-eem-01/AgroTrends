package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.Report;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.CreateReportRequest;
import com.project.agriculturalblogapplication.model.response.ReportResponse;
import com.project.agriculturalblogapplication.repositories.ReportRepository;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Users report content or other users for moderators to review (roadmap 8.3). */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;

    private final BlogService blogService;

    private final QuestionService questionService;

    private final AnswerService answerService;

    private final AiService aiService;

    private final CommentService commentService;

    private final UserService userService;

    private final AuthorizationService authorizationService;

    /** One report per user and target; the target must exist (404 otherwise). */
    @Transactional
    public ReportResponse report(CreateReportRequest request, String lang) {
        Long reporterId = authorizationService.currentUserId(lang);
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId, request.getTargetType(), request.getTargetId())) {
            throw new ApplicationException(HttpStatus.CONFLICT, ErrorCode.ERROR_ALREADY_REPORTED, lang);
        }
        assertExists(request, reporterId, lang);

        Report report = new Report();
        report.setReporter(userService.findByIdWithException(reporterId, lang));
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setReason(request.getReason());
        report.setDetails(request.getDetails() == null || request.getDetails().isBlank() ? null : request.getDetails().strip());
        return ReportResponse.from(reportRepository.save(report));
    }

    private void assertExists(CreateReportRequest request, Long reporterId, String lang) {
        Long id = request.getTargetId();
        switch (request.getTargetType()) {
            case BLOG -> blogService.findByIdWithException(id);
            case QUESTION -> questionService.findByIdWithException(id);
            case ANSWER -> answerService.findByIdWithException(id);
            case AI_ANSWER -> aiService.questionAnswer(id, lang);
            case COMMENT -> commentService.findByIdWithException(id);
            case USER -> {
                if (id.equals(reporterId)) {
                    throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_CANNOT_REPORT_YOURSELF, lang);
                }
                userService.findByIdWithException(id, lang);
            }
        }
    }
}
