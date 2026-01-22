package org.zerock.com.example.user;

import org.springframework.stereotype.Repository;

import java.sql.*;

@Repository
public class EmailVerificationDAO {

    public int insertToken(Connection con, long userId, String tokenHash, Timestamp expiresAt) throws SQLException {
        String sql = """
          INSERT INTO email_verifications(user_id, token_hash, expires_at)
          VALUES(?,?,?)
        """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, tokenHash);
            ps.setTimestamp(3, expiresAt);
            return ps.executeUpdate();
        }
    }

    public VerifyRow findUsableByTokenHash(Connection con, String tokenHash) throws SQLException {
        String sql = """
      SELECT verify_id, user_id, expires_at, used_at
      FROM email_verifications
      WHERE token_hash=?
      LIMIT 1
    """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                VerifyRow r = new VerifyRow();
                r.verifyId = rs.getLong("verify_id");
                r.userId = rs.getLong("user_id");
                r.expiresAt = rs.getTimestamp("expires_at");
                r.usedAt = rs.getTimestamp("used_at");
                return r;
            }
        }
    }


    public int markUsed(Connection con, long verifyId) throws SQLException {
        String sql = "UPDATE email_verifications SET used_at=NOW() WHERE verify_id=? AND used_at IS NULL";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, verifyId);
            return ps.executeUpdate();
        }
    }
    // EmailVerificationDAO.java 안에 추가

    public int insertTokenForEmail(Connection con, String verifyEmail, String purpose,
                                   String tokenHash, Timestamp expiresAt) throws SQLException {
        String sql = """
      INSERT INTO email_verifications(user_id, verify_email, purpose, token_hash, expires_at)
      VALUES(NULL, ?, ?, ?, ?)
    """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, verifyEmail);
            ps.setString(2, purpose);
            ps.setString(3, tokenHash);
            ps.setTimestamp(4, expiresAt);
            return ps.executeUpdate();
        }
    }

    public VerifyRow findUsableByEmailAndTokenHash(Connection con, String verifyEmail, String purpose,
                                                   String tokenHash) throws SQLException {
        String sql = """
      SELECT verify_id, user_id, expires_at, used_at
      FROM email_verifications
      WHERE verify_email=? AND purpose=? AND token_hash=?
      ORDER BY verify_id DESC
      LIMIT 1
    """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, verifyEmail);
            ps.setString(2, purpose);
            ps.setString(3, tokenHash);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                VerifyRow r = new VerifyRow();
                r.verifyId = rs.getLong("verify_id");
                r.userId = rs.getLong("user_id"); // NULL일 수 있음
                r.expiresAt = rs.getTimestamp("expires_at");
                r.usedAt = rs.getTimestamp("used_at");
                return r;
            }
        }
    }

    public static class VerifyRow {
        public long verifyId;
        public long userId;
        public Timestamp expiresAt;
        public Timestamp usedAt;
    }
}
