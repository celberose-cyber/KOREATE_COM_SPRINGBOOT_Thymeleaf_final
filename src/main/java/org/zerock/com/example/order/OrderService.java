package org.zerock.com.example.order;

import org.zerock.com.example.cart.*;
import org.zerock.com.example.common.DBUtil;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class OrderService {

    private final OrderDAO orderDAO = new OrderDAO();

    public OrderResult checkout(long userId, String address, String payMethod) throws Exception {

        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);

            try {
                List<CartItemDTO> cart = new CartDAO().list(userId);
                if (cart.isEmpty()) throw new IllegalStateException("Cart is empty");

                long total = 0L;
                List<OrderItemDTO> items = new ArrayList<>();

                for (CartItemDTO c : cart) {
                    total += (long) c.getUnitPrice() * c.getQuantity();

                    OrderItemDTO it = new OrderItemDTO();
                    it.setProductId(c.getProductId());
                    it.setQuantity(c.getQuantity());
                    it.setUnitPrice(c.getUnitPrice());
                    items.add(it);
                }

                String orderNo = "OD" + System.currentTimeMillis();

                long orderId = orderDAO.createOrder(
                        con, userId, orderNo, total, address, payMethod, items
                );

                // ✅ 지금은 "결제가 성공했다"라고 가정하고 바로 PAID 처리
                boolean paid = orderDAO.markPaidIfCreated(con, orderId);
                if (!paid) throw new IllegalStateException("Order is not in CREATED state: " + orderId);

                con.commit();
                return new OrderResult(orderId, orderNo, total);

            } catch (Exception e) {
                try { con.rollback(); } catch (Exception ignore) {}
                throw e;
            } finally {
                try { con.setAutoCommit(true); } catch (Exception ignore) {}
            }
        }
    }
}
