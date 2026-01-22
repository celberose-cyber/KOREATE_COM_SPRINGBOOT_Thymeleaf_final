package org.zerock.com.example.cart;

import org.zerock.com.example.user.GradePolicyDTO;

public class CartSummaryDTO {

    private final long total;             // 원가 합계
    private final long discountAmount;    // 할인 금액
    private final long discountedTotal;   // 할인 적용 합계
    private final long expectedPoint;     // 예상 적립
    private final GradePolicyDTO policy;  // 정책(등급)

    public CartSummaryDTO(long total, long discountAmount, long discountedTotal,
                          long expectedPoint, GradePolicyDTO policy) {
        this.total = total;
        this.discountAmount = discountAmount;
        this.discountedTotal = discountedTotal;
        this.expectedPoint = expectedPoint;
        this.policy = policy;
    }

    public long getTotal() { return total; }
    public long getDiscountAmount() { return discountAmount; }
    public long getDiscountedTotal() { return discountedTotal; }
    public long getExpectedPoint() { return expectedPoint; }
    public GradePolicyDTO getPolicy() { return policy; }
}
