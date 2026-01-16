package org.zerock.com.example.legal;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.zerock.com.example.common.DBUtil;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;

@Component
public class LegalSeeder {

    private final LegalDAO legalDAO;

    public LegalSeeder(LegalDAO legalDAO) {
        this.legalDAO = legalDAO;
    }

    // 앱 시작 후 1회 실행
    @jakarta.annotation.PostConstruct
    public void seed() {
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                seedOne(con, "TERMS", "legal/terms.md", "v1.0", "이용약관");
                seedOne(con, "PRIVACY", "legal/privacy.md", "v1.0", "개인정보처리방침");
                con.commit();
            } catch (Exception e) {
                con.rollback();
                // 개발 단계: 로그만 찍고 넘어가도 됨
                System.err.println("[LegalSeeder] seed failed: " + e.getMessage());
            } finally {
                try { con.setAutoCommit(true); } catch (Exception ignore) {}
            }
        } catch (Exception e) {
            System.err.println("[LegalSeeder] DB connect failed: " + e.getMessage());
        }
    }

    private void seedOne(Connection con, String docType, String classpath, String version, String title) throws Exception {
        String md = readClasspath(classpath);
        String html = MarkdownUtil.mdToHtml(md);

        var latest = legalDAO.findLatestByType(con, docType);

        if (latest == null) {
            // 처음이면 insert + active
            legalDAO.insert(con, docType, version, title, md, html, 1);
            return;
        }

        // 이미 있으면: md가 바뀐 경우만 업데이트(같은 doc_id에 덮어쓰기)
        // 운영에서 “버전 개정 이력”을 남기고 싶으면 여기서 insert로 새 버전 추가하도록 변경하면 됨
        String old = latest.getContentMd() == null ? "" : latest.getContentMd();
        if (!old.equals(md)) {
            legalDAO.updateContent(con, latest.getDocId(), md, html);
        }

        // active가 꺼져있으면 켜주기(개발환경 편의)
        if (!latest.isActive()) {
            legalDAO.deactivateAll(con, docType);
            legalDAO.activateDoc(con, latest.getDocId());
        }
    }

    private static String readClasspath(String path) throws Exception {
        var res = new ClassPathResource(path);
        byte[] bytes = res.getInputStream().readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
