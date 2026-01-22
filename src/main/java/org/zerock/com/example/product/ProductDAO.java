package org.zerock.com.example.product;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.zerock.com.example.common.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.sql.Timestamp;

@Repository
public class ProductDAO {

    private final JdbcTemplate jt;

    public ProductDAO(JdbcTemplate jt) {
        this.jt = jt;
    }

    // ✅ 공통: sale_now 계산식 (SELECT마다 동일하게 쓰려고 분리)
    private static final String SALE_NOW_EXPR = """
      CASE
        WHEN sale_price IS NOT NULL
         AND sale_start_at IS NOT NULL
         AND sale_end_at IS NOT NULL
         AND NOW() BETWEEN sale_start_at AND sale_end_at
        THEN 1 ELSE 0
      END AS sale_now
    """;

    // ✅ 공통: saleOnly 필터 (on_sale가 아니라 "실제 세일중(sale_now)" 기준)
    private static final String SALE_ONLY_WHERE = """
      AND sale_price IS NOT NULL
      AND sale_start_at IS NOT NULL
      AND sale_end_at IS NOT NULL
      AND NOW() BETWEEN sale_start_at AND sale_end_at
    """;

    // ✅ 크롤러 upsert (세일컬럼은 건드리지 않음: 기존 유지)
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

