package org.zerock.com.example.order;

public class OrderItemDTO {

    private long productId;
    private int quantity;
    private long unitPrice;

    public long getProductId() {
        return productId;
    }
    public void setProductId(long productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public long getUnitPrice() {
        return unitPrice;
    }
    public void setUnitPrice(long unitPrice) {
        this.unitPrice = unitPrice;
    }
}
