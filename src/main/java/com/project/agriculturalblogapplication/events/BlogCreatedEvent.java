package com.project.agriculturalblogapplication.events;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BlogCreatedEvent extends ApplicationEvent{
    private Long blogId;
    private String blogTitle;
    private Long authorId;
}
