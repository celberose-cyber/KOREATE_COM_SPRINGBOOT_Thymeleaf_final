package org.zerock.com.example.pay.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KakaoReadyResponse {
    private String tid;

    @JsonProperty("next_redirect_pc_url")
    private String nextRedirectPcUrl;

    @JsonProperty("created_at")
    private String createdAt;

    public String getTid() { return tid; }
    public void setTid(String tid) { this.tid = tid; }

    public String getNextRedirectPcUrl() { return nextRedirectPcUrl; }
    public void setNextRedirectPcUrl(String nextRedirectPcUrl) { this.nextRedirectPcUrl = nextRedirectPcUrl; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
