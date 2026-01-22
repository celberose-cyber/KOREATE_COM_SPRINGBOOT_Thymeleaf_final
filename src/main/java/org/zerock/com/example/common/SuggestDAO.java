package org.zerock.com.example.common;

import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SuggestDAO {

    public int countAll() throws Exception {
        String sql = "select count(*) from suggests";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public int countByUser(long userId) throws Exception {
        String sql = "select count(*) from suggests where user_id=?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // ✅ 관리자 전체 목록
    public List<SuggestDTO> listPaged(int size, int offset) throws Exception {
        String sql = """
            select s.id, s.user_id, u.username as writer_username,
                   s.title, s.content, s.created_at, s.updated_at,
                   count(c.id) as comment_count,
                   coalesce(sum(case when c.author_role='ADMIN' then 1 else 0 end), 0) as admin_comment_count
            from suggests s
            join users u on u.user_id = s.user_id
            left join suggest_comments c on c.suggest_id = s.id
            group by s.id, s.user_id, u.username, s.title, s.content, s.created_at, s.updated_at
            order by s.id desc
            limit ? offset ?
        """;

        List<SuggestDTO> list = new ArrayList<>();
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

    // ✅ 유저 내 글 목록
    public List<SuggestDTO> listByUserPaged(long userId, int size, int offset) throws Exception {
        String sql = """
            select s.id, s.user_id, u.username as writer_username,
                   s.title, s.content, s.created_at, s.updated_at,
                   count(c.id) as comment_count,
                   coalesce(sum(case when c.author_role='ADMIN' then 1 else 0 end), 0) as admin_comment_count
            from suggests s
            join users u on u.user_id = s.user_id
            left join suggest_comments c on c.suggest_id = s.id
            where s.user_id=?
            group by s.id, s.user_id, u.username, s.title, s.content, s.created_at, s.updated_at
            order by s.id desc
            limit ? offset ?
        """;

        List<SuggestDTO> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, size);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public SuggestDTO findById(long id) throws Exception {
        String sql = """
            select s.id, s.user_id, u.username as writer_username,
                   s.title, s.content, s.created_at, s.updated_at
            from suggests s
            join users u on u.user_id = s.user_id
            where s.id=?
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

    public void insert(long userId, String title, String content) throws Exception {
        String sql = "insert into suggests(user_id, title, content) values(?,?,?)";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, title);
            ps.setString(3, content);
            ps.executeUpdate();
        }
    }

    public void update(long id, String title, String content) throws Exception {
        String sql = "update suggests set title=?, content=?, updated_at=NOW() where id=?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, content);
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws Exception {
        String sql = "delete from suggests where id=?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private SuggestDTO map(ResultSet rs) throws Exception {
        SuggestDTO s = new SuggestDTO();
        s.setId(rs.getLong("id"));
        s.setUserId(rs.getLong("user_id"));
        s.setWriterUsername(rs.getString("writer_username"));
        s.setTitle(rs.getString("title"));
        s.setContent(rs.getString("content"));
        s.setCreatedAt(rs.getTimestamp("created_at"));
        s.setUpdatedAt(rs.getTimestamp("updated_at"));

        // ✅ 댓글 정보
        if (hasColumn(rs, "comment_count")) {
            s.setCommentCount(rs.getInt("comment_count"));
            s.setAdminCommentCount(rs.getInt("admin_comment_count"));
        }
        return s;
    }

    private boolean hasColumn(ResultSet rs, String col) {
        try {
            rs.findColumn(col);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}





