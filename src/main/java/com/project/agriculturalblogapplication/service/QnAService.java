package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.dtos.AnswerDto;
import com.project.agriculturalblogapplication.dtos.QuestionDto;
import com.project.agriculturalblogapplication.exceptionHandler.ResourceNotFoundException;
import com.project.agriculturalblogapplication.exceptionHandler.UnauthorizedActionException;
import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.repositories.AnswerRepository;
import com.project.agriculturalblogapplication.repositories.QuestionRepository;
import com.project.agriculturalblogapplication.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QnAService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public QnAService(QuestionRepository questionRepository,
                      AnswerRepository answerRepository,
                      UserRepository userRepository,
                      ModelMapper modelMapper) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
    }

    public QuestionDto postQuestion(Long userId, QuestionDto questionDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "ID", userId));

        Question question = modelMapper.map(questionDto, Question.class);
        question.setUser(user);

        return convertQuestionToDtoWithAnswers(questionRepository.save(question));
    }


    public AnswerDto postAnswer(Long userId, Long questionId, AnswerDto answerDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "ID", userId));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "ID", questionId));

        Answer answer = modelMapper.map(answerDto, Answer.class);
        answer.setUser(user);
        answer.setQuestion(question);

        return convertToDtoWithReplies(answerRepository.save(answer));
    }


    public AnswerDto replyToAnswer(Long userId, Long questionId, Long parentAnswerId, AnswerDto replyDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "ID", userId));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "ID", questionId));

        Answer parentAnswer = answerRepository.findById(parentAnswerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer", "ID", parentAnswerId));

        Answer reply = modelMapper.map(replyDto, Answer.class);
        reply.setUser(user);
        reply.setQuestion(question);
        reply.setParentAnswer(parentAnswer);

        return convertToDtoWithReplies(answerRepository.save(reply));
    }


    public List<QuestionDto> getAllQuestions() {
        return questionRepository.findAll()
                .stream()
                .map(this::convertQuestionToDtoWithAnswers)
                .toList();
    }


    public QuestionDto getQuestionById(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "ID", questionId));

        return convertQuestionToDtoWithAnswers(question);
    }


    public QuestionDto updateQuestion(Long userId, Long questionId, QuestionDto questionDto) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "ID", questionId));

        if (!question.getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("You are not allowed to update this question.");
        }

        question.setTitle(questionDto.getTitle());
        question.setContent(questionDto.getContent());

        return convertQuestionToDtoWithAnswers(questionRepository.save(question));
    }


    public void deleteQuestion(Long userId, Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "ID", questionId));

        if (!question.getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("You are not allowed to delete this question.");
        }

        questionRepository.delete(question);
    }


    public AnswerDto updateAnswer(Long userId, Long answerId, AnswerDto answerDto) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer", "ID", answerId));

        if (!answer.getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("You are not allowed to update this answer.");
        }

        answer.setContent(answerDto.getContent());

        return convertToDtoWithReplies(answerRepository.save(answer));
    }


    public void deleteAnswer(Long userId, Long answerId) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer", "ID", answerId));

        if (!answer.getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("You are not allowed to delete this answer.");
        }

        answerRepository.delete(answer);
    }

    private AnswerDto convertToDtoWithReplies(Answer answer) {
        AnswerDto dto = modelMapper.map(answer, AnswerDto.class);
        dto.setUserId(answer.getUser().getId());
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


    private QuestionDto convertQuestionToDtoWithAnswers(Question question) {
        QuestionDto dto = modelMapper.map(question, QuestionDto.class);
        dto.setUserId(question.getUser().getId());

        List<AnswerDto> topLevelAnswers = answerRepository.findByQuestionIdAndParentAnswerIsNull(question.getId())
                .stream()
                .map(this::convertToDtoWithReplies)
                .collect(Collectors.toList());

        dto.setAnswers(topLevelAnswers);

        return dto;
    }
}
