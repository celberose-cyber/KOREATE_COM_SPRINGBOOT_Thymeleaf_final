package org.zerock.com.example.legal;

import org.springframework.stereotype.Repository;

import java.sql.*;

@Repository
public class LegalDAO {

    public LegalDocumentDTO findActiveByType(Connection con, String docType) throws SQLException {
        String sql = """
          SELECT doc_id, doc_type, version, title, content_md, content_html_cache, is_active
          FROM legal_documents
          WHERE doc_type=? AND is_active=1
          ORDER BY effective_from DESC
          LIMIT 1
        """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, docType);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                LegalDocumentDTO d = new LegalDocumentDTO();
                d.setDocId(rs.getLong("doc_id"));
                d.setDocType(rs.getString("doc_type"));
                d.setVersion(rs.getString("version"));
                d.setTitle(rs.getString("title"));
                d.setContentMd(rs.getString("content_md"));
                d.setContentHtmlCache(rs.getString("content_html_cache"));
                d.setActive(rs.getInt("is_active") == 1);
                return d;
            }
        }
    }

    public int updateHtmlCache(Connection con, long docId, String html) throws SQLException {
        String sql = "UPDATE legal_documents SET content_html_cache=?, updated_at=NOW() WHERE doc_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, html);
            ps.setLong(2, docId);
            return ps.executeUpdate();
        }
    }
    public boolean isActiveDocId(Connection con, String docType, long docId) throws SQLException {
        String sql = "SELECT 1 FROM legal_documents WHERE doc_id=? AND doc_type=? AND is_active=1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, docId);
            ps.setString(2, docType);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
    public LegalDocumentDTO findLatestByType(Connection con, String docType) throws SQLException {
        String sql = """
      SELECT doc_id, doc_type, version, title, content_md, content_html_cache, is_active
      FROM legal_documents
      WHERE doc_type=?
      ORDER BY effective_from DESC
      LIMIT 1
    """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, docType);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                LegalDocumentDTO d = new LegalDocumentDTO();
                d.setDocId(rs.getLong("doc_id"));
                d.setDocType(rs.getString("doc_type"));
                d.setVersion(rs.getString("version"));
                d.setTitle(rs.getString("title"));
                d.setContentMd(rs.getString("content_md"));
                d.setContentHtmlCache(rs.getString("content_html_cache"));
                d.setActive(rs.getInt("is_active")==1);
                return d;
            }
        }
    }
    public long insert(Connection con, String docType, String version, String title,
                       String md, String html, int isActive) throws SQLException {
        String sql = """
      INSERT INTO legal_documents(doc_type, version, title, content_md, content_html_cache, is_active, effective_from)
      VALUES(?,?,?,?,?,?,NOW())
    """;
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, docType);
            ps.setString(2, version);
            ps.setString(3, title);
            ps.setString(4, md);
            ps.setString(5, html);
            ps.setInt(6, isActive);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public int updateContent(Connection con, long docId, String md, String html) throws SQLException {
        String sql = "UPDATE legal_documents SET content_md=?, content_html_cache=?, updated_at=NOW() WHERE doc_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, md);
            ps.setString(2, html);
            ps.setLong(3, docId);
            return ps.executeUpdate();
        }
    }
    public int deactivateAll(Connection con, String docType) throws SQLException {
        String sql = "UPDATE legal_documents SET is_active=0, updated_at=NOW() WHERE doc_type=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, docType);
            return ps.executeUpdate();
        }
    }
    public int activateDoc(Connection con, long docId) throws SQLException {
        String sql = "UPDATE legal_documents SET is_active=1, updated_at=NOW() WHERE doc_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, docId);
            return ps.executeUpdate();
        }
    }



}
