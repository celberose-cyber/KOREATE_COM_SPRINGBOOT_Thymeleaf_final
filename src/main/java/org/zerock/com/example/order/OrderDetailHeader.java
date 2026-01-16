package org.zerock.com.example.order;

import java.sql.Timestamp;

public record OrderDetailHeader(
        long orderId,
        String orderNo,
        String status,
        String address,
        long totalPrice,
        Timestamp paidAt,
        Timestamp cancelRequestedAt,
        String cancelReason,
        Timestamp refundedAt,
        Timestamp confirmedAt
) {}
