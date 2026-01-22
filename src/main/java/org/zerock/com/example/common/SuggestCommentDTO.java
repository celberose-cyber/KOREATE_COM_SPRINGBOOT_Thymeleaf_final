package org.zerock.com.example.common;

import java.sql.Timestamp;

public class SuggestCommentDTO {
    private long id;
    private long suggestId;

    private long authorUserId;
    private String authorRole;      // USER/ADMIN
    private String authorUsername;  // JOIN 표시용

    private String content;
    private Timestamp createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getSuggestId() { return suggestId; }
    public void setSuggestId(long suggestId) { this.suggestId = suggestId; }

    public long getAuthorUserId() { return authorUserId; }
    public void setAuthorUserId(long authorUserId) { this.authorUserId = authorUserId; }

    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }

    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
