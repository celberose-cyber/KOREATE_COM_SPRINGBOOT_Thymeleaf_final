package org.zerock.com.example.order;

public class OrderResult {

    private final long orderId;
    private final String orderNo;
    private final long total;

    public OrderResult(long orderId, String orderNo, long total) {
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.total = total;
    }

    public long getOrderId() { return orderId; }
    public String getOrderNo() { return orderNo; }
    public long getTotal() { return total; }
}