package org.zerock.com.example.order;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


@Repository
public class OrderDAO {

    /* =========================
       1) 주문 생성
       ========================= */
    public long createOrder(Connection con,
                            long userId,
                            String orderNo,
                            long totalPrice,
                            String address,
                            String payMethod,
                            List<OrderItemDTO> items) throws SQLException {

        String insertOrder = """
            INSERT INTO orders(user_id, order_no, status, total_price, address, pay_method)
            VALUES(?, ?, 'CREATED', ?, ?, ?)
        """;

        String insertItem = """
            INSERT INTO order_items(order_id, product_id, quantity, unit_price)
            VALUES(?,?,?,?)
        """;

        long orderId;

        try (PreparedStatement ps = con.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setString(2, orderNo);
            ps.setLong(3, totalPrice);
            ps.setString(4, address);
            ps.setString(5, payMethod);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Failed to get generated order_id");
                orderId = keys.getLong(1);
            }
        }

        try (PreparedStatement ps = con.prepareStatement(insertItem)) {
            for (OrderItemDTO it : items) {
                ps.setLong(1, orderId);
                ps.setLong(2, it.getProductId());
                ps.setInt(3, it.getQuantity());
                ps.setLong(4, it.getUnitPrice());
                ps.addBatch();
            }
            ps.executeBatch();
        }

