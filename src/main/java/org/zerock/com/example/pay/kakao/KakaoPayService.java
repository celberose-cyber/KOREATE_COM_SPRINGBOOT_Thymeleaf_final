package org.zerock.com.example.pay.kakao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class KakaoPayService {

    private final RestTemplate rt = new RestTemplate();

    @Value("${kakaopay.base-url:https://open-api.kakaopay.com}")
    private String baseUrl;

    @Value("${kakaopay.secret-key}")
    private String secretKey;

    @Value("${kakaopay.cid:TC0ONETIME}")
    private String cid;

    // 예: http://localhost:8080  (카카오 콘솔 Web 플랫폼 도메인에 등록한 값)
    @Value("${app.base-url}")
    private String appBaseUrl;

    public KakaoReadyResponse ready(String orderNo, String userIdStr, String itemName, int quantity, long totalAmount) {
        String url = baseUrl + "/online/v1/payment/ready";

        String base = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length()-1) : appBaseUrl;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cid", cid);
        body.put("partner_order_id", orderNo);
        body.put("partner_user_id", userIdStr);
        body.put("item_name", itemName);
        body.put("quantity", Math.max(1, quantity));
        body.put("total_amount", totalAmount);
        body.put("vat_amount", 0);        // ✅ 추가
        body.put("tax_free_amount", 0);

        // ✅ 여기 URL 경로도 실제 컨트롤러 매핑이랑 반드시 일치시켜야 함(아래 3번 참고)
        body.put("approval_url", base + "/pay/kakao/approve?orderNo=" + orderNo);
        body.put("cancel_url",   base + "/pay/kakao/cancel?orderNo=" + orderNo);
        body.put("fail_url",     base + "/pay/kakao/fail?orderNo=" + orderNo);

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headersJson());
        return rt.postForObject(url, req, KakaoReadyResponse.class);
    }


    public KakaoApproveResponse approve(String tid, String orderNo, String userIdStr, String pgToken) {
        String url = baseUrl + "/online/v1/payment/approve";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cid", cid);
        body.put("tid", tid);
        body.put("partner_order_id", orderNo);
        body.put("partner_user_id", userIdStr);
        body.put("pg_token", pgToken);

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headersJson());
        return rt.postForObject(url, req, KakaoApproveResponse.class);
    }

    public KakaoCancelResponse cancel(String tid, long cancelAmount) {
        String url = baseUrl + "/online/v1/payment/cancel";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cid", cid);
        body.put("tid", tid);
        body.put("cancel_amount", cancelAmount);
        body.put("cancel_tax_free_amount", 0);

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headersJson());
        return rt.postForObject(url, req, KakaoCancelResponse.class);
    }

    private HttpHeaders headersJson() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "SECRET_KEY " + secretKey);
        return headers;
    }

    private HttpHeaders headersForm() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "SECRET_KEY " + secretKey);
        return headers;
    }
}
