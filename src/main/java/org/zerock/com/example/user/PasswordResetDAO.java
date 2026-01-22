package org.zerock.com.example.user;

import org.springframework.stereotype.Repository;

import java.sql.*;

@Repository
public class PasswordResetDAO {

    public long insertToken(Connection con, String username, String tokenHash, Timestamp expiresAt) throws SQLException {
        String sql = """
            INSERT INTO password_reset_tokens(username, token_hash, expires_at)
            VALUES(?,?,?)
        """;
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, tokenHash);
            ps.setTimestamp(3, expiresAt);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
            throw new SQLException("failed to get generated reset_id");
        }
    }

    public Row findUsableByTokenHash(Connection con, String tokenHash) throws SQLException {
        String sql = """
            SELECT reset_id, username, token_hash, expires_at, used_at
            FROM password_reset_tokens
            WHERE token_hash=?
            ORDER BY reset_id DESC
            LIMIT 1
        """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Row r = new Row();
                r.resetId = rs.getLong("reset_id");
                r.username = rs.getString("username");
                r.tokenHash = rs.getString("token_hash");
                r.expiresAt = rs.getTimestamp("expires_at");
                r.usedAt = rs.getTimestamp("used_at");
                return r;
            }
        }
    }

    public int markUsed(Connection con, long resetId) throws SQLException {
        String sql = "UPDATE password_reset_tokens SET used_at=NOW() WHERE reset_id=? AND used_at IS NULL";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, resetId);
            return ps.executeUpdate();
        }
    }

    public static class Row {
        public long resetId;
        public String username;
        public String tokenHash;
        public Timestamp expiresAt;
        public Timestamp usedAt;
    }
}
