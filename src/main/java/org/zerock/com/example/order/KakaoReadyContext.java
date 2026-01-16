package org.zerock.com.example.order;

public record KakaoReadyContext(
        long orderId,
        String orderNo,
        String itemName,
        int quantity,
        long totalAmount
) {}
