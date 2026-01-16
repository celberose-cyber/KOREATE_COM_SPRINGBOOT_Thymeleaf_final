package org.zerock.com.example.user;

import org.springframework.stereotype.Service;
import org.zerock.com.example.common.DBUtil;
import org.zerock.com.example.legal.ConsentDAO;
import org.zerock.com.example.legal.LegalDAO;
import org.zerock.com.example.legal.LegalDocumentDTO;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class RegisterService {

    private final UserDAO userDAO;
    private final LegalDAO legalDAO;
    private final ConsentDAO consentDAO;
    private final EmailVerificationDAO emailVerificationDAO;
    private final EmailSender emailSender;

    public RegisterService(UserDAO userDAO,
                           LegalDAO legalDAO,
                           ConsentDAO consentDAO,
                           EmailVerificationDAO emailVerificationDAO,
                           EmailSender emailSender) {
        this.userDAO = userDAO;
        this.legalDAO = legalDAO;
        this.consentDAO = consentDAO;
        this.emailVerificationDAO = emailVerificationDAO;
        this.emailSender = emailSender;
    }

    public RegisterResult register(RegisterCommand cmd) throws Exception {
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                // 1) 활성 문서 존재 체크(문서 자체가 있어야 함)
                LegalDocumentDTO terms = legalDAO.findActiveByType(con, "TERMS");
                LegalDocumentDTO privacy = legalDAO.findActiveByType(con, "PRIVACY");
                if (terms == null || privacy == null) {
                    con.rollback();
                    return RegisterResult.fail("활성 약관/개인정보 문서가 없습니다. 관리자에게 문의하세요.");
                }

                // 1-1) 사용자가 본 docId가 아직 active인지 검증 (핵심)
                if (cmd.termsDocId == null || cmd.privacyDocId == null) {
                    con.rollback();
                    return RegisterResult.fail("약관 버전 정보가 누락되었습니다. 새로고침 후 다시 시도해주세요.");
                }
                if (!legalDAO.isActiveDocId(con, "TERMS", cmd.termsDocId) ||
                        !legalDAO.isActiveDocId(con, "PRIVACY", cmd.privacyDocId)) {
                    con.rollback();
                    return RegisterResult.fail("약관 버전이 변경되었습니다. 새로고침 후 다시 시도해주세요.");
                }

                // 2) 이메일 중복 체크
                if (userDAO.existsEmail(con, cmd.email)) {
                    con.rollback();
                    return RegisterResult.fail("이미 등록된 아이디입니다.");
                }

                // 3) 유저 생성 (✅ userId 여기서 만들어짐)
                UserDTO dto = new UserDTO();
                dto.setEmail(cmd.email);
                dto.setPasswordHash(cmd.passwordHash);
                dto.setName(cmd.name);
                dto.setPhone(cmd.phone);
                dto.setPrivacyAgreed(true);
                dto.setEmailVerified(false);

                long userId = userDAO.insertAndReturnId(con, dto);

                // 4) 동의 이력 저장 (폼에서 받은 docId로 저장)
                consentDAO.insert(con, userId, cmd.termsDocId, cmd.ipAddr, cmd.userAgent);
                consentDAO.insert(con, userId, cmd.privacyDocId, cmd.ipAddr, cmd.userAgent);

                // 5) 이메일 인증 토큰 생성/저장
                String token = generateToken(32);
                String tokenHash = PasswordUtil.sha256(token);

                Timestamp expiresAt = Timestamp.from(Instant.now().plus(30, ChronoUnit.MINUTES));
                emailVerificationDAO.insertToken(con, userId, tokenHash, expiresAt);

                con.commit();

                // 6) 메일 발송 (커밋 후!)
                String verifyUrl = cmd.appBaseUrl + "/verify-email?token=" + token;
                emailSender.send(cmd.email,
                        "[KOREATE] 이메일 인증",
                        "아래 링크를 눌러 인증을 완료하세요:\n" + verifyUrl + "\n\n(30분 내 만료)");

                return RegisterResult.ok("가입 완료! 이메일 인증 후 로그인할 수 있습니다.");
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                try { con.setAutoCommit(true); } catch (Exception ignore) {}
            }
        }
    }

    private static String generateToken(int bytes) {
        byte[] buf = new byte[bytes];
        new SecureRandom().nextBytes(buf);
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static class RegisterCommand {
        public String email;
        public String passwordHash;
        public String name;
        public String phone;
        public String ipAddr;
        public String userAgent;
        public String appBaseUrl; // 예: http://localhost:8080
        public Long termsDocId;
        public Long privacyDocId;
    }

    public static class RegisterResult {
        public boolean success;
        public String message;

        public static RegisterResult ok(String msg) {
            RegisterResult r = new RegisterResult();
            r.success = true; r.message = msg; return r;
        }
        public static RegisterResult fail(String msg) {
            RegisterResult r = new RegisterResult();
            r.success = false; r.message = msg; return r;
        }
    }
}
