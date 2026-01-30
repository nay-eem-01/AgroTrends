package com.project.agriculturalblogapplication.controllers;

import com.project.agriculturalblogapplication.config.CommonApiResponses;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.model.request.AskQuestionRequest;
import com.project.agriculturalblogapplication.model.request.CreateAiResponseRequest;
import com.project.agriculturalblogapplication.model.request.CreateAnswerRequest;
import com.project.agriculturalblogapplication.model.response.HttpResponse;
import com.project.agriculturalblogapplication.service.AiService;
import com.project.agriculturalblogapplication.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.project.agriculturalblogapplication.constatnt.AppConstants.DEFAULT_LANGUAGE_CODE;

@Tag(name = "AI Chat - controller", description = "AI chat related operations.")
@RestController
@RequestMapping("/api/ai")
@CommonApiResponses
@RequiredArgsConstructor
public class AiChatController {

    private final ChatClient chatClient;

    private final AiService aiService;

    private final UserService userService;

    @Operation(summary = "Ask anything", security = @SecurityRequirement(name = "jwtToken"))
    @ApiResponse(content = @Content(schema = @Schema(implementation = HttpResponse.class)), responseCode = "200")
    @PostMapping(value = "/ask")
    public ResponseEntity<HttpResponse> ask(@RequestBody AskQuestionRequest request,
                                            @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE_CODE) String lang
    ) {
        String answer = chatClient.prompt()
                .user(request.question())
                .call()
                .content();

        User user = userService.getUserInfo(lang);

        CreateAiResponseRequest aiResponseRequest = new CreateAiResponseRequest();
        aiResponseRequest.setQuestion(request.question());
        aiResponseRequest.setAnswer(answer);
        aiResponseRequest.setUserId(user.getId());

        aiService.save(aiResponseRequest);

        return HttpResponse.getResponseEntity(
                true,
                "Answer created successfully.",
                Map.of("answer", answer));
    }
}
