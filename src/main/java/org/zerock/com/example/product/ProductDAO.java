package org.zerock.com.example.product;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.zerock.com.example.common.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ProductDAO {

    private final JdbcTemplate jt;

    public ProductDAO(JdbcTemplate jt) {
        this.jt = jt;
    }

    // ✅ 크롤러에서 사용: detail_url 기준 upsert
    public int upsertByDetailUrl(ProductDTO p) {
        String sql = """
            INSERT INTO products(
              category, name, price, detail_url, image_url, source,
              pcode, spec_text, extra_text, reg_month,
              opinion_count, rating, review_count
            )
            VALUES(?,?,?,?,?,'danawa',?,?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE
              category=VALUES(category),
              name=VALUES(name),
              price=VALUES(price),
              image_url=VALUES(image_url),
              pcode=VALUES(pcode),
              spec_text=VALUES(spec_text),
              extra_text=VALUES(extra_text),
              reg_month=VALUES(reg_month),
              opinion_count=VALUES(opinion_count),
              rating=VALUES(rating),
              review_count=VALUES(review_count),
              updated_at=CURRENT_TIMESTAMP
        """;

        // ✅ 여기서 “Parameter at position 13 is not set” 터지는 원인은
        // ? 개수와 파라미터 개수가 안 맞아서입니다. 아래는 정확히 12개만 전달합니다.
        return jt.update(sql,
                p.getCategory(),          // 1
                p.getName(),              // 2
                p.getPrice(),             // 3
                p.getDetailUrl(),         // 4
                p.getImageUrl(),          // 5
                p.getPcode(),             // 6
                p.getSpecText(),          // 7
                p.getExtraText(),         // 8
                p.getRegMonth(),          // 9
                p.getOpinionCount(),      // 10
                p.getRating(),            // 11
                p.getReviewCount()        // 12
        );
    }

    // ✅ 상품목록 조회
    public List<ProductDTO> listByCategory(String category, int limit) {
        String sql = """
            SELECT
              id, category, pcode, name, price,
              detail_url, image_url,
              spec_text, extra_text, reg_month,
              opinion_count, rating, review_count,
              source, created_at, updated_at
            FROM products
            WHERE category = ?
            ORDER BY updated_at DESC, id DESC
            LIMIT ?
        """;

        return jt.query(sql, mapper(), category, limit);
    }
    public List<ProductDTO> listByCategoryPaged(String category, String q, String sort, int size, int offset) {

        String orderBy = switch (sort) {
            case "priceAsc"  -> "price ASC";
            case "priceDesc" -> "price DESC";
            case "rating"    -> "rating DESC";
            case "review"    -> "review_count DESC";
            case "new"       -> "created_at DESC";
            default          -> "updated_at DESC, id DESC";
        };

        boolean hasQ = (q != null && !q.isBlank());

        String baseSql = """
        SELECT
          id, category, pcode, name, price,
          detail_url, image_url,
          spec_text, extra_text, reg_month,
          opinion_count, rating, review_count,
          source, created_at, updated_at
        FROM products
        WHERE category = ?
    """;

        String sql = baseSql
                + (hasQ ? " AND name LIKE CONCAT('%', ?, '%') " : " ")
                + " ORDER BY " + orderBy
                + " LIMIT ? OFFSET ?";

        if (hasQ) {
            return jt.query(sql, mapper(), category, q, size, offset);
        }
        return jt.query(sql, mapper(), category, size, offset);
    }



    // (선택) 전체 삭제
    public int deleteAll() {
        return jt.update("DELETE FROM products");
    }

    public List<ProductDTO> searchByName(String keyword, String sort, int limit) {

        String orderBy = switch (sort) {
            case "priceAsc"  -> "price ASC";
            case "priceDesc" -> "price DESC";
            case "rating"    -> "rating DESC";
            case "review"    -> "review_count DESC";
            case "new"       -> "created_at DESC";
            default          -> "created_at DESC";
        };

        String sql =
                "SELECT id, category, pcode, name, price, detail_url, image_url, " +
                        "spec_text, extra_text, reg_month, opinion_count, rating, review_count, " +
                        "source, created_at, updated_at " +
                        "FROM products " +
                        "WHERE name LIKE CONCAT('%', ?, '%') " +
                        "ORDER BY " + orderBy + " " +
                        "LIMIT ?";

        return jt.query(sql, mapper(), keyword, limit);
    }
    public List<ProductDTO> searchByName(String keyword, String sort) {
        return searchByName(keyword, sort, 1000);
    }

    private RowMapper<ProductDTO> mapper() {
        return (rs, rowNum) -> {
            ProductDTO p = new ProductDTO();

            // ✅ 컬럼명: product_id 말고 id 로 읽어야 함!
            p.setId(rs.getLong("id"));

            p.setCategory(rs.getString("category"));
            p.setPcode(rs.getString("pcode"));
            p.setName(rs.getString("name"));
            Long price = rs.getObject("price", Long.class);
            p.setPrice(price);

            p.setDetailUrl(rs.getString("detail_url"));
            p.setImageUrl(rs.getString("image_url"));

            p.setSpecText(rs.getString("spec_text"));
            p.setExtraText(rs.getString("extra_text"));
            p.setRegMonth(rs.getString("reg_month"));

            int oc = rs.getInt("opinion_count");
            p.setOpinionCount(rs.wasNull() ? null : oc);

            double rating = rs.getDouble("rating");
            p.setRating(rs.wasNull() ? null : rating);

            int rc = rs.getInt("review_count");
            p.setReviewCount(rs.wasNull() ? null : rc);

            p.setSource(rs.getString("source"));
            p.setCreatedAt(rs.getString("created_at"));
            p.setUpdatedAt(rs.getString("updated_at"));

            return p;
        };
    }
    public ProductDTO findById(long id) {
        String sql = """
        SELECT
          id, category, pcode, name, price,
          detail_url, image_url,
          spec_text, extra_text, reg_month,
          opinion_count, rating, review_count,
          source, created_at, updated_at
        FROM products
        WHERE id = ?
    """;

        return jt.queryForObject(sql, mapper(), id);
    }
    public int countByCategory(String category, String q) {
        boolean hasQ = (q != null && !q.isBlank());

        String sql = "SELECT COUNT(*) FROM products WHERE category = ?"
                + (hasQ ? " AND name LIKE CONCAT('%', ?, '%')" : "");

        if (hasQ) {
            return jt.queryForObject(sql, Integer.class, category, q);
        }
        return jt.queryForObject(sql, Integer.class, category);
    }

}
