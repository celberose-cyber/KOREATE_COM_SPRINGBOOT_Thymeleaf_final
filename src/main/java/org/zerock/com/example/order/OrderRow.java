package org.zerock.com.example.order;

import java.sql.Timestamp;
public record OrderRow(
        long orderId,
        String orderNo,
        String firstItemName,
        String firstImageUrl,
        int itemCount,
        int totalQty,
        long totalPrice,
        String status,
        java.sql.Timestamp createdAt,
        java.sql.Timestamp paidAt
) {}



