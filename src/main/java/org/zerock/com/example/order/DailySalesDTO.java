package org.zerock.com.example.order;

public class DailySalesDTO {

    private String day;        // yyyy-MM-dd
    private int orderCount;    // 전체 주문 건수(상태 포함)
    private int paidCount;     // PAID / CONFIRMED 건수
    private long paidAmount;   // PAID / CONFIRMED 매출 합계

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }

    public int getPaidCount() {
        return paidCount;
    }

    public void setPaidCount(int paidCount) {
        this.paidCount = paidCount;
    }

    public long getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(long paidAmount) {
        this.paidAmount = paidAmount;
    }
}
