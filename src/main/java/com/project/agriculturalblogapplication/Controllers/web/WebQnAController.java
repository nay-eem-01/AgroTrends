package com.project.agriculturalblogapplication.Controllers.web;

import com.project.agriculturalblogapplication.DTOS.AnswerDto;
import com.project.agriculturalblogapplication.DTOS.QuestionDto;
import com.project.agriculturalblogapplication.Service.QnAService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/app/qna")
public class WebQnAController {

    private final QnAService qnaService;

    public WebQnAController(QnAService qnaService) {
        this.qnaService = qnaService;
    }

    // Ask Question
    @PostMapping("/ask/user/{userId}")
    public ResponseEntity<QuestionDto> postQuestion(@PathVariable Long userId, @RequestBody QuestionDto questionDto) {
        return new ResponseEntity<>(qnaService.postQuestion(userId, questionDto), HttpStatus.CREATED);
    }

    // Answer to a Question
    @PostMapping("/answer/user/{userId}/question/{questionId}")
    public ResponseEntity<AnswerDto> postAnswer(@PathVariable Long userId, @PathVariable Long questionId, @RequestBody AnswerDto answerDto) {
        return new ResponseEntity<>(qnaService.postAnswer(userId, questionId, answerDto), HttpStatus.CREATED);
    }

    // Reply to an Answer
    @PostMapping("/reply/user/{userId}/question/{questionId}/answer/{parentAnswerId}")
    public ResponseEntity<AnswerDto> replyToAnswer(@PathVariable Long userId, @PathVariable Long questionId, @PathVariable Long parentAnswerId, @RequestBody AnswerDto replyDto) {
        return new ResponseEntity<>(qnaService.replyToAnswer(userId, questionId, parentAnswerId, replyDto), HttpStatus.CREATED);
    }

    // Get all Question
    @GetMapping("/questions")
    public ResponseEntity<List<QuestionDto>> getAllQuestions() {
        return ResponseEntity.ok(qnaService.getAllQuestions());
    }

    // Get question By Id
    @GetMapping("/question/{questionId}")
    public ResponseEntity<QuestionDto> getQuestionById(@PathVariable Long questionId) {
        return ResponseEntity.ok(qnaService.getQuestionById(questionId));
    }

    // Update Question
    @PutMapping("/question/{questionId}/user/{userId}")
    public ResponseEntity<QuestionDto> updateQuestion(@PathVariable Long questionId, @PathVariable Long userId, @RequestBody QuestionDto questionDto) {
        return ResponseEntity.ok(qnaService.updateQuestion(userId, questionId, questionDto));
    }

    // Delete Question
    @DeleteMapping("/question/{questionId}/user/{userId}")
    public ResponseEntity<String> deleteQuestion(@PathVariable Long questionId, @PathVariable Long userId) {
        qnaService.deleteQuestion(userId, questionId);
        return ResponseEntity.ok("Question deleted successfully.");
    }

    //Update Answer
    @PutMapping("/answer/{answerId}/user/{userId}")
    public ResponseEntity<AnswerDto> updateAnswer(@PathVariable Long answerId, @PathVariable Long userId, @RequestBody AnswerDto answerDto) {
        return ResponseEntity.ok(qnaService.updateAnswer(userId, answerId, answerDto));
    }
    // Delete answer
    @DeleteMapping("/answer/{answerId}/user/{userId}")
    public ResponseEntity<String> deleteAnswer(@PathVariable Long answerId, @PathVariable Long userId) {
        qnaService.deleteAnswer(userId, answerId);
        return ResponseEntity.ok("Answer deleted successfully.");
    }

}
