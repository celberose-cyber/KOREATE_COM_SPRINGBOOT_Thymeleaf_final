package org.zerock.com.example.user;

import org.springframework.stereotype.Repository;
import java.sql.*;
import java.util.List;

@Repository
public class PointLedgerDAO {

    public void insert(Connection con, long userId, Long orderId, long delta, String reason) throws SQLException {
        String sql = """
            INSERT INTO point_ledger(user_id, order_id, delta, reason)
            VALUES(?,?,?,?)
        """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            if (orderId == null) ps.setNull(2, Types.BIGINT);
            else ps.setLong(2, orderId);
            ps.setLong(3, delta);
            ps.setString(4, reason);
            ps.executeUpdate();
        }
    }
    // ✅ (선택) 최근 포인트 변경 내역 조회(관리자/회원 화면에 쓰기 좋음)
    public List<PointLedgerRow> listRecentByUser(Connection con, long userId, int limit) throws SQLException {
        String sql = """
            SELECT ledger_id, user_id, order_id, delta, reason, created_at
              FROM point_ledger
             WHERE user_id=?
             ORDER BY ledger_id DESC
             LIMIT ?
        """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, Math.max(1, Math.min(200, limit)));
            try (ResultSet rs = ps.executeQuery()) {
                List<PointLedgerRow> out = new java.util.ArrayList<>();
                while (rs.next()) {
                    out.add(new PointLedgerRow(
                            rs.getLong("ledger_id"),
                            rs.getLong("user_id"),
                            (Long) rs.getObject("order_id"),
                            rs.getLong("delta"),
                            rs.getString("reason"),
                            rs.getTimestamp("created_at")
                    ));
                }
                return out;
            }
        }
    }
    public long sumEarnedPointsByOrder(Connection con, long orderId) throws SQLException {
        String sql = """
        SELECT COALESCE(SUM(delta),0)
          FROM point_ledger
         WHERE order_id=?
           AND reason='ORDER_EARN'
    """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }
    public boolean existsRevokeByOrder(Connection con, long orderId) throws SQLException {
        String sql = """
        SELECT 1
          FROM point_ledger
         WHERE order_id=?
           AND reason='ORDER_REVOKE'
         LIMIT 1
    """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public record PointLedgerRow(
            long ledgerId,
            long userId,
            Long orderId,
            long delta,
            String reason,
            Timestamp createdAt
    ) {}

}
