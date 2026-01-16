// DTO 하나 만들기: org.zerock.com.example.order.OrderPayInfo
package org.zerock.com.example.order;

public class OrderPayInfo {
    private long orderId;
    private String orderNo;
    private long totalPrice;
    private String kakaoTid;

    public OrderPayInfo(long orderId, String orderNo, long totalPrice, String kakaoTid) {
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.totalPrice = totalPrice;
        this.kakaoTid = kakaoTid;
    }
    public long getOrderId() { return orderId; }
    public String getOrderNo() { return orderNo; }
    public long getTotalPrice() { return totalPrice; }
    public String getKakaoTid() { return kakaoTid; }
}
