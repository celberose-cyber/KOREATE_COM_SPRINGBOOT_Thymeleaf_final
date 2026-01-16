package org.zerock.com.example.order;

public class CheckoutSummary {
    private long itemsTotal;
    private int discountRate;
    private long discountAmount;
    private long shippingFee;
    private long finalTotal;
    private int pointRate;
    private long earnPoint;

    public long getItemsTotal() { return itemsTotal; }
    public void setItemsTotal(long itemsTotal) { this.itemsTotal = itemsTotal; }

    public int getDiscountRate() { return discountRate; }
    public void setDiscountRate(int discountRate) { this.discountRate = discountRate; }

    public long getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(long discountAmount) { this.discountAmount = discountAmount; }

    public long getShippingFee() { return shippingFee; }
    public void setShippingFee(long shippingFee) { this.shippingFee = shippingFee; }

    public long getFinalTotal() { return finalTotal; }
    public void setFinalTotal(long finalTotal) { this.finalTotal = finalTotal; }

    public int getPointRate() { return pointRate; }
    public void setPointRate(int pointRate) { this.pointRate = pointRate; }

    public long getEarnPoint() { return earnPoint; }
    public void setEarnPoint(long earnPoint) { this.earnPoint = earnPoint; }
}
