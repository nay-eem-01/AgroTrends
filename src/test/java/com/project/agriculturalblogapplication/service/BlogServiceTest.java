package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.enums.BlogStatus;
import com.project.agriculturalblogapplication.model.request.CreateBlogRequest;
import com.project.agriculturalblogapplication.model.request.UpdateBlogRequest;
import com.project.agriculturalblogapplication.model.response.BlogResponse;
import com.project.agriculturalblogapplication.model.response.RelatedBlogResponse;
import com.project.agriculturalblogapplication.repositories.BlogRepositories;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import com.project.agriculturalblogapplication.enums.AscOrDescType;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    private final BlogService blogService =
            new BlogService(blogRepositories, categoryService, authorService, documentService, authorization);

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
    void someoneElsesDraftIsA404AndTheOwnersDraftIsVisible() {
        Blog draft = blog(9L);
        draft.setStatus(BlogStatus.DRAFT);
        when(blogRepositories.findById(9L)).thenReturn(Optional.of(draft));
        when(authorization.isOwnerOrAdmin(10L, LANG)).thenReturn(false, true);

        ApplicationException e = assertThrows(ApplicationException.class, () -> blogService.getById(9L, LANG));
        assertEquals(HttpStatus.NOT_FOUND, e.getHttpStatus());

        assertEquals(9L, blogService.getById(9L, LANG).id());
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
