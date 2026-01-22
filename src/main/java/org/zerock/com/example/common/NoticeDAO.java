package org.zerock.com.example.common;

import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Repository
public class NoticeDAO {

    public int countAll() throws Exception {
        String sql = "select count(*) from notices";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // 유저용 목록(페이징): 작성자 username 포함
    public List<NoticeDTO> listPaged(int size, int offset) throws Exception {
        String sql = """
            select n.id, n.title, n.content, n.created_by_user_id, u.username as created_by_username,
                   n.created_at, n.updated_at
            from notices n
            join users u on u.user_id = n.created_by_user_id
            order by n.id desc
            limit ? offset ?
        """;

        List<NoticeDTO> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, size);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public NoticeDTO findById(long id) throws Exception {
        String sql = """
            select n.id, n.title, n.content, n.created_by_user_id, u.username as created_by_username,
                   n.created_at, n.updated_at
            from notices n
            join users u on u.user_id = n.created_by_user_id
            where n.id=?
        """;

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    public void insert(String title, String content, long createdByUserId) throws Exception {
        String sql = "insert into notices(title, content, created_by_user_id) values(?,?,?)";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, content);
            ps.setLong(3, createdByUserId);
            ps.executeUpdate();
        }
    }

    public void update(long id, String title, String content) throws Exception {
        String sql = "update notices set title=?, content=?, updated_at=NOW() where id=?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, content);
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws Exception {
        String sql = "delete from notices where id=?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private NoticeDTO map(ResultSet rs) throws Exception {
        NoticeDTO n = new NoticeDTO();
        n.setId(rs.getLong("id"));
        n.setTitle(rs.getString("title"));
        n.setContent(rs.getString("content"));
        n.setCreatedByUserId(rs.getLong("created_by_user_id"));
        n.setCreatedByUsername(rs.getString("created_by_username"));
        n.setCreatedAt(rs.getTimestamp("created_at"));
        n.setUpdatedAt(rs.getTimestamp("updated_at"));
        return n;
    }
}
