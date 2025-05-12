package com.project.agriculturalblogapplication.ServiceImplementation;

import com.project.agriculturalblogapplication.DTOS.AnswerDto;
import com.project.agriculturalblogapplication.DTOS.QuestionDto;
import com.project.agriculturalblogapplication.ExceptionHandler.ResourceNotFoundException;
import com.project.agriculturalblogapplication.ExceptionHandler.UnauthorizedActionException;
import com.project.agriculturalblogapplication.Models.Answer;
import com.project.agriculturalblogapplication.Models.Question;
import com.project.agriculturalblogapplication.Models.Users;
import com.project.agriculturalblogapplication.Repositories.AnswerRepository;
import com.project.agriculturalblogapplication.Repositories.QuestionRepository;
import com.project.agriculturalblogapplication.Repositories.UserRepositories;
import com.project.agriculturalblogapplication.Service.QnAService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QnAServiceImpl implements QnAService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final UserRepositories userRepository;
    private final ModelMapper modelMapper;

    public QnAServiceImpl(QuestionRepository questionRepository,
                          AnswerRepository answerRepository,
                          UserRepositories userRepository,
                          ModelMapper modelMapper) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public QuestionDto postQuestion(Long userId, QuestionDto questionDto) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "ID", userId));

        Question question = modelMapper.map(questionDto, Question.class);
        question.setUser(user);
        question.setCreatedAt(LocalDateTime.now());

        return convertQuestionToDtoWithAnswers(questionRepository.save(question));
    }

    @Override
    public AnswerDto postAnswer(Long userId, Long questionId, AnswerDto answerDto) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "ID", userId));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "ID", questionId));

        Answer answer = modelMapper.map(answerDto, Answer.class);
        answer.setUser(user);
        answer.setQuestion(question);
        answer.setCreatedAt(LocalDateTime.now());
        answer.setUpdatedAt(LocalDateTime.now());

        return convertToDtoWithReplies(answerRepository.save(answer));
    }

    @Override
    public AnswerDto replyToAnswer(Long userId, Long questionId, Long parentAnswerId, AnswerDto replyDto) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "ID", userId));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "ID", questionId));

        Answer parentAnswer = answerRepository.findById(parentAnswerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer", "ID", parentAnswerId));

        Answer reply = modelMapper.map(replyDto, Answer.class);
        reply.setUser(user);
        reply.setQuestion(question);
        reply.setParentAnswer(parentAnswer);
        reply.setCreatedAt(LocalDateTime.now());
        reply.setUpdatedAt(LocalDateTime.now());

        return convertToDtoWithReplies(answerRepository.save(reply));
    }

    @Override
    public List<QuestionDto> getAllQuestions() {
        return questionRepository.findAll()
                .stream()
                .map(this::convertQuestionToDtoWithAnswers)
                .toList();
    }

    @Override
    public QuestionDto getQuestionById(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "ID", questionId));

        return convertQuestionToDtoWithAnswers(question);
    }

    @Override
    public QuestionDto updateQuestion(Long userId, Long questionId, QuestionDto questionDto) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "ID", questionId));

        if (!question.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedActionException("You are not allowed to update this question.");
        }

        question.setTitle(questionDto.getTitle());
        question.setContent(questionDto.getContent());

        return convertQuestionToDtoWithAnswers(questionRepository.save(question));
    }

    @Override
    public void deleteQuestion(Long userId, Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "ID", questionId));

        if (!question.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedActionException("You are not allowed to delete this question.");
        }

        questionRepository.delete(question);
    }

    @Override
    public AnswerDto updateAnswer(Long userId, Long answerId, AnswerDto answerDto) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer", "ID", answerId));

        if (!answer.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedActionException("You are not allowed to update this answer.");
        }

        answer.setContent(answerDto.getContent());
        answer.setUpdatedAt(LocalDateTime.now());

        return convertToDtoWithReplies(answerRepository.save(answer));
    }

    @Override
    public void deleteAnswer(Long userId, Long answerId) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer", "ID", answerId));

        if (!answer.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedActionException("You are not allowed to delete this answer.");
        }

        answerRepository.delete(answer);
    }

    // Utility method to map Answer and nested replies
    private AnswerDto convertToDtoWithReplies(Answer answer) {
        AnswerDto dto = modelMapper.map(answer, AnswerDto.class);
        dto.setUserId(answer.getUser().getUserId());
        dto.setQuestionId(answer.getQuestion().getId());

        if (answer.getParentAnswer() != null) {
            dto.setParentAnswerId(answer.getParentAnswer().getId());
        }
        List<Answer> replies = answer.getReplies();
        if (replies == null) {
            replies = new ArrayList<>();
        }


        dto.setReplies(
                replies.stream()
                        .map(this::convertToDtoWithReplies)
                        .collect(Collectors.toList())
        );

        return dto;
    }

    // Utility method to map Question and its top-level answers
    private QuestionDto convertQuestionToDtoWithAnswers(Question question) {
        QuestionDto dto = modelMapper.map(question, QuestionDto.class);
        dto.setUserId(question.getUser().getUserId());

        List<AnswerDto> topLevelAnswers = answerRepository.findByQuestionIdAndParentAnswerIsNull(question.getId())
                .stream()
                .map(this::convertToDtoWithReplies)
                .collect(Collectors.toList());

        dto.setAnswers(topLevelAnswers);

        return dto;
    }
}
