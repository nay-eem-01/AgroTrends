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


        return modelMapper.map(savedComment, CommentDto.class);
    }


    @Override
    public CommentDto updateComment(CommentDto commentDto,Long commentId) {
        Comments comment = commentRepository.findById(commentId).get();

        comment.setCommentContent(commentDto.getCommentContent());
        commentRepository.save(comment);

        return modelMapper.map(comment, CommentDto.class);
    }

    @Override
    public String deleteComment(Long commentId) {
        Comments comment = commentRepository.findById(commentId).orElseThrow(null);
        commentRepository.delete(comment);

        return "Comment Deleted";
    }

    @Override
    public List<CommentDto> viewAllCommentsByBlogId(Long blogId) {
        List<Comments> topLevelComments = commentRepository.findByBlogBlogIdAndParentCommentIsNull(blogId);
        return topLevelComments.stream()
                .map(this::convertToDtoWithReplies)
                .toList();
    }



    @Override
    public CommentDto replyToAComment(CommentDto commentDto, Long userId, Long blogId, Long parentCommentId) {
        Blogs blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new ResourceNotFoundException("Blog", "Blog ID", blogId));

        Users user = userRepositories.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "User ID", userId));

        Comments parentComment = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "Comment ID", parentCommentId));

        Comments reply = modelMapper.map(commentDto, Comments.class);
        reply.setUser(user);
        reply.setBlog(blog);
        reply.setParentComment(parentComment);
        reply.setCreatedAt(LocalDateTime.now());
        reply.setUpdatedAt(LocalDateTime.now());

        Comments savedReply = commentRepository.save(reply);
        return modelMapper.map(savedReply, CommentDto.class);
    }


    @Override
    public List<CommentDto> viewReplies(Long parentCommentId) {
        Comments parentComment = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "Comment ID", parentCommentId));

        List<Comments> replies = parentComment.getReplies();

        return replies.stream()
                .map(this::convertToDtoWithReplies)
                .toList();
    }



    private CommentDto convertToDtoWithReplies(Comments comment) {
        CommentDto dto = modelMapper.map(comment, CommentDto.class);

        // Manually map the IDs
        dto.setUserId(comment.getUser() != null ? comment.getUser().getUserId() : null);
        dto.setBlogId(comment.getBlog() != null ? comment.getBlog().getBlogId() : null);
        dto.setParentCommentId(comment.getParentComment() != null ? comment.getParentComment().getCommentId() : null);

        List<CommentDto> replyDtos = comment.getReplies()
                .stream()
                .map(this::convertToDtoWithReplies)
                .toList();
        dto.setReplies(replyDtos);
        return dto;
    }




}
