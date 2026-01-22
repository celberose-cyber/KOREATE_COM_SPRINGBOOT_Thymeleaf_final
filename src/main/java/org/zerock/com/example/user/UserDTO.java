package org.zerock.com.example.user;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class UserDTO {

    private long userId;

    private String username;      // ✅ 로그인 ID
    private String verifyEmail;   // ✅ 인증메일/연락메일

    private String passwordHash;
    private String name;
    private String role;

    private String grade;
    private long totalSpent;
    private long pointBalance;

    private String phone;
    private boolean privacyAgreed;
    private boolean emailVerified;

    private LocalDateTime createdAt;

    // getter / setter
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getVerifyEmail() { return verifyEmail; }
    public void setVerifyEmail(String verifyEmail) { this.verifyEmail = verifyEmail; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public long getTotalSpent() { return totalSpent; }
    public void setTotalSpent(long totalSpent) { this.totalSpent = totalSpent; }

    public long getPointBalance() { return pointBalance; }
    public void setPointBalance(long pointBalance) { this.pointBalance = pointBalance; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean isPrivacyAgreed() { return privacyAgreed; }
    public void setPrivacyAgreed(boolean privacyAgreed) { this.privacyAgreed = privacyAgreed; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }


}
