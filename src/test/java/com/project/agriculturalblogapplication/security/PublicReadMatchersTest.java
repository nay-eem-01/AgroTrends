package com.project.agriculturalblogapplication.security;

import com.project.agriculturalblogapplication.constatnt.SecurityConstants;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reading is public, posting is not: pins which GET paths anonymous visitors may reach. */
class PublicReadMatchersTest {

    private static boolean isPublicGet(String path) {
        return Arrays.stream(SecurityConstants.PUBLIC_GET_MATCHERS)
                .map(PathPatternParser.defaultInstance::parse)
                .anyMatch(pattern -> pattern.matches(PathContainer.parsePath(path)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/blogs/all", "/api/blogs/all/category/3", "/api/blogs/all/tag/rice", "/api/blogs/all/author/20",
            "/api/blogs/search", "/api/blogs/id/9", "/api/blogs/slug/boro-rice-abc123",
            "/api/feed/latest", "/api/feed/trending", "/api/categories/all", "/api/tags", "/api/authors/20",
            "/api/comments/blog/9", "/api/comments/replies/4", "/api/comments/id/4",
            "/api/questions/all", "/api/questions/all/user/7", "/api/questions/id/5",
            "/api/answers/question/5", "/api/answers/replies/6", "/api/answers/id/6",
    })
    void publishedContentIsReadableWithoutSigningIn(String path) {
        assertTrue(isPublicGet(path), path);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/blogs/me/drafts", "/api/blogs/id/9/related", "/api/blogs/id/9/claps", "/api/feed/following",
            "/api/bookmarks", "/api/me/following/authors", "/api/me/following/tags", "/api/ai/history",
            "/api/user/me", "/api/user/paginated", "/api/user/id/7", "/api/questions/id/5/ai-draft",
    })
    void privateOrPaidReadsStillNeedAToken(String path) {
        assertFalse(isPublicGet(path), path);
    }
}
