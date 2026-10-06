package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Author;
import com.project.agriculturalblogapplication.entities.Blog;
import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.UpdateBlogRequest;
import com.project.agriculturalblogapplication.repositories.BlogRepositories;
import com.project.agriculturalblogapplication.security.service.AuthorizationService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
