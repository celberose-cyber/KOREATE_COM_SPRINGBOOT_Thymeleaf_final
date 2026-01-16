package org.zerock.com.example.user;

import org.springframework.stereotype.Repository;
import java.sql.*;

@Repository
public class GradePolicyDAO {

    public GradePolicyDTO findPolicyByTotalSpent(Connection con, long totalSpent) throws SQLException {
        String sql = """
            SELECT grade, min_total_spent, discount_rate, point_rate
            FROM grade_policy
            WHERE min_total_spent <= ?
            ORDER BY min_total_spent DESC
            LIMIT 1
        """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, totalSpent);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                GradePolicyDTO p = new GradePolicyDTO();
                p.setGrade(rs.getString("grade"));
                p.setMinTotalSpent(rs.getLong("min_total_spent"));
                p.setDiscountRate(rs.getBigDecimal("discount_rate"));
                p.setPointRate(rs.getBigDecimal("point_rate"));
                return p;
            }
        }
    }
}
