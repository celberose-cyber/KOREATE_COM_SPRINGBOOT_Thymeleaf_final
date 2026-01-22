package org.zerock.com.example.product;

public class ProductDTO {

    private long id;
    private String category;
    private String pcode;
    private String name;
    private Long price;

    private String detailUrl;
    private String imageUrl;

    private String specText;
    private String extraText;
    private String regMonth;

    private Integer opinionCount;
    private Double rating;
    private Integer reviewCount;

    private long purchaseCount;

    private String source;
    private String createdAt;
    private String updatedAt;

    // 기존
    private boolean onSale;      // 관리자 토글/표시용
    private Long salePrice;

    // ✅ 기간 세일
    private String saleStartAt;  // DATETIME → String (현재 방식 유지)
    private String saleEndAt;
    private boolean saleNow;     // DB 계산 컬럼

    /* ================= getters / setters ================= */

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPcode() { return pcode; }
    public void setPcode(String pcode) { this.pcode = pcode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }

    public String getDetailUrl() { return detailUrl; }
    public void setDetailUrl(String detailUrl) { this.detailUrl = detailUrl; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getSpecText() { return specText; }
    public void setSpecText(String specText) { this.specText = specText; }

    public String getExtraText() { return extraText; }
    public void setExtraText(String extraText) { this.extraText = extraText; }

    public String getRegMonth() { return regMonth; }
    public void setRegMonth(String regMonth) { this.regMonth = regMonth; }

    public Integer getOpinionCount() { return opinionCount; }
    public void setOpinionCount(Integer opinionCount) { this.opinionCount = opinionCount; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

    public long getPurchaseCount() { return purchaseCount; }
    public void setPurchaseCount(long purchaseCount) { this.purchaseCount = purchaseCount; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public boolean isOnSale() { return onSale; }
    public void setOnSale(boolean onSale) { this.onSale = onSale; }

    public Long getSalePrice() { return salePrice; }
    public void setSalePrice(Long salePrice) { this.salePrice = salePrice; }

    public String getSaleStartAt() { return saleStartAt; }
    public void setSaleStartAt(String saleStartAt) { this.saleStartAt = saleStartAt; }

    public String getSaleEndAt() { return saleEndAt; }
    public void setSaleEndAt(String saleEndAt) { this.saleEndAt = saleEndAt; }

    public boolean isSaleNow() { return saleNow; }
    public void setSaleNow(boolean saleNow) { this.saleNow = saleNow; }

    /* ================= 비즈니스 로직 ================= */

    /** ✅ 최종 적용 단가 */
    public long getEffectivePrice() {
        long base = (price == null ? 0L : price);
        if (saleNow && salePrice != null && salePrice > 0) {
            return salePrice;
        }
        return base;
    }
}
