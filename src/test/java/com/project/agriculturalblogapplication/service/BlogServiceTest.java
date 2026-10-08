package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.entities.Tag;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.enums.BlogStatus;
import com.project.agriculturalblogapplication.enums.CropSeason;
import com.project.agriculturalblogapplication.model.AgriInfo;
import com.project.agriculturalblogapplication.model.request.CreateBlogRequest;
import com.project.agriculturalblogapplication.model.request.UpdateBlogRequest;
import com.project.agriculturalblogapplication.model.response.BlogResponse;
import com.project.agriculturalblogapplication.model.response.RelatedBlogResponse;
import com.project.agriculturalblogapplication.repositories.BlogRepositories;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import com.project.agriculturalblogapplication.enums.AscOrDescType;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlogServiceTest {

    private static final String LANG = "en";

    private final BlogRepositories blogRepositories = mock(BlogRepositories.class);
    private final CategoryService categoryService = mock(CategoryService.class);
    private final AuthorService authorService = mock(AuthorService.class);
    private final DocumentService documentService = mock(DocumentService.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final TagService tagService = mock(TagService.class);
    private final BlogService blogService =
            new BlogService(blogRepositories, categoryService, authorService, documentService, authorization, tagService);

    @Test
    void updateReplacesTheBlogsVectorsInsteadOfAddingMore() {
        Blog blog = blog(5L);
        when(blogRepositories.findById(5L)).thenReturn(Optional.of(blog));
        when(categoryService.findByIdWithException(30L)).thenReturn(blog.getCategory());
        when(blogRepositories.save(blog)).thenReturn(blog);

        blogService.update(updateRequest(5L), LANG);

        verify(documentService).reindexBlog(blog);
        verify(documentService, never()).indexBlog(any());
    }

    @Test
    void deleteRemovesTheBlogsVectorsAfterTheBlog() {
        Blog blog = blog(6L);
        when(blogRepositories.findById(6L)).thenReturn(Optional.of(blog));

        blogService.delete(6L, LANG);

        InOrder inOrder = inOrder(blogRepositories, documentService);
        inOrder.verify(blogRepositories).delete(blog);
        inOrder.verify(documentService).deleteBlog(6L);
    }

    @Test
    void deleteByAStrangerKeepsTheVectors() {
        when(blogRepositories.findById(7L)).thenReturn(Optional.of(blog(7L)));
        doThrow(new ApplicationException(HttpStatus.FORBIDDEN, "forbidden"))
                .when(authorization).assertOwnerOrAdmin(anyLong(), any());

        assertThrows(ApplicationException.class, () -> blogService.delete(7L, LANG));

        verify(documentService, never()).deleteBlog(anyLong());
    }

    @Test
    void reindexAllWalksEveryPage() {
        List<Blog> first = LongStream.rangeClosed(1, 50).mapToObj(BlogServiceTest::blog).toList();
        List<Blog> second = List.of(blog(51L), blog(52L));
        when(blogRepositories.findAll(any(Pageable.class))).thenAnswer(call -> {
            PageRequest request = call.getArgument(0);
            List<Blog> content = request.getPageNumber() == 0 ? first : second;
            return new PageImpl<>(content, request, 52);
        });

        assertEquals(52, blogService.reindexAll());

        verify(documentService, times(52)).reindexBlog(any());
        verify(blogRepositories).findAll(PageRequest.of(1, 50, Sort.by("id")));
    }

    @Test
    void relatedKeepsSimilarityOrderSkipsMissingBlogsAndClampsTheLimit() {
        Blog blog = blog(1L);
        when(blogRepositories.findById(1L)).thenReturn(Optional.of(blog));
        when(documentService.findRelatedBlogIds(blog, 10)).thenReturn(List.of(8L, 3L, 99L));
        when(blogRepositories.findAllById(List.of(8L, 3L, 99L))).thenReturn(List.of(blog(3L), blog(8L)));

        List<RelatedBlogResponse> related = blogService.related(1L, 50, LANG);

        assertEquals(List.of(new RelatedBlogResponse(8L, "Title 8"), new RelatedBlogResponse(3L, "Title 3")), related);
    }

    @Test
    void blogsByAuthorUseTheAuthorIdAndDatabasePaging() {
        Blog blog = blog(1L);
        when(authorService.findByIdWithException(20L)).thenReturn(blog.getAuthor());
        when(blogRepositories.findAllByAuthorAndStatus(eq(blog.getAuthor()), eq(BlogStatus.PUBLISHED), any(Pageable.class)))
                .thenAnswer(call -> new PageImpl<>(List.of(blog), call.getArgument(2), 41));

        var page = blogService.getAllByAuthor(new PaginationArgs(2, 20, "creationDate", AscOrDescType.desc), 20L, LANG);

        assertEquals(41, page.getTotalElements());
        assertEquals(2, page.getNumber());
        verify(authorService, never()).findByUserIdWithException(anyLong());
    }

    @Test
    void aDraftIsSavedWithoutVectorsOrPublishDate() {
        when(categoryService.findByIdWithException(30L)).thenReturn(blog(1L).getCategory());
        when(authorService.findByUserIdOrForbidden(any(), any())).thenReturn(blog(1L).getAuthor());
        when(blogRepositories.save(any(Blog.class))).thenAnswer(call -> call.getArgument(0));
        CreateBlogRequest request = new CreateBlogRequest();
        request.setCategoryId(30L);
        request.setTitle("Draft");
        request.setContent("Not ready");
        request.setStatus(BlogStatus.DRAFT);

        BlogResponse created = blogService.create(request, LANG);

        assertEquals(BlogStatus.DRAFT, created.status());
        assertNull(created.publishedAt());
        verify(documentService, never()).indexBlog(any());
    }

    @Test
    void publishingADraftIndexesItAndStampsThePublishDate() {
        Blog draft = blog(9L);
        draft.setStatus(BlogStatus.DRAFT);
        when(blogRepositories.findById(9L)).thenReturn(Optional.of(draft));
        when(blogRepositories.save(draft)).thenReturn(draft);

        BlogResponse published = blogService.publish(9L, LANG);

        assertEquals(BlogStatus.PUBLISHED, published.status());
        assertNotNull(published.publishedAt());
        verify(documentService).indexBlog(draft);
    }

    @Test
    void publishingTwiceIsA409() {
        when(blogRepositories.findById(9L)).thenReturn(Optional.of(blog(9L)));

        ApplicationException e = assertThrows(ApplicationException.class, () -> blogService.publish(9L, LANG));

        assertEquals(HttpStatus.CONFLICT, e.getHttpStatus());
    }

    @Test
    void unpublishingRemovesTheVectorsSoAiStopsCitingIt() {
        Blog blog = blog(9L);
        when(blogRepositories.findById(9L)).thenReturn(Optional.of(blog));
        when(blogRepositories.save(blog)).thenReturn(blog);

        assertEquals(BlogStatus.DRAFT, blogService.unpublish(9L, LANG).status());

        verify(documentService).deleteBlog(9L);
    }

    @Test
    void someoneElsesOrAnAnonymousReadersDraftIsA404AndTheOwnersDraftIsVisible() {
        Blog draft = blog(9L);
        draft.setStatus(BlogStatus.DRAFT);
        when(blogRepositories.findById(9L)).thenReturn(Optional.of(draft));
        // false for a stranger and for an anonymous reader alike: never a 401 that would confirm the draft exists
        when(authorization.isSignedInOwnerOrAdmin(10L)).thenReturn(false, true);

        ApplicationException e = assertThrows(ApplicationException.class, () -> blogService.getById(9L, LANG));
        assertEquals(HttpStatus.NOT_FOUND, e.getHttpStatus());

        assertEquals(9L, blogService.getById(9L, LANG).id());
    }

    @Test
    void createRetriesWhenTheRandomSlugIsTaken() {
        when(categoryService.findByIdWithException(30L)).thenReturn(blog(1L).getCategory());
        when(authorService.findByUserIdOrForbidden(any(), any())).thenReturn(blog(1L).getAuthor());
        when(blogRepositories.save(any(Blog.class))).thenAnswer(call -> call.getArgument(0));
        when(blogRepositories.existsBySlug(anyString())).thenReturn(true, false);
        CreateBlogRequest request = new CreateBlogRequest();
        request.setCategoryId(30L);
        request.setTitle("Rice Blast");
        request.setContent("Spray early.");

        BlogResponse created = blogService.create(request, LANG);

        assertTrue(created.slug().matches("rice-blast-[a-z0-9]{6}"), created.slug());
        verify(blogRepositories, times(2)).existsBySlug(anyString());
    }

    @Test
    void aSlugLeadingToSomeoneElsesDraftIsA404() {
        Blog draft = blog(9L);
        draft.setStatus(BlogStatus.DRAFT);
        when(blogRepositories.findBySlug("draft-abc123")).thenReturn(Optional.of(draft));
        when(blogRepositories.findById(9L)).thenReturn(Optional.of(draft));

        ApplicationException e = assertThrows(ApplicationException.class, () -> blogService.getBySlug("draft-abc123", LANG));

        assertEquals(HttpStatus.NOT_FOUND, e.getHttpStatus());
    }

    @Test
    void updateWithoutTagsKeepsThemAndWithTagsReplacesThem() {
        Blog blog = blog(5L);
        Tag rice = new Tag();
        rice.setName("rice");
        blog.setTags(new LinkedHashSet<>(Set.of(rice)));
        when(blogRepositories.findById(5L)).thenReturn(Optional.of(blog));
        when(categoryService.findByIdWithException(30L)).thenReturn(blog.getCategory());
        when(blogRepositories.save(blog)).thenReturn(blog);

        assertEquals(List.of("rice"), blogService.update(updateRequest(5L), LANG).tags());

        Tag wheat = new Tag();
        wheat.setName("wheat");
        Tag barley = new Tag();
        barley.setName("barley");
        when(tagService.resolve(List.of("Wheat", "barley"))).thenReturn(new LinkedHashSet<>(List.of(wheat, barley)));
        UpdateBlogRequest withTags = updateRequest(5L);
        withTags.setTags(List.of("Wheat", "barley"));

        assertEquals(List.of("barley", "wheat"), blogService.update(withTags, LANG).tags());
    }

    @Test
    void searchNeedsSomethingToSearchForAndTrimsIt() {
        ApplicationException e = assertThrows(ApplicationException.class, () -> blogService.search("   ", 0, 20, LANG));
        assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus());

        when(blogRepositories.search(eq("rice blast"), any(Pageable.class))).thenReturn(Page.empty());
        blogService.search("  rice blast ", 0, 500, LANG);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(blogRepositories).search(eq("rice blast"), pageable.capture());
        assertEquals(100, pageable.getValue().getPageSize());
    }

    @Test
    void feedsSortNewestOrMostClappedAndFollowingIsTheCallers() {
        when(authorization.currentUserId(LANG)).thenReturn(7L);
        when(blogRepositories.findFollowingFeed(eq(7L), any(Pageable.class))).thenReturn(Page.empty());
        when(blogRepositories.findAllByStatusAndPublishedAtAfter(eq(BlogStatus.PUBLISHED), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        blogService.followingFeed(0, 20, LANG);
        blogService.trendingFeed(0, 20);

        ArgumentCaptor<Pageable> following = ArgumentCaptor.forClass(Pageable.class);
        verify(blogRepositories).findFollowingFeed(eq(7L), following.capture());
        assertEquals(Sort.by(Sort.Direction.DESC, "publishedAt").and(Sort.by(Sort.Direction.DESC, "id")), following.getValue().getSort());

        ArgumentCaptor<LocalDateTime> since = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<Pageable> trending = ArgumentCaptor.forClass(Pageable.class);
        verify(blogRepositories).findAllByStatusAndPublishedAtAfter(eq(BlogStatus.PUBLISHED), since.capture(), trending.capture());
        assertEquals("clapCount", trending.getValue().getSort().iterator().next().getProperty());
        long days = Duration.between(since.getValue(), LocalDateTime.now()).toDays();
        assertEquals(BlogService.TRENDING_DAYS, days);
    }

    @Test
    void agriInfoIsKeptWhenAnUpdateLeavesItOut() {
        Blog blog = blog(5L);
        blog.setAgri(new AgriInfo("rice", CropSeason.RABI, "rangpur", null).toMetadata());
        when(blogRepositories.findById(5L)).thenReturn(Optional.of(blog));
        when(categoryService.findByIdWithException(30L)).thenReturn(blog.getCategory());
        when(blogRepositories.save(blog)).thenReturn(blog);

        assertEquals("rangpur", blogService.update(updateRequest(5L), LANG).agri().region());

        UpdateBlogRequest change = updateRequest(5L);
        change.setAgri(new AgriInfo("Jute", CropSeason.KHARIF_1, null, null));
        assertEquals(new AgriInfo("jute", CropSeason.KHARIF_1, null, null), blogService.update(change, LANG).agri());
    }

    private static UpdateBlogRequest updateRequest(Long blogId) {
        UpdateBlogRequest request = new UpdateBlogRequest();
        request.setBlogId(blogId);
        request.setCategoryId(30L);
        request.setTitle("New title");
        request.setContent("New content");
        return request;
    }

    private static Blog blog(Long id) {
        User user = new User();
        user.setId(10L);
        Author author = new Author();
        author.setId(20L);
        author.setUser(user);
        Category category = new Category();
        category.setId(30L);

        Blog blog = new Blog();
        blog.setId(id);
        blog.setTitle("Title " + id);
        blog.setContent("Content " + id);
        blog.setAuthor(author);
        blog.setCategory(category);
        return blog;
    }
}
