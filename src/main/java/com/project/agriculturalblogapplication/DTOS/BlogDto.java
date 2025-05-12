package com.project.agriculturalblogapplication.DTOS;

import com.project.agriculturalblogapplication.Models.Comments;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlogDto {

    private Long blogId;
    private String title;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentDto> comments;
    private UserDto author;


//    @Override
//    public String toString() {
//        return "BlogDto{" +
//                "blogId=" + blogId +
//                ", title='" + title + '\'' +
//                ", content='" + content + '\'' +
//                ", imageUrl='" + imageUrl + '\'' +
//                ", createdAt=" + createdAt +
//                ", updatedAt=" + updatedAt +
//                '}';
//    }


}


