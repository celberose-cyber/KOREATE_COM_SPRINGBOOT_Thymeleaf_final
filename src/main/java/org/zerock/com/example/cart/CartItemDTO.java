package org.zerock.com.example.cart;

public class CartItemDTO {

    private long cartItemId;
    private long productId;
    private String productName;
    private String thumbnailUrl;
    private long unitPrice;
    private int quantity;

    private Boolean onSale;
    private Long currentUnitPrice;
    private Boolean priceChanged;

    public Boolean getOnSale() { return onSale; }
    public void setOnSale(Boolean onSale) { this.onSale = onSale; }

    public Long getCurrentUnitPrice() { return currentUnitPrice; }
    public void setCurrentUnitPrice(Long currentUnitPrice) { this.currentUnitPrice = currentUnitPrice; }

    public Boolean getPriceChanged() { return priceChanged; }
    public void setPriceChanged(Boolean priceChanged) { this.priceChanged = priceChanged; }
    // ===== getters / setters =====

    public long getCartItemId() {return cartItemId;}

    public void setCartItemId(long cartItemId) {
        this.cartItemId = cartItemId;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public long getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(long unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
