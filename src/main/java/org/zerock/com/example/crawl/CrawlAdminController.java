package org.zerock.com.example.crawl;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
public class CrawlAdminController {

    private static final Map<String, String> CATEGORY_KEYWORD = Map.of(
            "computer", "본체",
            "monitor",  "모니터",
            "mouse",    "마우스",
            "keyboard", "키보드",
            "speaker",  "스피커"
    );

    private final DanawaProductParseService crawlService;

    public CrawlAdminController(DanawaProductParseService crawlService) {
        this.crawlService = crawlService;
    }

    @GetMapping("/admin/crawl")
    public String crawlPage() {
        return "admin/crawl";
    }

    @PostMapping("/admin/crawl")
    public String doCrawl(
            @RequestParam(defaultValue = "monitor") String category,
            @RequestParam(defaultValue = "3") int maxPages,
            Model model
    ) {
        // ✅ 1~10 강제 (서버쪽에서도 반드시 제한)
        maxPages = Math.max(1, Math.min(10, maxPages));

        String keyword = CATEGORY_KEYWORD.getOrDefault(category, "모니터");

        try {
            var s = crawlService.crawlAndUpsert(category, keyword, maxPages);

            model.addAttribute("message",
                    "완료: category=" + category +
                            ", keyword=" + keyword +
                            ", pages=" + maxPages +
                            ", seen=" + s.seen() +
                            ", upserted=" + s.upserted());

        } catch (Exception e) {
            model.addAttribute("message", "크롤링 실패: " + e.getMessage());
        }

        return "admin/crawl";
    }
}
