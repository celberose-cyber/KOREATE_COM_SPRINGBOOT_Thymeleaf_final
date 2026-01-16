package org.zerock.com.example.user;

import org.springframework.stereotype.Service;
import org.zerock.com.example.common.DBUtil;

import java.sql.Connection;
import java.time.Instant;

@Service
public class VerifyEmailService {

    private final EmailVerificationDAO emailVerificationDAO;
    private final UserDAO userDAO;

    public VerifyEmailService(EmailVerificationDAO emailVerificationDAO,
                              UserDAO userDAO) {
        this.emailVerificationDAO = emailVerificationDAO;
        this.userDAO = userDAO;
    }

    public VerifyResult verify(String token) throws Exception {
        if (token == null || token.isBlank()) {
            return VerifyResult.fail("인증 토큰이 없습니다.");
        }

        String tokenHash = PasswordUtil.sha256(token);

        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                var row = emailVerificationDAO.findUsableByTokenHash (con, tokenHash);

                if (row == null) {
                    return VerifyResult.fail("유효하지 않은 인증 링크입니다.");
                }
                if (row.usedAt != null) {
                    return VerifyResult.fail("이미 사용된 인증 링크입니다.");
                }
                if (row.expiresAt.toInstant().isBefore(Instant.now())) {
                    return VerifyResult.fail("인증 링크가 만료되었습니다.");
                }

                // ✅ 이메일 인증 완료 처리
                int used = emailVerificationDAO.markUsed(con, row.verifyId);
                if (used == 0) return VerifyResult.fail("이미 처리된 인증 링크입니다.");

                userDAO.setEmailVerified(con, row.userId, true);
                con.commit();
                return VerifyResult.ok("이메일 인증이 완료되었습니다.");
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }

    public static class VerifyResult {
        public boolean success;
        public String message;

        static VerifyResult ok(String msg) {
            var r = new VerifyResult();
            r.success = true;
            r.message = msg;
            return r;
        }

        static VerifyResult fail(String msg) {
            var r = new VerifyResult();
            r.success = false;
            r.message = msg;
            return r;
        }
    }
}
