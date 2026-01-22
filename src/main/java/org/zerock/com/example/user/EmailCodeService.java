package org.zerock.com.example.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zerock.com.example.common.DBUtil;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class EmailCodeService {

    private final EmailVerificationDAO emailVerificationDAO;
    private final EmailSender emailSender;
    @Value("${app.base-url}")
    private String appBaseUrl;   // ✅ 클래스 필드에 선언

    private String base() {
        if (appBaseUrl == null || appBaseUrl.isBlank()) {
            throw new IllegalStateException("app.base-url is not configured");
        }
        return appBaseUrl.endsWith("/")
                ? appBaseUrl.substring(0, appBaseUrl.length() - 1)
                : appBaseUrl;
    }
    public EmailCodeService(EmailVerificationDAO emailVerificationDAO, EmailSender emailSender) {
        this.emailVerificationDAO = emailVerificationDAO;
        this.emailSender = emailSender;
    }

    // =========================
    // ✅ 신규: purpose 기반 공용 API
    // =========================
    public SendResult sendCode(String verifyEmail, String purpose) throws Exception {
        if (verifyEmail == null || verifyEmail.isBlank()) {
            return SendResult.fail("이메일을 입력해주세요.");
        }
        String email = verifyEmail.trim().toLowerCase();
        String p = normalizePurpose(purpose); // REGISTER / FIND_ID / RESET_PW ...

        // 6자리 숫자 코드
        String code = generate6DigitCode();
        String tokenHash = PasswordUtil.sha256(code);
        Timestamp expiresAt = Timestamp.from(Instant.now().plus(10, ChronoUnit.MINUTES));

        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                emailVerificationDAO.insertTokenForEmail(con, email, p, tokenHash, expiresAt);
                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                try { con.setAutoCommit(true); } catch (Exception ignore) {}
            }
        }

        String subject = "[KOREATE] 이메일 인증코드";
        String html = buildEmailHtml(code);

        emailSender.sendHtml(email, subject, html);

        return SendResult.ok("인증코드를 발송했습니다. 메일함을 확인해주세요.");
    }

    public VerifyResult verifyCode(String verifyEmail, String code, String purpose) throws Exception {
        if (verifyEmail == null || verifyEmail.isBlank()) {
            return VerifyResult.fail("이메일을 입력해주세요.");
        }
        if (code == null || code.isBlank()) {
            return VerifyResult.fail("인증코드를 입력해주세요.");
        }

        String email = verifyEmail.trim().toLowerCase();
        String p = normalizePurpose(purpose);

        String c = code.trim();

        // 숫자 6자리만 허용 (원하면 정책 변경 가능)
        if (!c.matches("\\d{6}")) {
            return VerifyResult.fail("인증코드는 6자리 숫자입니다.");
        }

        String tokenHash = PasswordUtil.sha256(c);

        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                var row = emailVerificationDAO.findUsableByEmailAndTokenHash(con, email, p, tokenHash);
                if (row == null) return VerifyResult.fail("인증코드가 올바르지 않습니다.");
                if (row.usedAt != null) return VerifyResult.fail("이미 사용된 인증코드입니다.");
                if (row.expiresAt.toInstant().isBefore(Instant.now())) return VerifyResult.fail("인증코드가 만료되었습니다.");

                int used = emailVerificationDAO.markUsed(con, row.verifyId);
                if (used == 0) return VerifyResult.fail("이미 처리된 인증코드입니다.");

                con.commit();
                return VerifyResult.ok("이메일 인증이 완료되었습니다.");
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                try { con.setAutoCommit(true); } catch (Exception ignore) {}
            }
        }
    }

    // =========================
    // ✅ 기존 호환 메서드 (REGISTER 전용 유지)
    // =========================
    public SendResult sendRegisterCode(String verifyEmail) throws Exception {
        return sendCode(verifyEmail, "REGISTER");
    }

    public VerifyResult verifyRegisterCode(String verifyEmail, String code) throws Exception {
        return verifyCode(verifyEmail, code, "REGISTER");
    }

    // =========================
    // 내부 유틸
    // =========================
    private static String normalizePurpose(String purpose) {
        if (purpose == null || purpose.isBlank()) return "REGISTER";
        return purpose.trim().toUpperCase();
    }

    private static String generate6DigitCode() {
        int n = new SecureRandom().nextInt(900000) + 100000;
        return String.valueOf(n);
    }

    private static String buildEmailHtml(String code) {
        return """
        <!doctype html>
        <html lang="ko">
          <body>
            <p>아래 인증코드를 입력해 주세요.</p>
            <div style="font-size:22px;font-weight:800;letter-spacing:2px;
                        padding:12px 14px;border:1px solid #eee;border-radius:12px;
                        display:inline-block;background:#fafafa;">
              %s
            </div>
            <p style="color:#777;font-size:12px;margin-top:10px;">(10분 내 만료)</p>
          </body>
        </html>
        """.formatted(code);
    }

    // EmailCodeService.java 내부에 추가
    public void sendFoundUsernamesEmail(
            String verifyEmail,
            List<String> usernames
    ) throws Exception {

        String email = verifyEmail.trim().toLowerCase();
        String subject = "[KOREATE] 아이디 찾기 결과 안내";

        StringBuilder sb = new StringBuilder();
        sb.append("<ul style='padding-left:18px;'>");
        for (String u : usernames) {
            if (u == null) continue;
            sb.append("<li style='margin:6px 0;font-weight:800;'>")
                    .append(escapeHtml(u))
                    .append("</li>");
        }
        sb.append("</ul>");

        String loginUrl = base() + "/login"; // ✅ 절대 URL

        String html = """
        <!doctype html>
        <html lang="ko">
          <body style="font-family:Arial,sans-serif;">
            <p>요청하신 아이디 찾기 결과입니다.</p>

            %s

            <div style="margin-top:16px;">
              <a href="%s"
                 target="_blank"
                 rel="noopener noreferrer"
                 style="color:#1a73e8;font-weight:800;text-decoration:underline;">
                 로그인 페이지로 이동
              </a>
            </div>
          </body>
        </html>
        """.formatted(sb.toString(), loginUrl);

        emailSender.sendHtml(email, subject, html);
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }


    // =========================
    // 결과 타입
    // =========================
    public static class SendResult {
        public boolean success;
        public String message;
        static SendResult ok(String msg){ var r=new SendResult(); r.success=true; r.message=msg; return r; }
        static SendResult fail(String msg){ var r=new SendResult(); r.success=false; r.message=msg; return r; }
    }

    public static class VerifyResult {
        public boolean success;
        public String message;
        static VerifyResult ok(String msg){ var r=new VerifyResult(); r.success=true; r.message=msg; return r; }
        static VerifyResult fail(String msg){ var r=new VerifyResult(); r.success=false; r.message=msg; return r; }
    }
}
