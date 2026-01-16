package org.zerock.com.example.api.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

    private static final ObjectMapper om = new ObjectMapper();
    private static final String SESSION_PENDING_ACTION = "PENDING_CHAT_ACTION";

    @Value("${shop.llm.url:http://localhost:11434/api/chat}")
    private String ollamaChatUrl;

    @Value("${shop.llm.model:qwen2.5:7b}")
    private String ollamaModel;

    @Value("${shop.llm.enabled:true}")
    private boolean llmEnabled;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> chat(@RequestParam String message, HttpSession session) {

        if (message == null || message.isBlank()) {
            return Map.of("answer", "메시지를 입력해주세요.", "action", Map.of());
        }

        String m = normalize(message);

        // 0) "안녕/도움말/help" => 기능 안내 고정 (LLM 흔들림 방지)
        if (containsAny(m, "안녕", "hello", "hi", "ㅎㅇ", "처음", "도움", "help")) {
            return Map.of(
                    "answer",
                    "안녕하세요! 저는 아래 안내를 도와드려요 🙂\n" +
                            "• 품목안내 • TOP10 • 회원정책 • 결제안내\n" +
                            "원하시면 '품목안내', 'TOP10', '정책', '결제' 라고 입력해보세요!",
                    "action", Map.of()
            );
        }

        // 1) ✅ 사용자가 "네/보여줘" 같은 긍정이면: 직전 pendingAction 실행
        if (isAffirmative(m)) {
            Map<String, Object> pending = getPendingAction(session);
            if (pending != null) {
                session.removeAttribute(SESSION_PENDING_ACTION);

                // 실행 단계 confirmRequired=false
                Map<String, Object> a = new HashMap<>(pending);
                a.put("confirmRequired", false);

                return Map.of(
                        "answer", "네! 바로 보여드릴게요 🙂",
                        "action", a
                );
            }
        }

        // 2) ✅ 룰 기반 action 먼저 결정
        Map<String, Object> action = detectAction(message);

        if (action != null) {
            // 직후에 "네 보여주세요"가 오면 실행할 수 있게 pending 저장
            setPendingAction(session, action);

            // confirm 단계 confirmRequired=true
            Map<String, Object> a = new HashMap<>(action);
            a.put("confirmRequired", true);

            String answer = actionAnswer(a);

            return Map.of("answer", answer, "action", a);
        }

        // 3) ✅ 룰이 없으면 LLM fallback
        if (!llmEnabled) {
            return Map.of(
                    "answer", "원하시는 안내가 있나요? (예: TOP10, 결제, 회원정책, 매장/품목)",
                    "action", Map.of()
            );
        }

        String llmAnswer = callOllamaForAnswer(message);

        if (looksNonKorean(llmAnswer)) {
            llmAnswer = "원하시는 안내를 선택해주세요 🙂 (TOP10 / 결제 / 회원정책 / 매장·품목)";
        }

        return Map.of("answer", llmAnswer, "action", Map.of());
    }

    // ---------------- 룰 기반 ----------------

    private Map<String, Object> detectAction(String msg) {
        String m = normalize(msg);

        if (containsAny(m, "매장", "품목", "카테고리", "취급", "뭐팔아", "뭘팔아")) {
            return panel("intro", Map.of());
        }
        if (containsAny(m, "회원", "등급", "혜택", "적립", "할인", "포인트")) {
            return panel("policy", Map.of());
        }
        if (containsAny(m, "결제", "카드", "카카오", "무통장", "계좌", "페이")) {
            return panel("pay", Map.of());
        }
        if (containsAny(m, "top10", "추천", "베스트", "랭킹", "인기", "top")) {
            String sort = detectSort(m);
            String category = detectCategory(m);
            return panel("top10", Map.of("sort", sort, "category", category));
        }
        return null;
    }

    private String detectSort(String m) {
        if (m.contains("가격")) {
            if (containsAny(m, "높", "비싼", "고가", "내림차순")) return "price_desc";
            if (containsAny(m, "낮", "싼", "저가", "오름차순")) return "price_asc";
        }
        if (containsAny(m, "리뷰", "후기")) return "reviews";
        if (containsAny(m, "최신", "신상", "새로")) return "new";
        if (containsAny(m, "평점", "별점")) return "rating";
        return "rating";
    }

    private String detectCategory(String m) {
        if (containsAny(m, "모니터")) return "monitor";
        if (containsAny(m, "마우스")) return "mouse";
        if (containsAny(m, "키보드")) return "keyboard";
        if (containsAny(m, "스피커")) return "speaker";
        if (containsAny(m, "컴퓨터", "본체", "pc")) return "computer";
        return "all";
    }

    private Map<String, Object> panel(String name, Map<String, Object> params) {
        return Map.of(
                "type", "panel",
                "name", name,
                "params", params == null ? Map.of() : params
        );
    }

    private String actionAnswer(Map<String, Object> action) {
        String name = String.valueOf(action.get("name"));

        // confirmRequired=true일 때만 확인 문구를 좀 더 친절하게
        boolean confirm = Boolean.TRUE.equals(action.get("confirmRequired"));

        if (!confirm) {
            // 실행 단계(이미 렌더링 들어감)
            return "바로 안내를 띄워드릴게요 🙂";
        }

        return switch (name) {
            case "intro"  -> "매장/취급 품목 안내를 보여드릴까요? 원하시면 '네'라고 답해주세요 🙂";
            case "top10"  -> "TOP10 상품 추천을 띄워드릴까요? 원하시면 '네'라고 답해주세요 🙂";
            case "policy" -> "회원 등급/혜택 안내를 보여드릴까요? 원하시면 '네'라고 답해주세요 🙂";
            case "pay"    -> "결제 방법 안내를 보여드릴까요? 원하시면 '네'라고 답해주세요 🙂";
            default       -> "원하시는 안내를 도와드릴게요 🙂";
        };
    }

    // ---------------- LLM fallback ----------------

    private String callOllamaForAnswer(String userMessage) {
        String system = """
          당신은 한국어 쇼핑몰 상담원입니다.
          - 반드시 한국어로만 답변하세요. 중국어/영어 사용 금지.
          - 1~3문장으로 짧게 답변하세요.
          - 필요한 정보가 있으면 1~2개만 추가 질문하세요.
          - 확실하지 않으면: TOP10/결제/회원정책/매장·품목 중 선택하도록 안내하세요.
          """;

        final String bodyJson;
        try {
            bodyJson = """
            {
              "model": "%s",
              "stream": false,
              "messages": [
                {"role": "system", "content": %s},
                {"role": "user", "content": %s}
              ]
            }
            """.formatted(
                    escapeJson(ollamaModel),
                    om.writeValueAsString(system),
                    om.writeValueAsString(userMessage)
            );
        } catch (Exception e) {
            return "죄송해요. 답변을 생성하는 중 오류가 났어요.";
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaChatUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> r = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (r.statusCode() != 200) {
                return "현재 상담이 원활하지 않아요. 잠시 후 다시 시도해주세요.";
            }

            JsonNode root = om.readTree(r.body());
            String answer = root.path("message").path("content").asText("").trim();
            if (answer.isBlank()) answer = "원하시는 안내가 있나요? (TOP10 / 결제 / 회원정책 / 매장·품목)";
            return answer;

        } catch (Exception e) {
            return "현재 상담이 원활하지 않아요. 잠시 후 다시 시도해주세요.";
        }
    }

    // ---------------- 세션 pendingAction ----------------

    private void setPendingAction(HttpSession session, Map<String, Object> action) {
        try {
            String json = om.writeValueAsString(action);
            session.setAttribute(SESSION_PENDING_ACTION, json);
        } catch (Exception ignore) {}
    }

    private Map<String, Object> getPendingAction(HttpSession session) {
        Object v = session.getAttribute(SESSION_PENDING_ACTION);
        if (v == null) return null;
        try {
            return om.readValue(String.valueOf(v), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------- 헬퍼 ----------------

    private static boolean isAffirmative(String m) {
        return containsAny(m,
                "네", "예", "응", "그래", "좋아", "ok", "오케이", "어", "ㅇ","엉","웅",
                "보여줘", "보여주세요", "띄워줘", "띄워주세요", "해줘", "해주세요"
        );
    }

    private static boolean looksNonKorean(String s) {
        if (s == null || s.isBlank()) return true;
        return s.chars().anyMatch(ch -> (ch >= 0x4E00 && ch <= 0x9FFF));
    }

    private static String normalize(String s) {
        return (s == null) ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean containsAny(String s, String... tokens) {
        for (String t : tokens) {
            if (t == null || t.isBlank()) continue;
            if (s.contains(t.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
