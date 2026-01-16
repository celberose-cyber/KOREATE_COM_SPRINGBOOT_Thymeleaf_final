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

    public static class VerifyRow {
        public long verifyId;
        public long userId;
        public Timestamp expiresAt;
        public Timestamp usedAt;
    }
}
