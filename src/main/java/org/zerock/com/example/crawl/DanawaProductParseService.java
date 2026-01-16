package org.zerock.com.example.crawl;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.stereotype.Service;
import org.zerock.com.example.product.ProductDAO;
import org.zerock.com.example.product.ProductDTO;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.Map;

@Service
public class DanawaProductParseService {

    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36";

    private final ProductDAO dao;

    public DanawaProductParseService(ProductDAO dao) {
        this.dao = dao;
    }

    public record CrawlResult(int seen, int upserted) {}

    public CrawlResult crawlAndUpsert(String category, String keyword, int maxPages) {
        int seen = 0;
        int upserted = 0;

        try (Playwright pw = Playwright.create()) {
            Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage(new Browser.NewPageOptions().setUserAgent(UA));

            for (int p = 1; p <= maxPages; p++) {
                String url = buildSearchUrl(keyword, p);
                page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

                // 렌더 안정화 (너무 길 필요 없음)
                page.waitForTimeout(700);

                Locator items = page.locator("li.prod_item[id^='productItem']");
                int count = items.count();
                if (count == 0) break;

                for (int i = 0; i < count; i++) {
                    Locator item = items.nth(i);

                    String id = item.getAttribute("id");
                    String pcode = (id != null && id.startsWith("productItem"))
                            ? id.substring("productItem".length()).trim()
                            : null;

                    Locator nameA = item.locator("div.prod_info p.prod_name > a").first();
                    if (nameA.count() == 0) continue;

                    String name = safeText(nameA);
                    String detailUrl = absUrl(nameA.getAttribute("href"));
                    if (name == null || name.isBlank()) continue;
                    if (detailUrl == null || detailUrl.isBlank()) continue;

                    // 이미지
                    String imageUrl = null;
                    Locator img = item.locator("div.thumb_image img").first();
                    if (img.count() > 0) {
                        imageUrl = firstNonBlank(
                                absFix(img.getAttribute("data-src")),
                                absFix(img.getAttribute("src"))
                        );
                    }

                    // 가격
                    Integer price = null;
                    Locator minPrice = item.locator("input[id^='min_price_']").first();
                    if (minPrice.count() > 0) price = parseIntSafe(minPrice.getAttribute("value"));

                    // ✅ specText / extraText 분리 (태그 기반)
                    String specText = null;
                    String extraText = null;
                    Map<String, String> se = splitSpecAndExtra(item);
                    if (se != null) {
                        specText = se.get("specText");
                        extraText = se.get("extraText");
                    }

                    // 등록월
                    String regMonth = null;
                    Locator rm = item.locator("dl.meta_item.mt_date dd").first();
                    if (rm.count() > 0) regMonth = safeText(rm);

                    // 상품의견
                    Integer opinionCount = null;
                    Locator oc = item.locator("a.click_log_prod_content_count strong").first();
                    if (oc.count() > 0) opinionCount = parseIntSafe(safeText(oc));

                    // 평점/리뷰
                    Double rating = null;
                    Integer reviewCount = null;
                    Locator score = item.locator("a.click_log_prod_review_count .text__score").first();
                    if (score.count() > 0) rating = parseDoubleSafe(safeText(score));

                    Locator rc = item.locator("a.click_log_prod_review_count .text__number").first();
                    if (rc.count() > 0) reviewCount = parseIntSafe(safeText(rc));

                    ProductDTO dto = new ProductDTO();
                    dto.setCategory(category);
                    dto.setPcode(safeTrimOrNull(pcode));
                    dto.setName(safeTrimOrNull(name));
                    dto.setPrice(price);
                    dto.setDetailUrl(detailUrl);
                    dto.setImageUrl(imageUrl);
                    dto.setSpecText(specText);
                    dto.setExtraText(extraText);
                    dto.setRegMonth(regMonth);
                    dto.setOpinionCount(opinionCount);
                    dto.setRating(rating);
                    dto.setReviewCount(reviewCount);

                    seen++;
                    int affected = dao.upsertByDetailUrl(dto);
                    if (affected > 0) upserted++;
                }

                page.waitForTimeout(900);
            }

            browser.close();
            return new CrawlResult(seen, upserted);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String buildSearchUrl(String keyword, int page) {
        return "https://search.danawa.com/dsearch.php?query=" +
                URLEncoder.encode(keyword, StandardCharsets.UTF_8) +
                "&tab=goods&list=list&limit=40&page=" + page +
                "&mode=simple";
    }

    /**
     * div.spec_list 를 childNodes 순서대로 읽고,
     * span.cm_mark 등장 이후는 extraText로 분리.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, String> splitSpecAndExtra(Locator item) {
        Object obj = item.evaluate("""
            (el) => {
              const spec = el.querySelector('div.spec_list');
              if (!spec) return { specText: null, extraText: null };

              const nodes = Array.from(spec.childNodes);

              let extraMode = false;
              let specParts = [];
              let extraParts = [];

              function push(arr, text) {
                const t = (text || '').replace(/\\s+/g, ' ').trim();
                if (t) arr.push(t);
              }

              for (const n of nodes) {
                if (n.nodeType === Node.ELEMENT_NODE) {
                  const e = n;

                  // ✅ 라벨 태그 등장 -> extra 모드
                  if (e.matches('span.cm_mark')) {
                    extraMode = true;
                    push(extraParts, e.innerText);
                    continue;
                  }

                  // br은 줄바꿈
                  if (e.tagName === 'BR') {
                    (extraMode ? extraParts : specParts).push('\\n');
                    continue;
                  }

                  const text = e.innerText;
                  if (extraMode) push(extraParts, text);
                  else push(specParts, text);

                } else if (n.nodeType === Node.TEXT_NODE) {
                  const text = n.textContent;
                  if (extraMode) push(extraParts, text);
                  else push(specParts, text);
                }
              }

              const joinClean = (arr) => arr
                .join(' ')
                .replace(/\\s*\\n\\s*/g, '\\n')
                .replace(/\\s{2,}/g, ' ')
                .trim();

              return {
                specText: joinClean(specParts) || null,
                extraText: joinClean(extraParts) || null
              };
            }
        """);

        return (Map<String, String>) obj;
    }

    // ---------- utils ----------
    private static String safeText(Locator loc) {
        String t = loc.innerText();
        return t == null ? null : t.trim();
    }

    private static String absUrl(String href) {
        if (href == null || href.isBlank()) return null;
        if (href.startsWith("//")) return "https:" + href;
        if (href.startsWith("http://") || href.startsWith("https://")) return href;
        if (href.startsWith("/")) return "https://search.danawa.com" + href;
        return href;
    }

    private static String absFix(String url) {
        if (url == null || url.isBlank()) return null;
        if (url.startsWith("//")) return "https:" + url;
        return url;
    }

    private static Integer parseIntSafe(String s) {
        if (s == null) return null;
        String x = s.replaceAll("[^0-9]", "");
        if (x.isBlank()) return null;
        try { return Integer.parseInt(x); } catch (Exception e) { return null; }
    }

    private static Double parseDoubleSafe(String s) {
        if (s == null) return null;
        String x = s.replaceAll("[^0-9.]", "");
        if (x.isBlank()) return null;
        try { return Double.parseDouble(x); } catch (Exception e) { return null; }
    }

    private static String firstNonBlank(String... xs) {
        for (String x : xs) if (x != null && !x.isBlank()) return x;
        return null;
    }

    private static String safeTrimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isBlank() ? null : t;
    }
}
