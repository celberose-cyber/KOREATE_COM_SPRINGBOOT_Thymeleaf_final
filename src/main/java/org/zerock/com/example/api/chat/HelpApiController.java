package org.zerock.com.example.api.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.zerock.com.example.product.ProductDAO;
import org.zerock.com.example.product.ProductDTO;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/help")
public class HelpApiController {

    private final ProductDAO productDAO;

    public HelpApiController(ProductDAO productDAO) {
        this.productDAO = productDAO;
    }
    @Value("${shop.llm.url:http://localhost:11434/api/chat}")
    private String ollamaChatUrl;

    @Value("${shop.llm.model:qwen2.5:7b}")
    private String ollamaModel;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final ObjectMapper om = new ObjectMapper();
    private final ExecutorService ssePool = Executors.newCachedThreadPool();

    // ✅ SSE는 text/event-stream
    @GetMapping(value = "/say/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sayStream(@RequestParam String intent,
                                @RequestParam(required = false) String category) {

        // SSE 타임아웃(원하면 늘리세요)
        SseEmitter emitter = new SseEmitter(60_000L);

        String system = """
        You are a Korean customer 안내 문구 생성기.
        - 출력은 딱 1문장 (최대 25자~45자)
        - 존댓말
        - 이모지는 있어도 1개까지만
        - 과장/허위 금지
        """;

        String user = "intent=" + intent + ", category=" + (category == null ? "" : category);

        // ✅ Ollama 요청 바디 (stream:true)
        final String bodyJson;
        try {
            bodyJson = """
            {
              "model": "%s",
              "stream": true,
              "messages": [
                {"role":"system","content":%s},
                {"role":"user","content":%s}
              ]
            }
            """.formatted(
                    escapeJson(ollamaModel),
                    om.writeValueAsString(system),
                    om.writeValueAsString(user)
            );
        } catch (Exception e) {
            safeSend(emitter, "error", "body build failed");
            emitter.complete();
            return emitter;
        }

        // ✅ 비동기로 스트리밍 릴레이
        ssePool.submit(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(ollamaChatUrl))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(60))
                        .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<java.io.InputStream> resp =
                        http.send(req, HttpResponse.BodyHandlers.ofInputStream());

                if (resp.statusCode() != 200) {
                    safeSend(emitter, "error", "ollama http " + resp.statusCode());
                    emitter.complete();
                    return;
                }

                // Ollama streaming은 "한 줄에 JSON 하나" (NDJSON) 형태로 옴
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {

                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) continue;

                        JsonNode root = om.readTree(line);

                        // ✅ Ollama /api/chat 스트리밍에서 텍스트 조각
                        String delta = root.path("message").path("content").asText("");

                        // done=true면 종료
                        boolean done = root.path("done").asBoolean(false);

                        if (!delta.isEmpty()) {
                            // event: token, data: "..."
                            safeSend(emitter, "token", delta);
                        }

                        if (done) break;
                    }
                }

                safeSend(emitter, "done", "[DONE]");
                emitter.complete();

            } catch (Exception ex) {
                safeSend(emitter, "error", ex.getClass().getSimpleName() + ": " + ex.getMessage());
                emitter.completeWithError(ex);
            }
        });

        // 클라이언트가 끊으면 서버도 정리
        emitter.onCompletion(() -> {});
        emitter.onTimeout(() -> emitter.complete());

        return emitter;
    }

    private static void safeSend(SseEmitter emitter, String event, String data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (Exception ignore) {
            // 클라이언트가 이미 닫았으면 여기서 예외가 날 수 있음
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");

    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @GetMapping(value="/top10", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> top10(
            @RequestParam(defaultValue = "rating") String sort,
            @RequestParam(defaultValue = "all") String category
    ) {
        int size = 10;
        String q = ""; // TOP10은 검색어 없음

        if (category == null || category.isBlank()) {
            category = "all";
        }

        // ✅ 프론트 sort 값 → DAO sort 값 매핑
        String sortParam = switch (sort) {
            // 버튼/기존 UI 호환
            case "price" -> "priceDesc";

            // 채팅 action / 신규 파라미터
            case "price_desc", "priceDesc" -> "priceDesc";
            case "price_asc",  "priceAsc"  -> "priceAsc";

            case "reviews", "review" -> "review";
            case "new"              -> "new";
            case "rating"           -> "rating";

            // 모르는 값은 다 rating으로 고정(안전)
            default -> "rating";
        };
        List<ProductDTO> list;

        // ✅ 핵심 분기
        if ("all".equalsIgnoreCase(category)) {
            // 🔥 전체 상품 TOP10
            list = productDAO.searchByName("", sortParam, size);
        } else {
            // 🔥 카테고리별 TOP10
            list = productDAO.listByCategoryPaged(category, q, sortParam, size, 0);
        }

        List<Map<String, Object>> items = new ArrayList<>();

        for (ProductDTO p : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("price", p.getPrice());          // ✅ null OK
            m.put("rating", p.getRating());
            m.put("reviewCount", p.getReviewCount());
            m.put("imageUrl", p.getImageUrl());
            m.put("detailUrl", p.getDetailUrl());
            items.add(m);
        }


        return Map.of(
                "sort", sortParam,
                "category", category,
                "items", items
        );
    }

    @GetMapping(value="/intro", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> intro() {
        return Map.of(
                "text", "저희 매장은 컴퓨터(본체), 모니터, 마우스, 키보드, 스피커를 취급합니다.",
                "categories", List.of(
                        Map.of("label","컴퓨터(본체)","slug","computer"),
                        Map.of("label","모니터","slug","monitor"),
                        Map.of("label","마우스","slug","mouse"),
                        Map.of("label","키보드","slug","keyboard"),
                        Map.of("label","스피커","slug","speaker")
                )
        );
    }

    @GetMapping(value="/policy", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> policy() {
        return Map.of(
                "text", "회원 등급은 누적 결제 금액 기준으로 적용되며, 등급별 할인/적립 혜택이 있습니다.",
                "policies", List.of(
                        Map.of("grade","BRONZE","minTotalSpent",0,"discountRate",0.0,"pointRate",1.0),
                        Map.of("grade","SILVER","minTotalSpent",100000,"discountRate",2.0,"pointRate",2.0),
                        Map.of("grade","GOLD","minTotalSpent",300000,"discountRate",4.0,"pointRate",3.0),
                        Map.of("grade","PLATINUM","minTotalSpent",1000000,"discountRate",6.0,"pointRate",4.0),
                        Map.of("grade","DIAMOND","minTotalSpent",1500000,"discountRate",8.0,"pointRate",5.0)
                )
        );
    }

    @GetMapping(value="/pay", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> pay() {
        return Map.of(
                "methods", List.of(
                        Map.of("name","카카오페이","tag","간편결제","desc","카카오페이로 빠르게 결제할 수 있습니다."),
                        Map.of("name","카드 결제","tag","일반결제","desc","신용/체크카드로 결제할 수 있습니다."),
                        Map.of("name","무통장 입금","tag","수동확인","desc","입금 후 확인되면 주문 상태가 결제완료로 변경됩니다.")
                )
        );
    }

}

