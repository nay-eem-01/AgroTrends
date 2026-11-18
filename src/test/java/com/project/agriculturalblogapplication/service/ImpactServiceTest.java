package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.model.response.UserImpactResponse;
import com.project.agriculturalblogapplication.repositories.QuestionAiAnswerRepository;
import com.project.agriculturalblogapplication.repositories.QuestionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A member's impact (frontend request B9). */
class ImpactServiceTest {

    private final UserService users = mock(UserService.class);
    private final AnswerService answers = mock(AnswerService.class);
    private final ExpertService experts = mock(ExpertService.class);
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final QuestionAiAnswerRepository aiAnswers = mock(QuestionAiAnswerRepository.class);
    private final ImpactService impact = new ImpactService(users, answers, experts, questions, aiAnswers);

    @Test
    void vettedAnswersAreAcceptedOnesPlusThoseInTheExpertsTopics() {
        when(answers.countPublicByUser(7L)).thenReturn(42L);
        when(answers.countAcceptedByUser(7L)).thenReturn(12L);
        when(questions.sumViewsOfQuestionsAnsweredBy(7L)).thenReturn(3400L);
        when(questions.idsWithAnswerAcceptedFrom(7L)).thenReturn(List.of(1L, 2L));
        when(experts.verifiedTopics(Set.of(7L))).thenReturn(Map.of(7L, Set.of(5L)));
        when(questions.idsAnsweredInTopics(7L, Set.of(5L))).thenReturn(List.of(2L, 3L));
        when(aiAnswers.countCitingQuestions(Set.of(1L, 2L, 3L))).thenReturn(7L);
        when(aiAnswers.countStoryCitations(7L)).thenReturn(19L);

        assertEquals(new UserImpactResponse(42, 12, 3400, 7, 19), impact.of(7L, "en"));
    }

    @Test
    void withNothingVettedNoCitationQueryRunsAndTheResultIsKeptForAnHour() {
        when(questions.idsWithAnswerAcceptedFrom(8L)).thenReturn(List.of());
        when(experts.verifiedTopics(Set.of(8L))).thenReturn(Map.of());

        impact.of(8L, "en");
        impact.of(8L, "en");

        verify(aiAnswers, never()).countCitingQuestions(any());
        verify(questions, times(1)).sumViewsOfQuestionsAnsweredBy(8L);
    }
}
