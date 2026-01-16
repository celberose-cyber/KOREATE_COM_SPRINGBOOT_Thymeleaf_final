package org.zerock.com.example.user;

import org.springframework.stereotype.Repository;
import org.zerock.com.example.common.DBUtil;
import java.sql.*;
@Repository
public class UserDAO {

    // 로그인용 (email+hash)
    public UserDTO findByEmailAndHash(Connection con, String email, String hash) throws SQLException {
        String sql = """
          SELECT user_id, email, name, role, total_spent, grade, point_balance,
                 phone, privacy_agreed, email_verified
          FROM users
          WHERE email=? AND password_hash=?
        """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, hash);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                UserDTO u = new UserDTO();
                u.setUserId(rs.getLong("user_id"));
                u.setEmail(rs.getString("email"));
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
    // UserDAO
    public UserDTO findById(Connection con, long userId) throws SQLException {
        String sql = """
        SELECT user_id, email, name, role, total_spent, grade, point_balance
        FROM users
        WHERE user_id=?
    """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                UserDTO u = new UserDTO();
                u.setUserId(rs.getLong("user_id"));
                u.setEmail(rs.getString("email"));
                u.setName(rs.getString("name"));
                u.setRole(rs.getString("role"));
                u.setTotalSpent(rs.getLong("total_spent"));
                u.setGrade(rs.getString("grade"));
                u.setPointBalance(rs.getLong("point_balance"));
                return u;
            }
        }
    }

    // (편의) con 없이도 쓰고 싶으면 오버로드
    public UserDTO findById(long userId) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            return findById(con, userId);
        }
    }

    // ✅ (추가) AuthController가 쓰는 "2개 인자" 오버로드
    public UserDTO findByEmailAndHash(String email, String hash) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            return findByEmailAndHash(con, email, hash);
        }
    }

    // --- 아래는 기존 메소드들 (있다면 유지) ---

    public boolean existsEmail(Connection con, String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE email=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
    public long insertAndReturnId(Connection con, UserDTO dto) throws SQLException {
        String sql = """
          INSERT INTO users(email, password_hash, name, phone, privacy_agreed, email_verified)
          VALUES(?,?,?,?,?,?)
        """;
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, dto.getEmail());
            ps.setString(2, dto.getPasswordHash());
            ps.setString(3, dto.getName());
            ps.setString(4, dto.getPhone());
            ps.setInt(5, dto.isPrivacyAgreed() ? 1 : 0);
            ps.setInt(6, dto.isEmailVerified() ? 1 : 0);
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
    public int insert(UserDTO dto) throws SQLException {
        String sql = "INSERT INTO users(email, password_hash, name) VALUES(?,?,?)";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, dto.getEmail());
            ps.setString(2, dto.getPasswordHash());
            ps.setString(3, dto.getName());
            return ps.executeUpdate();
        }
    }

    public int addTotalSpent(Connection con, long userId, long amount) throws SQLException {
        String sql = "UPDATE users SET total_spent = total_spent + ? WHERE user_id=?";
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
        String sql = "UPDATE users SET point_balance = point_balance + ? WHERE user_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, delta);
            ps.setLong(2, userId);
            return ps.executeUpdate();
        }
    }
}
