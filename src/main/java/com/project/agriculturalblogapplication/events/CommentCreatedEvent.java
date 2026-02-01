package com.project.agriculturalblogapplication.events;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentCreatedEvent extends ApplicationEvent{
    private Long commentId;
    private Long blogId;
    private String blogTitle;
    private Long blogAuthorId;
    private String commentText;
    private Long commenterUserId;
    private String commenterUserName;
}
