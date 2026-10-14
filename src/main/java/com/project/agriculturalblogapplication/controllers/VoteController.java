package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.enums.VoteDirection;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.model.response.MyVotesResponse;
import com.project.agriculturalblogapplication.model.response.VoteResponse;
import com.project.agriculturalblogapplication.service.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.project.agriculturalblogapplication.constatnt.AppConstants.DEFAULT_LANGUAGE_CODE;
import static com.project.agriculturalblogapplication.constatnt.AppConstants.LANG;

@Tag(name = "Vote - controller", description = "Up and down votes on questions and answers.")
@RestController
@CommonApiResponses
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @Operation(summary = "Vote on a question (UP or DOWN; replaces your earlier vote; not your own)",
            security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = VoteResponse.class)), responseCode = "200")
    @PutMapping("/api/questions/id/{questionId}/vote")
    public ResponseEntity<HttpResponse> voteOnQuestion(@PathVariable Long questionId,
                                                       @RequestParam(name = "direction") VoteDirection direction,
                                                       @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Voted.", voteService.voteOnQuestion(questionId, direction, lang));
    }

    @Operation(summary = "Withdraw your vote on a question", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = VoteResponse.class)), responseCode = "200")
    @DeleteMapping("/api/questions/id/{questionId}/vote")
    public ResponseEntity<HttpResponse> unvoteQuestion(@PathVariable Long questionId,
                                                       @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Vote removed.", voteService.voteOnQuestion(questionId, null, lang));
    }

    @Operation(summary = "Vote on an answer (UP or DOWN; replaces your earlier vote; not your own)",
            security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = VoteResponse.class)), responseCode = "200")
    @PutMapping("/api/answers/id/{answerId}/vote")
    public ResponseEntity<HttpResponse> voteOnAnswer(@PathVariable Long answerId,
                                                     @RequestParam(name = "direction") VoteDirection direction,
                                                     @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Voted.", voteService.voteOnAnswer(answerId, direction, lang));
    }

    @Operation(summary = "Withdraw your vote on an answer", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = VoteResponse.class)), responseCode = "200")
    @DeleteMapping("/api/answers/id/{answerId}/vote")
    public ResponseEntity<HttpResponse> unvoteAnswer(@PathVariable Long answerId,
                                                     @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Vote removed.", voteService.voteOnAnswer(answerId, null, lang));
    }

    @Operation(summary = "Your votes on a question and its answers", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = MyVotesResponse.class)), responseCode = "200")
    @GetMapping("/api/questions/id/{questionId}/my-votes")
    public ResponseEntity<HttpResponse> myVotes(@PathVariable Long questionId,
                                                @RequestParam(name = LANG, defaultValue = DEFAULT_LANGUAGE_CODE) String lang) {
        return HttpResponse.getResponseEntity(true, "Data loaded successfully.", voteService.myVotes(questionId, lang));
    }
}
