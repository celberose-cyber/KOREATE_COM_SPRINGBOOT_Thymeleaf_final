package org.zerock.com.example.cart;

import org.springframework.stereotype.Repository;
import org.zerock.com.example.common.DBUtil;

import java.sql.*;
import java.util.*;

@Repository
public class CartDAO {

    public void addOrIncrease(long userId, long productId, int qty, long unitPrice) throws SQLException {
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
            ps.setInt(3, qty);
            ps.setLong(4, unitPrice);
            ps.executeUpdate();
        }
    }

    // ✅ 화면용(단독 커넥션)
    public List<CartItemDTO> list(long userId) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            return list(con, userId);
        }
    }

    // ✅ 트랜잭션용(CheckoutService에서 사용)
    public List<CartItemDTO> list(Connection con, long userId) throws SQLException {
        String sql = """
            SELECT
              c.cart_item_id AS cart_item_id,
              c.product_id   AS product_id,
              c.quantity     AS quantity,
              c.unit_price   AS unit_price,
              p.name         AS product_name,
              p.image_url    AS thumbnail_url
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

    // ✅ 결제 완료 후 비우기(트랜잭션)
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
}
