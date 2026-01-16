package org.zerock.com.example.user;

public class UserDTO {

    private long userId;
    private String email;
    private String passwordHash;
    private String name;
    private String role;

    private String grade;
    private long totalSpent;
    private long pointBalance;

    private String phone;
    private boolean privacyAgreed;
    private boolean emailVerified;

    // ===== 기본 정보 =====
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    // ===== 확장 정보 =====
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public long getTotalSpent() { return totalSpent; }
    public void setTotalSpent(long totalSpent) { this.totalSpent = totalSpent; }

    public long getPointBalance() { return pointBalance; }
    public void setPointBalance(long pointBalance) { this.pointBalance = pointBalance; }

    // ===== 신규 필드 =====
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean isPrivacyAgreed() { return privacyAgreed; }
    public void setPrivacyAgreed(boolean privacyAgreed) {
        this.privacyAgreed = privacyAgreed;
    }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}
