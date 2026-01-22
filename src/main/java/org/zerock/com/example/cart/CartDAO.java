package org.zerock.com.example.cart;

import org.springframework.stereotype.Repository;
import org.zerock.com.example.common.DBUtil;

import java.sql.*;
import java.util.*;

@Repository
public class CartDAO {

    public void addOrIncrease(long userId, long productId, int qty, long unitPrice) throws SQLException {
        int safeQty = Math.max(1, qty);
        long safePrice = Math.max(0, unitPrice);

        String sql = """
            INSERT INTO cart_items(user_id, product_id, quantity, unit_price)
            VALUES(?,?,?,?)
            ON DUPLICATE KEY UPDATE
              quantity = quantity + VALUES(quantity),
              unit_price = VALUES(unit_price)
        """;

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, productId);
            ps.setInt(3, safeQty);
            ps.setLong(4, safePrice);
            ps.executeUpdate();
        }
    }

    public List<CartItemDTO> list(long userId) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            return list(con, userId);
        }
    }

    public List<CartItemDTO> list(Connection con, long userId) throws SQLException {
        String sql = """
    SELECT
      c.cart_item_id AS cart_item_id,
      c.product_id   AS product_id,
      c.quantity     AS quantity,
      c.unit_price   AS unit_price,          -- 스냅샷(담을 때 단가)
      p.name         AS product_name,
      p.image_url    AS thumbnail_url,

      p.on_sale      AS on_sale,
      p.sale_price   AS sale_price,
      p.price        AS price,

      CASE
        WHEN p.on_sale = 1 AND p.sale_price IS NOT NULL AND p.sale_price > 0
          THEN p.sale_price
        ELSE p.price
      END AS current_unit_price

    FROM cart_items c
    JOIN products p ON p.id = c.product_id
    WHERE c.user_id = ?
    ORDER BY c.cart_item_id DESC
""";


        List<CartItemDTO> out = new ArrayList<>();

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CartItemDTO dto = new CartItemDTO();
                    dto.setCartItemId(rs.getLong("cart_item_id"));
                    dto.setProductId(rs.getLong("product_id"));
                    dto.setQuantity(rs.getInt("quantity"));
                    dto.setUnitPrice(rs.getLong("unit_price"));
                    dto.setProductName(rs.getString("product_name"));
                    dto.setThumbnailUrl(rs.getString("thumbnail_url"));
                    // ---- 추가: 현재가/세일 여부/변동 ----
                    Integer onSaleInt = rs.getObject("on_sale", Integer.class);
                    boolean onSale = (onSaleInt != null && onSaleInt == 1);
                    dto.setOnSale(onSale);

                    Long current = rs.getObject("current_unit_price", Long.class);
                    // price가 null일 가능성 대비: null이면 스냅샷 단가 사용
                    dto.setCurrentUnitPrice(current != null ? current : dto.getUnitPrice());

                    long snap = dto.getUnitPrice();
                    long cur  = dto.getCurrentUnitPrice() != null ? dto.getCurrentUnitPrice() : snap;
                    dto.setPriceChanged(cur != snap);
                    out.add(dto);
                }
            }
        }
        return out;
    }

    public void updateQty(long userId, long cartItemId, int qty) throws SQLException {
        if (qty <= 0) { delete(userId, cartItemId); return; }

        String sql = "UPDATE cart_items SET quantity=? WHERE cart_item_id=? AND user_id=?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setLong(2, cartItemId);
            ps.setLong(3, userId);
            ps.executeUpdate();
        }
    }

    public void delete(long userId, long cartItemId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE cart_item_id=? AND user_id=?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, cartItemId);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    public int clear(Connection con, long userId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE user_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            return ps.executeUpdate();
        }
    }

    public void clear(long userId) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            clear(con, userId);
        }
    }
    public CartItemDTO findByCartItemId(long userId, long cartItemId) throws SQLException {
        String sql = """
      SELECT cart_item_id, product_id, quantity, unit_price
      FROM cart_items
      WHERE user_id=? AND cart_item_id=?
    """;
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, cartItemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                CartItemDTO dto = new CartItemDTO();
                dto.setCartItemId(rs.getLong("cart_item_id"));
                dto.setProductId(rs.getLong("product_id"));
                dto.setQuantity(rs.getInt("quantity"));
                dto.setUnitPrice(rs.getLong("unit_price"));
                return dto;
            }
        }
    }

    public void updateUnitPrice(long userId, long cartItemId, long unitPrice) throws SQLException {
        String sql = "UPDATE cart_items SET unit_price=? WHERE user_id=? AND cart_item_id=?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, Math.max(0, unitPrice));
            ps.setLong(2, userId);
            ps.setLong(3, cartItemId);
            ps.executeUpdate();
        }
    }

}
