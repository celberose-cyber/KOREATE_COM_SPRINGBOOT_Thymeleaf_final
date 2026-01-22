package org.zerock.com.example.user;

import org.springframework.stereotype.Service;
import org.zerock.com.example.common.DBUtil;
import org.zerock.com.example.legal.ConsentDAO;
import org.zerock.com.example.legal.LegalDAO;
import org.zerock.com.example.legal.LegalDocumentDTO;

import java.sql.Connection;

@Service
public class RegisterService {

    private final UserDAO userDAO;
    private final LegalDAO legalDAO;
    private final ConsentDAO consentDAO;

    public RegisterService(UserDAO userDAO,
                           LegalDAO legalDAO,
                           ConsentDAO consentDAO) {
        this.userDAO = userDAO;
        this.legalDAO = legalDAO;
        this.consentDAO = consentDAO;
    }

    public RegisterResult register(RegisterCommand cmd) throws Exception {
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                LegalDocumentDTO terms = legalDAO.findActiveByType(con, "TERMS");
                LegalDocumentDTO privacy = legalDAO.findActiveByType(con, "PRIVACY");
                if (terms == null || privacy == null) {
                    con.rollback();
                    return RegisterResult.fail("활성 약관/개인정보 문서가 없습니다. 관리자에게 문의하세요.");
                }

                if (cmd.termsDocId == null || cmd.privacyDocId == null) {
                    con.rollback();
                    return RegisterResult.fail("약관 버전 정보가 누락되었습니다. 새로고침 후 다시 시도해주세요.");
                }

                if (!legalDAO.isActiveDocId(con, "TERMS", cmd.termsDocId)
                        || !legalDAO.isActiveDocId(con, "PRIVACY", cmd.privacyDocId)) {
                    con.rollback();
                    return RegisterResult.fail("약관 버전이 변경되었습니다. 새로고침 후 다시 시도해주세요.");
                }

                // ✅ username 중복 체크
                if (userDAO.existsUsername(con, cmd.username)) {
                    con.rollback();
                    return RegisterResult.fail("이미 사용 중인 아이디입니다.");
                }

                // (선택) verify_email 중복 체크 - 실서비스에서는 활성화 권장
                // if (userDAO.existsVerifyEmail(con, cmd.verifyEmail)) {
                //     con.rollback();
                //     return RegisterResult.fail("이미 등록된 이메일입니다.");
                // }

                UserDTO dto = new UserDTO();
                dto.setUsername(cmd.username);
                dto.setVerifyEmail(cmd.verifyEmail);
                dto.setPasswordHash(cmd.passwordHash);
                dto.setName(cmd.name);
                dto.setPhone(cmd.phone);
                dto.setPrivacyAgreed(true);

                // ✅ 가입 전 코드 인증을 통과했으므로, 가입 시점에 "이메일 인증 완료"로 저장
                dto.setEmailVerified(true);

                long userId = userDAO.insertAndReturnId(con, dto);

                // ✅ email_verified_at 기록(컬럼이 있을 때만)
                userDAO.setEmailVerifiedAtNow(con, userId);

                consentDAO.insert(con, userId, cmd.termsDocId, cmd.ipAddr, cmd.userAgent);
                consentDAO.insert(con, userId, cmd.privacyDocId, cmd.ipAddr, cmd.userAgent);

                con.commit();
                return RegisterResult.ok("가입 완료! 로그인할 수 있습니다.");
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                try { con.setAutoCommit(true); } catch (Exception ignore) {}
            }
        }
    }

    public static class RegisterCommand {
        public String username;
        public String verifyEmail;
        public String passwordHash;
        public String name;
        public String phone;
        public String ipAddr;
        public String userAgent;
        public Long termsDocId;
        public Long privacyDocId;
    }

    public static class RegisterResult {
        public boolean success;
        public String message;

        public static RegisterResult ok(String msg) {
            RegisterResult r = new RegisterResult();
            r.success = true;
            r.message = msg;
            return r;
        }

        public static RegisterResult fail(String msg) {
            RegisterResult r = new RegisterResult();
            r.success = false;
            r.message = msg;
            return r;
        }
    }
}