        return orderId;
    }

    /* =========================
       2) 카카오 결제 준비: tid 저장 + READY 전환
       ========================= */
    public boolean markReadyWithTidIfCreated(Connection con, long orderId, String tid) throws SQLException {
        String sql = """
            UPDATE orders
               SET kakao_tid = ?,
                   status    = 'READY'
             WHERE order_id  = ?
               AND status    = 'CREATED'
        """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tid);
            ps.setLong(2, orderId);
            return ps.executeUpdate() == 1;
        }
    }

    /* =========================
       3) 결제 확정: READY/CREATED → PAID
       ========================= */
    public boolean markPaidIfReadyOrCreated(Connection con, long orderId) throws SQLException {
        String sql = """
            UPDATE orders
               SET status='PAID', paid_at=NOW()
             WHERE order_id=?
               AND status IN ('CREATED','READY')
        """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            return ps.executeUpdate() == 1;
        }
    }

    // (호환용)
    public boolean markPaidIfCreated(Connection con, long orderId) throws SQLException {
        String sql = """
            UPDATE orders
               SET status='PAID', paid_at=NOW()
             WHERE order_id=?
               AND status='CREATED'
        """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            return ps.executeUpdate() == 1;
        }
    }



    /* =========================
       5) 조회: total_price / 합산 / snapshot / 결제정보
       ========================= */
    public long getOrderTotalPrice(Connection con, long orderId) throws SQLException {
        String sql = "SELECT total_price FROM orders WHERE order_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("order not found: " + orderId);
                return rs.getLong(1);
            }
        }
    }

    // ✅ PAID만 합산
    public long totalSpentByUser(Connection con, long userId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(total_price),0) FROM orders WHERE user_id=? AND status='PAID'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public void updateSnapshotRates(Connection con,
                                    long orderId,
                                    String grade,
                                    BigDecimal discountRate,
                                    BigDecimal pointRate) throws SQLException {
        String sql = """
            UPDATE orders
               SET grade_at_purchase=?,
                   discount_rate_at_purchase=?,
                   point_rate_at_purchase=?
             WHERE order_id=?
        """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, grade);
            ps.setBigDecimal(2, discountRate);
            ps.setBigDecimal(3, pointRate);
            ps.setLong(4, orderId);
            ps.executeUpdate();
        }
    }

    // ✅ 결제 정보 조회 (orderNo + userId로 소유권 검증)
    public OrderPayInfo findPayInfoByOrderNoAndUser(Connection con, String orderNo, long userId) throws SQLException {
        String sql = """
            SELECT order_id, order_no, total_price, kakao_tid
              FROM orders
             WHERE order_no = ?
               AND user_id  = ?
        """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, orderNo);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new OrderPayInfo(
                        rs.getLong("order_id"),
                        rs.getString("order_no"),
                        rs.getLong("total_price"),
                        rs.getString("kakao_tid")
                );
            }
        }
    }



    // ✅ 결과 페이지용 조회 (orderId + userId로 소유권 검증)
    public OrderResult findResultByOrderIdAndUser(Connection con, long orderId, long userId) throws SQLException {
        String sql = """
            SELECT order_id, order_no, total_price
              FROM orders
             WHERE order_id=? AND user_id=?
        """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new OrderResult(
                        rs.getLong("order_id"),
                        rs.getString("order_no"),
                        rs.getLong("total_price")
                );
            }
        }
    }

    public List<OrderRow> listByUser(Connection con, long userId) throws SQLException {
        String sql = """
       SELECT
                                o.order_id,
                                  o.order_no,
                                  o.status,
                                  o.total_price,
                                  o.paid_at,
                                  o.created_at,
                                
                                  -- ✅ 상품 종류 수 (라인 수)
                                  (SELECT COUNT(*)
                                     FROM order_items oi
                                    WHERE oi.order_id = o.order_id) AS item_count,
                                
                                  -- ✅ 총 수량
                                  (SELECT COALESCE(SUM(oi.quantity), 0)
                                     FROM order_items oi
                                    WHERE oi.order_id = o.order_id) AS total_qty,
                                
                                  -- 대표상품명
                                  (SELECT p.name
                                     FROM order_items oi
                                     JOIN products p ON p.id = oi.product_id
                                    WHERE oi.order_id = o.order_id
                                    ORDER BY oi.order_item_id ASC
                                    LIMIT 1) AS first_item_name,
                                
                                  -- 대표상품 썸네일
                                  (SELECT p.image_url
                                     FROM order_items oi
                                     JOIN products p ON p.id = oi.product_id
                                    WHERE oi.order_id = o.order_id
                                    ORDER BY oi.order_item_id ASC
                                    LIMIT 1) AS first_image_url
                                
                                FROM orders o
                                WHERE o.user_id = ?
                                  AND o.status IN ('PAID','CONFIRMED','CANCEL_REQUESTED','REFUNDED')
                                ORDER BY o.order_id DESC;
                                
    """;
// AND o.status IN ('PAID','CONFIRMED','CANCEL_REQUESTED','REFUNDED') 를 통해 CREATED/READY/CANCELED컬럼은 보이지 않음
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<OrderRow> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new OrderRow(
                            rs.getLong("order_id"),
                            rs.getString("order_no"),
                            rs.getString("first_item_name"),
                            rs.getString("first_image_url"),
                            rs.getInt("item_count"),   // ✅ COUNT(*)
                            rs.getInt("total_qty"),    // ✅ SUM(quantity)
                            rs.getLong("total_price"),
                            rs.getString("status"),
                            rs.getTimestamp("created_at"),
                            rs.getTimestamp("paid_at")
                    ));
                }
                return out;
            }
        }
    }


    public boolean markConfirmedIfPaid(Connection con, long orderId, long userId) throws SQLException {
        String sql = """
        UPDATE orders
           SET status='CONFIRMED', confirmed_at=NOW()
         WHERE order_id=?
           AND user_id=?
           AND status='PAID'
    """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setLong(2, userId);
            return ps.executeUpdate() == 1;
        }
    }
    public boolean markCancelRequestedIfPaid(Connection con, long orderId, long userId, String reason) throws SQLException {
        String sql = """
        UPDATE orders
           SET status='CANCEL_REQUESTED',
               cancel_reason=?,
               cancel_requested_at=NOW()
         WHERE order_id=? AND user_id=?
           AND status='PAID'
    """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, reason);
            ps.setLong(2, orderId);
            ps.setLong(3, userId);
            return ps.executeUpdate() == 1;
        }
    }
    public boolean markRefundedIfCancelRequested(Connection con, long orderId) throws SQLException {
        String sql = """
        UPDATE orders
           SET status='REFUNDED', refunded_at=NOW()
         WHERE order_id=?
           AND status='CANCEL_REQUESTED'
    """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            return ps.executeUpdate() == 1;
        }
    }
    public OrderDetailHeader findOrderHeader(Connection con, long orderId, long userId) throws SQLException {
        String sql = """
    SELECT order_id, order_no, status, address,
           total_price,
           paid_at, cancel_requested_at, cancel_reason,
           refunded_at, confirmed_at
      FROM orders
     WHERE order_id=? AND user_id=?
""";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                return new OrderDetailHeader(
                        rs.getLong("order_id"),
                        rs.getString("order_no"),
                        rs.getString("status"),
                        rs.getString("address"),
                        rs.getLong("total_price"),
                        rs.getTimestamp("paid_at"),
                        rs.getTimestamp("cancel_requested_at"),
                        rs.getString("cancel_reason"),
                        rs.getTimestamp("refunded_at"),
                        rs.getTimestamp("confirmed_at")
                );

            }
        }
    }
    public List<OrderDetailItem> findOrderItems(Connection con, long orderId) throws SQLException {
        String sql = """
        SELECT p.name, p.image_url, oi.quantity, oi.unit_price
          FROM order_items oi
          JOIN products p ON p.id = oi.product_id
         WHERE oi.order_id=?
         ORDER BY oi.order_item_id ASC
    """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                List<OrderDetailItem> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new OrderDetailItem(
                            rs.getString("name"),
                            rs.getString("image_url"),
                            rs.getInt("quantity"),
                            rs.getLong("unit_price")
                    ));
                }
                return list;
            }
        }
    }

}

