package org.zerock.com.example.legal;

import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Repository
public class ConsentDAO {

    public int insert(Connection con, long userId, long docId, String ip, String ua) throws SQLException {
        // uq_user_doc 때문에 중복 POST 대비: DUPLICATE KEY UPDATE (선택)
        String sql = """
          INSERT INTO user_consents(user_id, doc_id, ip_addr, user_agent)
          VALUES(?,?,?,?)
          ON DUPLICATE KEY UPDATE
            consented_at = consented_at
        """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, docId);
            ps.setString(3, ip);
            ps.setString(4, ua);
            return ps.executeUpdate();
        }
    }
}
