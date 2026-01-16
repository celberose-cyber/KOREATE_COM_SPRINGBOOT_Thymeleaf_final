package org.zerock.com.example.legal;

import org.springframework.stereotype.Service;
import org.zerock.com.example.common.DBUtil;

import java.sql.Connection;

@Service
public class LegalService {
    private final LegalDAO legalDAO;

    public LegalService(LegalDAO legalDAO) {
        this.legalDAO = legalDAO;
    }

    public LegalDocView getActiveDocView(String docType) throws Exception {
        try (Connection con = DBUtil.getConnection()) {
            LegalDocumentDTO d = legalDAO.findActiveByType(con, docType);
            if (d == null) return null;

            String html = d.getContentHtmlCache();
            if (html == null || html.isBlank()) {
                html = MarkdownUtil.mdToHtml(d.getContentMd());
                // 옵션: 캐시 저장 (원하면 유지)
                legalDAO.updateHtmlCache(con, d.getDocId(), html);
            }

            LegalDocView v = new LegalDocView();
            v.docId = d.getDocId();
            v.title = d.getTitle();
            v.version = d.getVersion();
            v.html = html;
            return v;
        }
    }

    public static class LegalDocView {
        public long docId;
        public String title;
        public String version;
        public String html;
    }
}
