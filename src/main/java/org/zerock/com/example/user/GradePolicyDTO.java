package org.zerock.com.example.user;

import java.math.BigDecimal;

public class GradePolicyDTO {
    private String grade;
    private long minTotalSpent;
    private BigDecimal discountRate; // % (예: 3.00)
    private BigDecimal pointRate;    // % (예: 1.00)

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public long getMinTotalSpent() { return minTotalSpent; }
    public void setMinTotalSpent(long minTotalSpent) { this.minTotalSpent = minTotalSpent; }

    public BigDecimal getDiscountRate() { return discountRate; }
    public void setDiscountRate(BigDecimal discountRate) { this.discountRate = discountRate; }

    public BigDecimal getPointRate() { return pointRate; }
    public void setPointRate(BigDecimal pointRate) { this.pointRate = pointRate; }
}