        return jt.update(sql,
                p.getCategory(),     // 1
                p.getName(),         // 2
                p.getPrice(),        // 3
                p.getDetailUrl(),    // 4
                p.getImageUrl(),     // 5
                p.getPcode(),        // 6
                p.getSpecText(),     // 7
                p.getExtraText(),    // 8
                p.getRegMonth(),     // 9
                p.getOpinionCount(), // 10
                p.getRating(),       // 11
                p.getReviewCount()   // 12
        );
    }

    // ✅ (쇼핑몰) 카테고리 목록
    public List<ProductDTO> listByCategory(String category, int limit) {
        String sql = """
        SELECT
          id, category, pcode, name, price,
          sale_price, on_sale,
          sale_start_at, sale_end_at,
        """ + SALE_NOW_EXPR + """
          ,
          detail_url, image_url,
          spec_text, extra_text, reg_month,
          opinion_count, rating, review_count,
          purchase_count,
          source, created_at, updated_at
        FROM products
        WHERE category = ?
        ORDER BY updated_at DESC, id DESC
        LIMIT ?
        """;

        return jt.query(sql, mapper(), category, limit);
    }

    // ✅ (쇼핑몰) 카테고리 + 검색 + 정렬 + 세일중만 + 페이징
    public List<ProductDTO> listByCategoryPaged(
            String category, String q, String sort,
            boolean saleOnly, int size, int offset) {

        String orderBy = switch (sort) {
            case "priceAsc"  -> "price ASC";
            case "priceDesc" -> "price DESC";
            case "rating"    -> "rating DESC";
            case "review"    -> "review_count DESC";
            case "new"       -> "created_at DESC";
            default          -> "updated_at DESC, id DESC";
        };

        boolean hasQ = (q != null && !q.isBlank());

        String sql = """
        SELECT
          id, category, pcode, name, price,
          sale_price, on_sale,
          sale_start_at, sale_end_at,
        """ + SALE_NOW_EXPR + """
          ,
          detail_url, image_url,
          spec_text, extra_text, reg_month,
          opinion_count, rating, review_count,
          purchase_count,
          source, created_at, updated_at
        FROM products
        WHERE category = ?
        """
                + (hasQ ? " AND name LIKE CONCAT('%', ?, '%') " : "")
                + (saleOnly ? ("\n" + SALE_ONLY_WHERE + "\n") : "")
                + " ORDER BY " + orderBy
                + " LIMIT ? OFFSET ?";

        if (hasQ) return jt.query(sql, mapper(), category, q, size, offset);
        return jt.query(sql, mapper(), category, size, offset);
    }

    // ✅ (쇼핑몰) 검색
    public List<ProductDTO> searchByName(String keyword, String sort, int limit) {
        String orderBy = switch (sort) {
            case "priceAsc"  -> "price ASC";
            case "priceDesc" -> "price DESC";
            case "rating"    -> "rating DESC";
            case "review"    -> "review_count DESC";
            case "new"       -> "created_at DESC";
            default          -> "created_at DESC";
        };

        String sql = """
        SELECT
          id, category, pcode, name, price,
          sale_price, on_sale,
          sale_start_at, sale_end_at,
        """ + SALE_NOW_EXPR + """
          ,
          detail_url, image_url,
          spec_text, extra_text, reg_month,
          opinion_count, rating, review_count,
          purchase_count,
          source, created_at, updated_at
        FROM products
        WHERE name LIKE CONCAT('%', ?, '%')
        ORDER BY """ + orderBy + """
        LIMIT ?
        """;

        return jt.query(sql, mapper(), keyword, limit);
    }

    public List<ProductDTO> searchByName(String keyword, String sort) {
        return searchByName(keyword, sort, 1000);
    }

    // ✅ (공용) 단건
    public ProductDTO findById(long id) {
        String sql = """
        SELECT
          id, category, pcode, name, price,
          sale_price, on_sale,
          sale_start_at, sale_end_at,
        """ + SALE_NOW_EXPR + """
          ,
          detail_url, image_url,
          spec_text, extra_text, reg_month,
          opinion_count, rating, review_count,
          purchase_count,
          source, created_at, updated_at
        FROM products
        WHERE id = ?
        """;

        return jt.queryForObject(sql, mapper(), id);
    }

    // ✅ (쇼핑몰/관리자 공용) 카테고리 카운트
    public int countByCategory(String category, String q, boolean saleOnly) {
        boolean hasQ = (q != null && !q.isBlank());

        String sql = """
        SELECT COUNT(*) FROM products
        WHERE category = ?
        """
                + (hasQ ? " AND name LIKE CONCAT('%', ?, '%') " : "")
                + (saleOnly ? ("\n" + SALE_ONLY_WHERE + "\n") : "");

        if (hasQ) return jt.queryForObject(sql, Integer.class, category, q);
        return jt.queryForObject(sql, Integer.class, category);
    }

    // ✅ (관리자) 전체 카운트 (all 포함)
    public int countAll(String category, String q, boolean saleOnly) {
        boolean hasCat = (category != null && !"all".equalsIgnoreCase(category));
        boolean hasQ   = (q != null && !q.isBlank());

        String sql = "SELECT COUNT(*) FROM products WHERE 1=1"
                + (hasCat ? " AND category = ? " : "")
                + (hasQ   ? " AND name LIKE CONCAT('%', ?, '%') " : "")
                + (saleOnly ? ("\n" + SALE_ONLY_WHERE + "\n") : "");

        if (hasCat && hasQ) return jt.queryForObject(sql, Integer.class, category, q);
        if (hasCat)         return jt.queryForObject(sql, Integer.class, category);
        if (hasQ)           return jt.queryForObject(sql, Integer.class, q);
        return jt.queryForObject(sql, Integer.class);
    }

    // ✅ (관리자) 전체 목록 페이징
    public List<ProductDTO> listAllPaged(
            String category, String q, String sort,
            boolean saleOnly, int size, int offset) {

        String orderBy = switch (sort) {
            case "priceAsc"  -> "price ASC";
            case "priceDesc" -> "price DESC";
            case "rating"    -> "rating DESC";
            case "review"    -> "review_count DESC";
            case "new"       -> "created_at DESC";
            case "purchase"  -> "purchase_count DESC";
            default          -> "updated_at DESC, id DESC";
        };

        boolean hasCat = (category != null && !"all".equalsIgnoreCase(category));
        boolean hasQ   = (q != null && !q.isBlank());

        String sql = """
        SELECT
          id, category, pcode, name, price,
          sale_price, on_sale,
          sale_start_at, sale_end_at,
        """ + SALE_NOW_EXPR + """
          ,
          detail_url, image_url,
          spec_text, extra_text, reg_month,
          opinion_count, rating, review_count,
          purchase_count,
          source, created_at, updated_at
        FROM products
        WHERE 1=1
        """
                + (hasCat ? " AND category = ? " : "")
                + (hasQ   ? " AND name LIKE CONCAT('%', ?, '%') " : "")
                + (saleOnly ? ("\n" + SALE_ONLY_WHERE + "\n") : "")
                + " ORDER BY " + orderBy
                + " LIMIT ? OFFSET ?";

        if (hasCat && hasQ) return jt.query(sql, mapper(), category, q, size, offset);
        if (hasCat)         return jt.query(sql, mapper(), category, size, offset);
        if (hasQ)           return jt.query(sql, mapper(), q, size, offset);
        return jt.query(sql, mapper(), size, offset);
    }

    // (선택) 전체 삭제
    public int deleteAll() {
        return jt.update("DELETE FROM products");
    }

    public boolean deleteById(long id) throws Exception {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    // ✅ 유지 (요청하신대로 그대로)
    public void updatePrices(long id, long price, Long salePrice) {
        String sql = "UPDATE products SET price = ?, sale_price = ? WHERE id = ?";
        jt.update(sql, price, salePrice, id);
    }

    // ✅ 유지 (요청하신대로 그대로)
    public void updateSaleStatus(long id, boolean onSale, Long salePrice) {
        if (salePrice == null) {
            String sql = "UPDATE products SET on_sale = ? WHERE id = ?";
            jt.update(sql, onSale ? 1 : 0, id);
        } else {
            String sql = "UPDATE products SET on_sale = ?, sale_price = ? WHERE id = ?";
            jt.update(sql, onSale ? 1 : 0, salePrice, id);
        }
    }

    // 구매횟수 증가 (기존 유지)
    public void increasePurchaseCount(Connection con, long productId, int qty) throws Exception {
        String sql = "UPDATE products SET purchase_count = purchase_count + ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, qty));
            ps.setLong(2, productId);
            ps.executeUpdate();
        }
    }

    // ✅ mapper: 이제 모든 SELECT가 동일 컬럼을 주므로 안정
    private RowMapper<ProductDTO> mapper() {
        return (rs, rowNum) -> {
            ProductDTO p = new ProductDTO();

            p.setId(rs.getLong("id"));
            p.setCategory(rs.getString("category"));
            p.setPcode(rs.getString("pcode"));
            p.setName(rs.getString("name"));

            p.setPrice(rs.getObject("price", Long.class));
            p.setSalePrice(rs.getObject("sale_price", Long.class));

            Integer onSale = rs.getObject("on_sale", Integer.class);
            p.setOnSale(onSale != null && onSale == 1);

            p.setSaleStartAt(rs.getString("sale_start_at"));
            p.setSaleEndAt(rs.getString("sale_end_at"));

            Integer saleNow = rs.getObject("sale_now", Integer.class);
            p.setSaleNow(saleNow != null && saleNow == 1);

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

            Long pc = rs.getObject("purchase_count", Long.class);
            p.setPurchaseCount(pc == null ? 0L : pc);

            p.setSource(rs.getString("source"));
            p.setCreatedAt(rs.getString("created_at"));
            p.setUpdatedAt(rs.getString("updated_at"));

            return p;
        };
    }
    // ProductDAO 안에 추가
    public int applyPercentSale(List<Long> ids, int percent, LocalDateTime start, LocalDateTime end) {

        // ids -> "?, ?, ?, ..."
        String inSql = ids.stream().map(x -> "?").collect(Collectors.joining(","));

        // ✅ sale_price 계산을 DB에서 수행 (percent는 공통)
        // CAST(ROUND(... ) AS SIGNED) : BIGINT에 깔끔히 들어가게
        String sql = """
        UPDATE products
        SET
          on_sale = 1,
          sale_start_at = ?,
          sale_end_at   = ?,
          sale_price = CASE
            WHEN price IS NULL THEN NULL
            ELSE CAST(ROUND(price * (100 - ?) / 100.0) AS SIGNED)
          END
        WHERE id IN (""" + inSql + ")";

        // 파라미터: start, end, percent, ids...
        Object[] params = new Object[3 + ids.size()];
        params[0] = Timestamp.valueOf(start);
        params[1] = Timestamp.valueOf(end);
        params[2] = percent;

        for (int i = 0; i < ids.size(); i++) {
            params[3 + i] = ids.get(i);
        }

        return jt.update(sql, params);
    }

    public int clearSalePeriod(List<Long> ids) {

    if (ids == null || ids.isEmpty()) return 0;

    String inSql = ids.stream().map(x -> "?").collect(Collectors.joining(","));

    String sql = """
        UPDATE products
        SET
          on_sale = 0,
          sale_price = NULL,
          sale_start_at = NULL,
          sale_end_at   = NULL
        WHERE id IN (""" + inSql + ")";

    Object[] params = new Object[ids.size()];
    for (int i = 0; i < ids.size(); i++) {
        params[i] = ids.get(i);
    }

    return jt.update(sql, params);
}

}
