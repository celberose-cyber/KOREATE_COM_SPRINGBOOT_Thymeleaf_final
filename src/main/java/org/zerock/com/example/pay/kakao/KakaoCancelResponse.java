package org.zerock.com.example.pay.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KakaoCancelResponse {
    private String tid;

    @JsonProperty("status")
    private String status;

    public String getTid() { return tid; }
    public void setTid(String tid) { this.tid = tid; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
