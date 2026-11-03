package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Answer;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.entities.Question;
import com.project.agriculturalblogapplication.entities.QuestionAiAnswer;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.enums.AiAnswerStatus;
import com.project.agriculturalblogapplication.enums.BlogStatus;
import com.project.agriculturalblogapplication.repositories.BlogRepositories;
import com.project.agriculturalblogapplication.repositories.QuestionAiAnswerRepository;
import com.project.agriculturalblogapplication.repositories.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Link previews (frontend request B7): escaping, nothing private leaks, honest "expert" claims, image fallback. */
class SharePageServiceTest {

    private static final String SHARE_URL = "https://api.agrotrends.app/share/questions/5";

    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final QuestionAiAnswerRepository aiAnswers = mock(QuestionAiAnswerRepository.class);
    private final BlogRepositories blogs = mock(BlogRepositories.class);
    private final ExpertService experts = mock(ExpertService.class);
    private final SharePageService share = new SharePageService(questions, aiAnswers, blogs, experts);

    private Question question;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(share, "frontendUrl", "https://agrotrends.app");
        question = new Question();
        question.setId(5L);
        question.setTitle("Leaves \"><script>alert(1)</script> yellow");
        question.setContent("<p>My <b>rice</b> leaves turn yellow.</p><script>steal()</script>");
        Category topic = new Category();
        topic.setId(2L);
        question.setCategory(topic);
        when(questions.findById(5L)).thenReturn(Optional.of(question));
        when(aiAnswers.findByQuestionId(5L)).thenReturn(Optional.empty());
        when(experts.verifiedTopics(any())).thenReturn(Map.of());
    }

    @Test
    void userTextIsEscapedAndHtmlNeverEchoed() {
        String page = share.question(5L, SHARE_URL).orElseThrow();

        assertFalse(page.contains("<script>alert"));
        assertFalse(page.contains("steal()"));
        assertFalse(page.contains("<b>rice"));
        assertTrue(page.contains("&quot;&gt;&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertTrue(page.contains("content=\"My rice leaves turn yellow.\""));
        assertTrue(page.contains("<meta property=\"og:url\" content=\"" + SHARE_URL + "\">"));
        assertTrue(page.contains("<link rel=\"canonical\" href=\"https://agrotrends.app/questions/5\">"));
    }

    @Test
    void aHiddenQuestionOrUnpublishedStoryHasNoPage() {
        question.setHidden(true);
        Blog draft = new Blog();
        draft.setStatus(BlogStatus.DRAFT);
        when(blogs.findBySlug("draft-1")).thenReturn(Optional.of(draft));

        assertTrue(share.question(5L, SHARE_URL).isEmpty());
        assertTrue(share.story("draft-1", SHARE_URL).isEmpty());
        assertFalse(share.notFound(SHARE_URL).contains("yellow"));
    }

    @Test
    void theImageIsTheFirstPhotoElseTheSiteDefault() {
        assertTrue(share.question(5L, SHARE_URL).orElseThrow().contains("content=\"https://agrotrends.app/og-default.png\""));

        question.setImageUrls(List.of("https://images.agrotrends.app/a.jpg", "https://images.agrotrends.app/b.jpg"));
        assertTrue(share.question(5L, SHARE_URL).orElseThrow().contains("content=\"https://images.agrotrends.app/a.jpg\""));
    }

    @Test
    void expertAnsweredOnlyWhenAnExpertReallyDid() {
        Answer accepted = new Answer();
        User author = new User();
        author.setId(9L);
        accepted.setUser(author);
        question.setAcceptedAnswer(accepted);
        assertFalse(share.question(5L, SHARE_URL).orElseThrow().contains("verified expert"));

        when(experts.verifiedTopics(any())).thenReturn(Map.of(9L, Set.of(2L)));
        assertTrue(share.question(5L, SHARE_URL).orElseThrow().contains("Answered by a verified expert · My rice"));

        question.setAcceptedAnswer(null);
        QuestionAiAnswer confirmed = new QuestionAiAnswer();
        confirmed.setStatus(AiAnswerStatus.CONFIRMED);
        when(aiAnswers.findByQuestionId(5L)).thenReturn(Optional.of(confirmed));
        question.setTitle("ধানের পাতা হলুদ");
        String page = share.question(5L, SHARE_URL).orElseThrow();
        assertTrue(page.contains("যাচাইকৃত বিশেষজ্ঞ উত্তর দিয়েছেন · "));
        assertTrue(page.contains("content=\"bn_BD\""));
    }
}
