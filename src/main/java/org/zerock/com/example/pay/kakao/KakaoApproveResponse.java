package org.zerock.com.example.pay.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KakaoApproveResponse {
    private String tid;

    @JsonProperty("partner_order_id")
    private String partnerOrderId;

    @JsonProperty("partner_user_id")
    private String partnerUserId;

    private Amount amount;

    public static class Amount {
        private Long total;
        public Long getTotal() { return total; }
        public void setTotal(Long total) { this.total = total; }
    }

    public String getTid() { return tid; }
    public void setTid(String tid) { this.tid = tid; }

    public String getPartnerOrderId() { return partnerOrderId; }
    public void setPartnerOrderId(String partnerOrderId) { this.partnerOrderId = partnerOrderId; }

    public String getPartnerUserId() { return partnerUserId; }
    public void setPartnerUserId(String partnerUserId) { this.partnerUserId = partnerUserId; }

    public Amount getAmount() { return amount; }
    public void setAmount(Amount amount) { this.amount = amount; }
}
