package org.zerock.com.example.common;

import java.sql.Timestamp;

public class SuggestDTO {
    private long id;
    private long userId;
    private String writerUsername; // JOIN 표시용

    private String title;
    private String content;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    // ✅ 추가: 댓글수
    private int commentCount;        // 전체 댓글 수
    private int adminCommentCount;   // 관리자 댓글 수

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public int getAdminCommentCount() { return adminCommentCount; }
    public void setAdminCommentCount(int adminCommentCount) { this.adminCommentCount = adminCommentCount; }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getWriterUsername() { return writerUsername; }
    public void setWriterUsername(String writerUsername) { this.writerUsername = writerUsername; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
