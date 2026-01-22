package org.zerock.com.example.common;

import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SuggestCommentDAO {

    public List<SuggestCommentDTO> listBySuggest(long suggestId) throws Exception {
        String sql = """
            select c.id, c.suggest_id, c.author_user_id, c.author_role,
                   u.username as author_username,
                   c.content, c.created_at
            from suggest_comments c
            join users u on u.user_id = c.author_user_id
            where c.suggest_id=?
            order by c.id asc
        """;

        List<SuggestCommentDTO> list = new ArrayList<>();

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, suggestId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SuggestCommentDTO c = new SuggestCommentDTO();
                    c.setId(rs.getLong("id"));
                    c.setSuggestId(rs.getLong("suggest_id"));
                    c.setAuthorUserId(rs.getLong("author_user_id"));
                    c.setAuthorRole(rs.getString("author_role"));
                    c.setAuthorUsername(rs.getString("author_username"));
                    c.setContent(rs.getString("content"));
                    c.setCreatedAt(rs.getTimestamp("created_at"));
                    list.add(c);
                }
            }
        }
        return list;
    }

    public void insert(long suggestId, long authorUserId, String authorRole, String content) throws Exception {
        String sql = "insert into suggest_comments(suggest_id, author_user_id, author_role, content) values(?,?,?,?)";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, suggestId);
            ps.setLong(2, authorUserId);
            ps.setString(3, authorRole);
            ps.setString(4, content);
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws Exception {
        String sql = "delete from suggest_comments where id=?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}
