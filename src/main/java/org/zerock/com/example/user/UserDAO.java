package org.zerock.com.example.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.zerock.com.example.common.DBUtil;
import org.springframework.jdbc.core.RowMapper;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class UserDAO {
    private final JdbcTemplate jt;

    public UserDAO(JdbcTemplate jt) {
        this.jt = jt;
    }
    // ✅ 로그인용 (username + hash)
    public UserDTO findByUsernameAndHash(Connection con, String username, String hash) throws SQLException {
        String sql = """
          SELECT user_id, username, verify_email, name, role, total_spent, grade, point_balance,
                 phone, privacy_agreed, email_verified
          FROM users
          WHERE username=? AND password_hash=?
        """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hash);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                UserDTO u = new UserDTO();
                u.setUserId(rs.getLong("user_id"));
                u.setUsername(rs.getString("username"));
                u.setVerifyEmail(rs.getString("verify_email"));
                u.setName(rs.getString("name"));
                u.setRole(rs.getString("role"));
                u.setTotalSpent(rs.getLong("total_spent"));
                u.setGrade(rs.getString("grade"));
                u.setPointBalance(rs.getLong("point_balance"));
                u.setPhone(rs.getString("phone"));
                u.setPrivacyAgreed(rs.getInt("privacy_agreed") == 1);
                u.setEmailVerified(rs.getInt("email_verified") == 1);
                return u;
            }
        }
    }

    public UserDTO findByUsernameAndHash(String username, String hash) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            return findByUsernameAndHash(con, username, hash);
        }
    }

    public boolean existsUsername(Connection con, String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existsVerifyEmail(Connection con, String verifyEmail) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE verify_email=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, verifyEmail);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long insertAndReturnId(Connection con, UserDTO dto) throws SQLException {
        String sql = """
          INSERT INTO users(username, verify_email, password_hash, name, phone, privacy_agreed, email_verified)
          VALUES(?,?,?,?,?,?,?)
        """;
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, dto.getUsername());
            ps.setString(2, dto.getVerifyEmail());
            ps.setString(3, dto.getPasswordHash());
            ps.setString(4, dto.getName());
            ps.setString(5, dto.getPhone());
            ps.setInt(6, dto.isPrivacyAgreed() ? 1 : 0);
            ps.setInt(7, dto.isEmailVerified() ? 1 : 0);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
            throw new SQLException("failed to get generated user_id");
        }
    }

    public int setEmailVerified(Connection con, long userId, boolean verified) throws SQLException {
        String sql = "UPDATE users SET email_verified=? WHERE user_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, verified ? 1 : 0);
            ps.setLong(2, userId);
            return ps.executeUpdate();
        }
    }

    // findById도 username/verify_email 포함하고 싶으면 여기처럼 확장
    public UserDTO findById(Connection con, long userId) throws SQLException {
        String sql = """
          SELECT user_id, username, verify_email, name, role, total_spent, grade, point_balance
          FROM users
          WHERE user_id=?
        """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                UserDTO u = new UserDTO();
                u.setUserId(rs.getLong("user_id"));
                u.setUsername(rs.getString("username"));
                u.setVerifyEmail(rs.getString("verify_email"));
                u.setName(rs.getString("name"));
                u.setRole(rs.getString("role"));
                u.setTotalSpent(rs.getLong("total_spent"));
                u.setGrade(rs.getString("grade"));
                u.setPointBalance(rs.getLong("point_balance"));
                return u;
            }
        }
    }

    public UserDTO findById(long userId) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            return findById(con, userId);
        }
    }
    public int addTotalSpent(Connection con, long userId, long amount) throws SQLException {
        String sql = "UPDATE users SET total_spent = GREATEST(0, total_spent + ?) WHERE user_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, amount);
            ps.setLong(2, userId);
            return ps.executeUpdate();
        }
    }

    public int updateGrade(Connection con, long userId, String grade) throws SQLException {
        String sql = "UPDATE users SET grade=? WHERE user_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, grade);
            ps.setLong(2, userId);
            return ps.executeUpdate();
        }
    }

    public int addPointBalance(Connection con, long userId, long delta) throws SQLException {
        String sql = """
        UPDATE users
           SET point_balance = GREATEST(0, point_balance + ?)
         WHERE user_id=?
    """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, delta);
            ps.setLong(2, userId);
            return ps.executeUpdate();
        }
    }
    public long getPointBalance(Connection con, long userId) throws SQLException {
        String sql = "SELECT point_balance FROM users WHERE user_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("user not found: " + userId);
                return rs.getLong(1);
            }
        }
    }

    public int setEmailVerifiedAtNow(Connection con, long userId) throws SQLException {
        String sql = "UPDATE users SET email_verified_at = NOW() WHERE user_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            return ps.executeUpdate();
        }
    }
    public String findUsernameByNameAndEmail(Connection con, String name, String verifyEmail) throws SQLException {
        String sql = """
        SELECT username
        FROM users
        WHERE name=? AND verify_email=?
        LIMIT 1
    """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, verifyEmail);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getString("username");
            }
        }
    }

    public String findUsernameByNameAndEmail(String name, String verifyEmail) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            return findUsernameByNameAndEmail(con, name, verifyEmail);
        }
    }
    public boolean existsByUsernameAndEmail(Connection con, String username, String verifyEmail) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username=? AND verify_email=? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, verifyEmail);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existsByUsernameAndEmail(String username, String verifyEmail) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            return existsByUsernameAndEmail(con, username, verifyEmail);
        }
    }
    public int updatePasswordHash(Connection con, String username, String newHash) throws SQLException {
        String sql = "UPDATE users SET password_hash=? WHERE username=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setString(2, username);
            return ps.executeUpdate();
        }
    }
    // UserDAO.java
    public List<String> findUsernamesByNameAndEmail(String name, String verifyEmail) throws Exception {
        List<String> list = new ArrayList<>();

        String sql = """
        select username
        from users
        where name = ?
          and verify_email = ?
        order by user_id asc
    """;

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, verifyEmail);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("username"));
                }
            }
        }
        return list;
    }
    public boolean existsByUsername(String username) throws Exception {
        String sql = "select 1 from users where username = ? limit 1";
        try (Connection con = DBUtil.getConnection();
             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }


    public int updatePasswordHash(String username, String newHash) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            return updatePasswordHash(con, username, newHash);
        }
    }

    public List<UserDTO> listUsers(int limit) throws Exception {
        String sql =
                "select user_id, username, name, phone, verify_email, role, grade, " +
                        "       total_spent, point_balance, created_at " +
                        "from users " +
                        "order by user_id desc limit ?";

        List<UserDTO> list = new ArrayList<>();

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UserDTO u = new UserDTO();
                    u.setUserId(rs.getLong("user_id"));
                    u.setUsername(rs.getString("username"));
                    u.setName(rs.getString("name"));
                    u.setPhone(rs.getString("phone"));
                    u.setVerifyEmail(rs.getString("verify_email"));
                    u.setRole(rs.getString("role"));
                    u.setGrade(rs.getString("grade"));
                    u.setTotalSpent(rs.getLong("total_spent"));
                    u.setPointBalance(rs.getLong("point_balance"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        u.setCreatedAt(ts.toLocalDateTime());
                    }

                    list.add(u);
                }
            }
        }
        return list;
    }


    public UserDTO findUserById(long userId) throws Exception {
        String sql =
                "select user_id, username, name, phone, verify_email, role, grade, total_spent, point_balance " +
                        "from users where user_id=?";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                UserDTO u = new UserDTO();
                u.setUserId(rs.getLong("user_id"));
                u.setUsername(rs.getString("username"));
                u.setName(rs.getString("name"));
                u.setPhone(rs.getString("phone"));
                u.setVerifyEmail(rs.getString("verify_email"));
                u.setRole(rs.getString("role"));
                u.setGrade(rs.getString("grade"));
                u.setTotalSpent(rs.getLong("total_spent"));
                u.setPointBalance(rs.getLong("point_balance"));
                return u;
            }
        }
    }


    public boolean updateUserAdmin(long userId, String name, String phone, String role, String grade) throws Exception {
        String sql =
                "update users set name=?, phone=?, role=?, grade=? where user_id=?";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, role);
            ps.setString(4, grade);
            ps.setLong(5, userId);

            return ps.executeUpdate() == 1;
        }
    }

    public int countAll(String q) {
        String sql = """
        SELECT COUNT(*)
        FROM users
        WHERE 1=1
    """ + (q != null && !q.isBlank()
                ? " AND (username LIKE CONCAT('%', ?, '%') OR name LIKE CONCAT('%', ?, '%'))"
                : "");

        if (q != null && !q.isBlank()) {
            return jt.queryForObject(sql, Integer.class, q, q);
        }
        return jt.queryForObject(sql, Integer.class);
    }


    public List<UserDTO> listPaged(
            String q,
            String sort,
            int size,
            int offset
    ) {
        String orderBy = switch (sort) {
            case "idAsc"  -> "user_id ASC";
            case "idDesc" -> "user_id DESC";
            case "newAsc" -> "created_at ASC";
            case "newDesc" -> "created_at DESC";
            default -> "user_id DESC";
        };

        boolean hasQ = (q != null && !q.isBlank());

        String sql = """
        SELECT
            user_id, username, verify_email,
            name, role, grade,
            total_spent, point_balance,
            phone, privacy_agreed, email_verified,
            created_at
        FROM users
        WHERE 1=1
    """
                + (hasQ ? " AND (username LIKE CONCAT('%', ?, '%') OR name LIKE CONCAT('%', ?, '%')) " : "")
                + " ORDER BY " + orderBy
                + " LIMIT ? OFFSET ?";

        if (hasQ) {
            return jt.query(sql, mapper(), q, q, size, offset);
        }
        return jt.query(sql, mapper(), size, offset);
    }


    // 기존 mapper가 없다면 추가 (필드명은 UserDTO에 맞게)
    private RowMapper<UserDTO> mapper() {
        return (rs, rowNum) -> {
            UserDTO u = new UserDTO();
            u.setUserId(rs.getLong("user_id"));
            u.setUsername(rs.getString("username"));
            u.setVerifyEmail(rs.getString("verify_email"));
            u.setName(rs.getString("name"));
            u.setRole(rs.getString("role"));
            u.setGrade(rs.getString("grade"));
            u.setTotalSpent(rs.getLong("total_spent"));
            u.setPointBalance(rs.getLong("point_balance"));
            u.setPhone(rs.getString("phone"));
            u.setPrivacyAgreed(rs.getInt("privacy_agreed") == 1);
            u.setEmailVerified(rs.getInt("email_verified") == 1);

            Timestamp ts = rs.getTimestamp("created_at");
            if (ts != null) {
                u.setCreatedAt(ts.toLocalDateTime());
            }
            return u;
        };
    }


    public long getTotalSpent(Connection con, long userId) throws SQLException {
        String sql = "SELECT total_spent FROM users WHERE user_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("user not found: " + userId);
                return rs.getLong(1);
            }
        }
    }
    public int setTotalSpent(Connection con, long userId, long totalSpent) throws SQLException {
        String sql = "UPDATE users SET total_spent=? WHERE user_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, Math.max(0, totalSpent));
            ps.setLong(2, userId);
            return ps.executeUpdate();
        }
    }
    // UserDAO.java (추가/보강 메서드만)
    public Long findIdByUsername(Connection con, String username) throws Exception {
        String sql = "select user_id from users where username = ? limit 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username == null ? null : username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
                return null;
            }
        }
    }

    public String findUsernameById(Connection con, Long userId) throws Exception {
        String sql = "select username from users where user_id = ? limit 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
                return null;
            }
        }
    }





}
