package org.zerock.com.example.user;

import org.springframework.stereotype.Service;
import org.zerock.com.example.common.DBUtil;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class PasswordResetService {

    private final UserDAO userDAO;
    private final PasswordResetDAO passwordResetDAO;

    public PasswordResetService(UserDAO userDAO, PasswordResetDAO passwordResetDAO) {
        this.userDAO = userDAO;
        this.passwordResetDAO = passwordResetDAO;
    }

    // 1) username+email 매칭 확인 후 reset token 발급
    public IssueResult issueResetToken(String username, String verifyEmail) throws Exception {
        if (username == null || username.isBlank()) return IssueResult.fail("아이디를 입력해주세요.");
        if (verifyEmail == null || verifyEmail.isBlank()) return IssueResult.fail("이메일을 입력해주세요.");

        String u = username.trim().toLowerCase();
        String email = verifyEmail.trim().toLowerCase();

        // ✅ 계정-이메일 매칭 확인
        boolean match = userDAO.existsByUsernameAndEmail(u, email);
        if (!match) {
            // 보안상 “존재하지 않음”을 노출하지 않으려면 메시지를 동일하게 처리해도 됨
            return IssueResult.fail("입력하신 정보와 일치하는 계정을 찾을 수 없습니다.");
        }

        // ✅ 랜덤 토큰(링크용) 생성 + 해시 저장
        String rawToken = generateToken32();
        String tokenHash = PasswordUtil.sha256(rawToken);
        Timestamp expiresAt = Timestamp.from(Instant.now().plus(30, ChronoUnit.MINUTES));

        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                passwordResetDAO.insertToken(con, u, tokenHash, expiresAt);
                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                try { con.setAutoCommit(true); } catch (Exception ignore) {}
            }
        }

        // 컨트롤러가 redirect URL을 만들 수 있게 rawToken을 반환
        return IssueResult.ok(rawToken);
    }

    // 2) token으로 비밀번호 변경
    public ResetResult resetPassword(String token, String newPassword) throws Exception {
        if (token == null || token.isBlank()) return ResetResult.fail("토큰이 없습니다. 다시 요청해주세요.");
        if (newPassword == null || newPassword.isBlank()) return ResetResult.fail("새 비밀번호를 입력해주세요.");
        if (newPassword.length() < 6) return ResetResult.fail("비밀번호는 6자 이상으로 설정해주세요.");

        String tokenHash = PasswordUtil.sha256(token.trim());

        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                var row = passwordResetDAO.findUsableByTokenHash(con, tokenHash);
                if (row == null) return ResetResult.fail("유효하지 않은 토큰입니다.");
                if (row.usedAt != null) return ResetResult.fail("이미 사용된 토큰입니다.");
                if (row.expiresAt.toInstant().isBefore(Instant.now())) return ResetResult.fail("토큰이 만료되었습니다.");

                // ✅ 비번 변경
                String newHash = PasswordUtil.sha256(newPassword);
                int updated = userDAO.updatePasswordHash(con, row.username, newHash);
                if (updated == 0) return ResetResult.fail("비밀번호 변경에 실패했습니다.");

                // ✅ 토큰 사용 처리
                int used = passwordResetDAO.markUsed(con, row.resetId);
                if (used == 0) return ResetResult.fail("이미 처리된 요청입니다.");

                con.commit();
                return ResetResult.ok("비밀번호가 변경되었습니다.");
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                try { con.setAutoCommit(true); } catch (Exception ignore) {}
            }
        }
    }

    private static String generateToken32() {
        // URL에 넣기 쉬운 토큰(영숫자)
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 32; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }

    public static class IssueResult {
        public boolean success;
        public String message;
        public String token;
        static IssueResult ok(String token) { var r=new IssueResult(); r.success=true; r.token=token; r.message="OK"; return r; }
        static IssueResult fail(String msg) { var r=new IssueResult(); r.success=false; r.message=msg; return r; }
    }

    public static class ResetResult {
        public boolean success;
        public String message;
        static ResetResult ok(String msg) { var r=new ResetResult(); r.success=true; r.message=msg; return r; }
        static ResetResult fail(String msg) { var r=new ResetResult(); r.success=false; r.message=msg; return r; }
    }
}
