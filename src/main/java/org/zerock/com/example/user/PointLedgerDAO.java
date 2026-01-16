package org.zerock.com.example.user;

import org.springframework.stereotype.Repository;
import java.sql.*;

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
}
