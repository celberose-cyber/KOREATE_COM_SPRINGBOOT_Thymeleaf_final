package org.zerock.com.example.product;

import org.springframework.stereotype.Repository;


public class ProductDTO {
    private long id;                // ✅ DB 컬럼이 id 라고 가정 (product_id 쓰면 또 터짐)
    private String category;
    private String pcode;
    private String name;
    private long price;
    private String detailUrl;
    private String imageUrl;
    private String specText;
    private String extraText;
    private String regMonth;
    private Integer opinionCount;
    private Double rating;
    private Integer reviewCount;

    private String source;          // danawa
    private String createdAt;
    private String updatedAt;

    // --- getters/setters ---
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPcode() { return pcode; }
    public void setPcode(String pcode) { this.pcode = pcode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }

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

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
