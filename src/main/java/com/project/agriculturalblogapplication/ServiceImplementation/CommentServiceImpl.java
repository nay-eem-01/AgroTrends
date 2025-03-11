package com.project.agriculturalblogapplication.ServiceImplementation;

import com.project.agriculturalblogapplication.DTOS.CommentDto;
import com.project.agriculturalblogapplication.ExceptionHandler.ResourceNotFoundException;
import com.project.agriculturalblogapplication.Models.Blogs;
import com.project.agriculturalblogapplication.Models.Comments;
import com.project.agriculturalblogapplication.Models.Users;
import com.project.agriculturalblogapplication.Repositories.BlogRepositories;
import com.project.agriculturalblogapplication.Repositories.CommentRepository;
import com.project.agriculturalblogapplication.Repositories.UserRepositories;
import com.project.agriculturalblogapplication.Service.CommentService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

    private final BlogRepositories blogRepository;
    private final UserRepositories userRepositories;
    private final CommentRepository commentRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public CommentServiceImpl(BlogRepositories blogRepository, UserRepositories userRepositories, CommentRepository commentRepository, ModelMapper modelMapper) {
        this.blogRepository = blogRepository;
        this.userRepositories = userRepositories;
        this.commentRepository = commentRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public CommentDto addNewComment(CommentDto commentDto,Long userId,Long blogId) {

        Blogs blog = blogRepository.findById(blogId).orElseThrow(()-> new ResourceNotFoundException("Blog","blog ID",blogId));
        Users user = userRepositories.findById(userId).orElseThrow(()-> new ResourceNotFoundException("User","user ID",userId));

        Comments newComment = modelMapper.map(commentDto, Comments.class);
        newComment.setUser(user);
        newComment.setBlog(blog);
        newComment.setCreatedAt(LocalDateTime.now());
        newComment.setUpdatedAt(LocalDateTime.now());

       Comments savedComment = commentRepository.save(newComment);
       CommentDto savedCommentDto = modelMapper.map(savedComment, CommentDto.class);

       savedCommentDto.setBlogId(blog.getBlogId());
       savedCommentDto.setUserId(user.getUserId());

        return savedCommentDto;
    }

    @Override
    public CommentDto replyToAComment(CommentDto commentDto) {
        return null;
    }

    @Override
    public CommentDto updateComment(CommentDto commentDto) {
        return null;
    }

    @Override
    public CommentDto deleteComment(CommentDto commentDto) {
        return null;
    }

    @Override
    public List<CommentDto> viewAllComments() {
        return null;
    }

    @Override
    public List<CommentDto> viewReplies() {
        return null;
    }
}
