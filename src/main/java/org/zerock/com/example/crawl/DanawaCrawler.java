package org.zerock.com.example.crawl;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

//@Service
public class DanawaCrawler {

    public record CrawlStats(int seen, int inserted, int updated) {}

    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36";

    private final JdbcTemplate jt;

    public DanawaCrawler(JdbcTemplate jt) {
        this.jt = jt;
    }

    public CrawlStats crawlMainItems(String category, String keyword, int maxPages) throws Exception {
        int seen = 0, inserted = 0, updated = 0;

        try (Playwright pw = Playwright.create()) {
            Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true));

            Page page = browser.newPage(new Browser.NewPageOptions().setUserAgent(UA));

            for (int p = 1; p <= maxPages; p++) {
                String url = buildSearchUrl(keyword, p);
                page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

                // ✅ JS 렌더링된 상품 DOM 대기
                Locator items = page.locator("li.prod_item[id^='productItem']");
                // 페이지에 따라 조금 느릴 수 있어서 15초
                items.first().waitFor(new Locator.WaitForOptions().setTimeout(15000));

                int count = items.count();
                System.out.println("[crawl][pw] category=" + category + " page=" + p + " items=" + count);

                if (count == 0) break;

                for (int i = 0; i < count; i++) {
                    Locator item = items.nth(i);

                    String id = item.getAttribute("id"); // productItem12345
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

                    // 가격: hidden min_price
                    Integer price = null;
                    Locator minPrice = item.locator("input[id^='min_price_']").first();
                    if (minPrice.count() > 0) {
                        price = parseIntSafe(minPrice.getAttribute("value"));
                    }

                    // 스펙
                    String specText = null;
                    Locator spec = item.locator("div.spec_list").first();
                    if (spec.count() > 0) specText = normalize(safeText(spec));

                    // 등록월
                    String regMonth = null;
                    Locator rm = item.locator("dl.meta_item.mt_date dd").first();
                    if (rm.count() > 0) regMonth = safeText(rm);

                    // 상품의견 수
                    Integer opinionCount = null;
                    Locator oc = item.locator("a.click_log_prod_content_count strong").first();
                    if (oc.count() > 0) opinionCount = parseIntSafe(safeText(oc));

                    // 평점/리뷰수
                    Double rating = null;
                    Integer reviewCount = null;
                    Locator score = item.locator("a.click_log_prod_review_count .text__score").first();
                    if (score.count() > 0) rating = parseDoubleSafe(safeText(score));
                    Locator rc = item.locator("a.click_log_prod_review_count .text__number").first();
                    if (rc.count() > 0) reviewCount = parseIntSafe(safeText(rc));

                    seen++;

                    // ✅ inserted/updated 정확 카운트: 존재 체크 후 분기
                    boolean exists = existsByDetailUrl(detailUrl);

                    if (!exists) {
                        insertProduct(category, pcode, name, price, detailUrl, imageUrl,
                                specText, regMonth, opinionCount, rating, reviewCount);
                        inserted++;
                    } else {
                        updateProduct(category, pcode, name, price, detailUrl, imageUrl,
                                specText, regMonth, opinionCount, rating, reviewCount);
                        updated++;
                    }
                }

                Thread.sleep(1200);
            }

            browser.close();
        }

        return new CrawlStats(seen, inserted, updated);
    }

    // ---------------------------
    // DB
    // ---------------------------
    private boolean existsByDetailUrl(String detailUrl) {
        Integer n = jt.queryForObject(
                "SELECT COUNT(*) FROM products WHERE detail_url = ?",
                Integer.class,
                detailUrl
        );
        return n != null && n > 0;
    }

    private void insertProduct(
            String category, String pcode, String name, Integer price,
            String detailUrl, String imageUrl, String specText, String regMonth,
            Integer opinionCount, Double rating, Integer reviewCount
    ) {
        String sql = """
            INSERT INTO products(
              category, pcode, name, price, detail_url, image_url,
              source, spec_text, reg_month, opinion_count, rating, review_count
            )
            VALUES(?,?,?,?,?,?,'danawa',?,?,?,?,?)
        """;

        jt.update(sql,
                category,
                safeTrimOrNull(pcode),
                name,
                price,
                detailUrl,
                imageUrl,
                specText,
                regMonth,
                opinionCount,
                rating,
                reviewCount
        );
    }

    private void updateProduct(
            String category, String pcode, String name, Integer price,
            String detailUrl, String imageUrl, String specText, String regMonth,
            Integer opinionCount, Double rating, Integer reviewCount
    ) {
        String sql = """
            UPDATE products
               SET category=?,
                   pcode=?,
                   name=?,
                   price=?,
                   image_url=?,
                   spec_text=?,
                   reg_month=?,
                   opinion_count=?,
                   rating=?,
                   review_count=?,
                   updated_at=CURRENT_TIMESTAMP
             WHERE detail_url=?
        """;

        jt.update(sql,
                category,
                safeTrimOrNull(pcode),
                name,
                price,
                imageUrl,
                specText,
                regMonth,
                opinionCount,
                rating,
                reviewCount,
                detailUrl
        );
    }

    // ---------------------------
    // URL builder
    // ---------------------------
    private String buildSearchUrl(String keyword, int page) {
        return "https://search.danawa.com/dsearch.php?query=" +
                URLEncoder.encode(keyword, StandardCharsets.UTF_8) +
                "&tab=goods&list=list&limit=40&page=" + page +
                "&mode=simple";
    }

    // ---------------------------
    // utils
    // ---------------------------
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

    private static String normalize(String s) {
        if (s == null) return null;
        return s.replaceAll("\\s+", " ").trim();
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
