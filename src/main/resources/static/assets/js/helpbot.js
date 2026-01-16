async function getJson(url) {
    const r = await fetch(url, { headers: { "Accept": "application/json" } });
    if (!r.ok) throw new Error("HTTP " + r.status);
    return r.json();
}
async function getText(url) {
    const r = await fetch(url, { headers: { "Accept": "application/json" } });
    if (!r.ok) throw new Error("HTTP " + r.status);
    return (await r.json()).text;
}

let typingToken = 0;

async function typeText(el, text, speed = 25) {
    const myToken = ++typingToken;
    el.textContent = "";
    for (let i = 0; i < text.length; i++) {
        if (myToken !== typingToken) return; // 취소됨
        el.textContent += text[i];
        await new Promise(r => setTimeout(r, speed));
    }
}


function escapeHtml(s) {
    return (s ?? "").replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;")
        .replaceAll('"',"&quot;").replaceAll("'","&#039;");
}

function renderIntro(panel, data) {
    panel.innerHTML = `
    <div class="box">
      <div class="box-hd">
        <div class="box-title">매장/취급 품목 안내</div>
        <span class="pill">INFO</span>
      </div>

      <div>${escapeHtml(data.text)}</div>

      <div class="meta" style="margin-top:8px;">
        ${data.categories.map(c => `
          <button class="pill cat-btn"
                  data-slug="${escapeHtml(c.slug)}"
                  data-label="${escapeHtml(c.label)}"
                  type="button">
            ${escapeHtml(c.label)}
          </button>
        `).join("")}
      </div>

      <div id="introHint" class="muted" style="margin-top:10px;">
        카테고리 버튼을 누르시면 상품을 확인하실 수 있어요.
      </div>
    </div>
  `;
}


function renderPolicy(panel, data) {
    panel.innerHTML = `
    <div class="box">
      <div class="box-hd">
        <div class="box-title">회원정책 안내</div>
        <span class="pill">POLICY</span>
      </div>
      <div>${escapeHtml(data.text)}</div>
      ${data.policies?.length ? `
        <div class="list" style="margin-top:10px;">
          ${data.policies.map(p => `
            <div class="box" style="margin:0;">
              <div class="row">
                <div class="name">${escapeHtml(p.grade)}</div>
                <span class="pill">최소 ${Number(p.minTotalSpent).toLocaleString()}원</span>
              </div>
              <div class="meta" style="margin-top:6px;">
                <span>할인 ${p.discountRate}%</span>
                <span>적립 ${p.pointRate}%</span>
              </div>
            </div>
          `).join("")}
        </div>
      ` : `<div class="muted" style="margin-top:8px;">정책 데이터가 없습니다.</div>`}
    </div>
  `;
}

function renderPay(panel, data) {
    panel.innerHTML = `
    <div class="box">
      <div class="box-hd">
        <div class="box-title">결제방법 안내</div>
        <span class="pill">PAY</span>
      </div>
      <div class="list">
        ${data.methods.map(m => `
          <div class="box" style="margin:0;">
            <div class="row">
              <div class="name">${escapeHtml(m.name)}</div>
              <span class="pill">${escapeHtml(m.tag)}</span>
            </div>
            <div class="muted" style="margin-top:6px;">${escapeHtml(m.desc)}</div>
          </div>
        `).join("")}
      </div>
    </div>
  `;
}
function normalizeUrl(u) {
    if (!u) return "";
    // //example.com 형태 보정
    if (u.startsWith("//")) return location.protocol + u;
    return u;
}

function renderTop10(panel, data) {
    const sort = (data.sort || "rating");
    const category = (data.category || "all");

    const itemsHtml = (data.items || []).map((it, idx) => {
        const img = normalizeUrl(it.imageUrl);
        const detail = normalizeUrl(it.detailUrl);

        // 내부 상세 페이지가 실제로 없으면 products?category=... 로 보내거나,
        // id 기반 상세가 있으면 아래 유지
        const href = CTX + "product?id=" + encodeURIComponent(it.id)
            + "&returnUrl=" + encodeURIComponent(location.pathname + location.search);
        return `
      <a class="t10-item"
         href="${escapeHtml(href)}"
         data-href="${escapeHtml(href)}"
         target="${detail ? "_blank" : "_self"}"
         rel="noopener">
        <div class="t10-rank">${idx + 1}</div>

        <div class="t10-thumb ${img ? "" : "noimg"}">
          ${
            img
                ? `<img class="t10-img" src="${escapeHtml(img)}"
                       alt="${escapeHtml(it.name)}"
                       loading="lazy"
                       onerror="this.closest('.t10-thumb').classList.add('noimg'); this.remove();">`
                : `<div class="t10-noimg">NO IMAGE</div>`
        }
        </div>

        <div class="t10-body">
          <div class="t10-name">${escapeHtml(it.name)}</div>
          <div class="t10-meta">
            <span><b>${Number(it.price).toLocaleString()}</b>원</span>
            <span>평점 ${it.rating}</span>
            <span>리뷰 ${Number(it.reviewCount).toLocaleString()}</span>
          </div>
        </div>

        <div class="t10-cta">보기 →</div>
      </a>
    `;
    }).join("");

    panel.innerHTML = `
    <div class="box">
      <div class="box-hd">
        <div>
          <div class="box-title">TOP10 상품 추천</div>
          <div class="muted" style="margin-top:4px;">
            정렬 기준을 바꿔 TOP10을 볼 수 있어요.
          </div>
        </div>
        <span class="pill">TOP10</span>
      </div>

      <!-- ✅ 컨트롤 -->
      <div class="ctrl" style="margin-top:10px;">
        <div class="ctrl-title">정렬</div>
        <div class="btn-row">
          <button class="chip ${sort==="rating"?"active":""}" data-sort="rating"  type="button">평점순</button>
          <button class="chip ${sort==="priceDesc"?"active":""}" data-sort="priceDesc" type="button">가격높은순</button>
          <button class="chip ${sort==="priceAsc" ?"active":""}" data-sort="priceAsc"  type="button">가격낮은순</button>
          <button class="chip ${sort==="reviews"?"active":""}" data-sort="reviews" type="button">리뷰순</button>
          <button class="chip ${sort==="new"?"active":""}"    data-sort="new"     type="button">최신순</button>
        </div>

        <div class="ctrl-title" style="margin-top:10px;">카테고리</div>
        <div class="btn-row">
          <button class="chip ${category==="all"?"active":""}" data-cat="all" type="button">전체</button>
          <button class="chip ${category==="monitor"?"active":""}" data-cat="monitor" type="button">모니터</button>
          <button class="chip ${category==="mouse"?"active":""}" data-cat="mouse" type="button">마우스</button>
          <button class="chip ${category==="keyboard"?"active":""}" data-cat="keyboard" type="button">키보드</button>
          <button class="chip ${category==="speaker"?"active":""}" data-cat="speaker" type="button">스피커</button>
          <button class="chip ${category==="computer"?"active":""}" data-cat="computer" type="button">컴퓨터</button>
        </div>
      </div>

      <div class="t10-list" style="margin-top:10px;">
        ${ (data.items && data.items.length) ? itemsHtml : `<div class="muted">표시할 상품이 없습니다.</div>` }
      </div>
    </div>
  `;

    // ✅ 버튼 클릭 핸들러 (panel 내부 이벤트로 처리)
    panel.querySelectorAll(".chip[data-sort]").forEach(b => {
        b.addEventListener("click", async () => {
            const nextSort = b.dataset.sort;
            const d = await getJson(HELP_API + "/top10?sort=" + encodeURIComponent(nextSort) + "&category=" + encodeURIComponent(category));
            renderTop10(panel, d);
        });
    });

    panel.querySelectorAll(".chip[data-cat]").forEach(b => {
        b.addEventListener("click", async () => {
            const nextCat = b.dataset.cat;
            const d = await getJson(HELP_API + "/top10?sort=" + encodeURIComponent(sort) + "&category=" + encodeURIComponent(nextCat));
            renderTop10(panel, d);
        });
    });
}

function streamSayHint({ intent, category, hintEl }) {
    return new Promise((resolve, reject) => {
        // 이전 스트림이 있으면 끊기 (전역으로 관리)
        if (window.__hintES) {
            window.__hintES.close();
            window.__hintES = null;
        }

        // 표시 초기화
        if (hintEl) hintEl.textContent = "";

        const url = HELP_API + "/say/stream?intent=" +
            encodeURIComponent(intent) +
            "&category=" + encodeURIComponent(category || "");

        const es = new EventSource(url);
        window.__hintES = es;

        es.addEventListener("token", (ev) => {
            // 토큰/조각이 올 때마다 추가 → 진짜 스트리밍 느낌
            if (hintEl) hintEl.textContent += ev.data;
        });

        es.addEventListener("done", () => {
            es.close();
            window.__hintES = null;
            resolve();
        });

        es.addEventListener("error", (ev) => {
            // EventSource는 서버가 끊어져도 error가 뜸
            es.close();
            window.__hintES = null;
            reject(new Error("SSE error"));
        });
    });
}
function bindChat() {
    const btn = document.getElementById("sendBtn");
    const input = document.getElementById("chatInput");

    if (!btn || !input) return; // help.html에 채팅 UI 없으면 그냥 패스

    btn.addEventListener("click", sendChat);
    input.addEventListener("keydown", (e) => {
        if (e.key === "Enter") sendChat();
    });
}
function appendBotCard(log) {
    const wrap = document.createElement("div");
    wrap.className = "chat-card-line";
    wrap.innerHTML = `<b>봇:</b> <div class="chat-card"></div>`;
    log.appendChild(wrap);
    log.scrollTop = log.scrollHeight;
    return wrap.querySelector(".chat-card");
}
function appendBotCard(log) {
    const wrap = document.createElement("div");
    wrap.className = "chat-card-line";
    wrap.innerHTML = `<b>봇:</b> <div class="chat-card"></div>`;
    log.appendChild(wrap);
    log.scrollTop = log.scrollHeight;
    return wrap.querySelector(".chat-card");
}

async function sendChat() {
    const input = document.getElementById("chatInput");
    const log   = document.getElementById("chatLog");
    const panel = document.getElementById("panel");

    const msg = (input.value || "").trim();
    if (!msg) return;

    log.insertAdjacentHTML("beforeend", `<div><b>나:</b> ${escapeHtml(msg)}</div>`);
    log.scrollTop = log.scrollHeight;
    input.value = "";

    const botLine = document.createElement("div");
    botLine.innerHTML = `<b>봇:</b> <span class="botText"></span>`;
    log.appendChild(botLine);
    log.scrollTop = log.scrollHeight;

    const botTextEl = botLine.querySelector(".botText");

    try {
        const url = (CTX.endsWith("/") ? CTX : CTX + "/") + "api/chat";

        const body = new URLSearchParams();
        body.set("message", msg);

        const res = await fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8" },
            body: body.toString(),
            credentials: "same-origin"
        });

        const data = await res.json();
        const answer = (data && data.answer) ? data.answer : "(응답 없음)";

        await typeText(botTextEl, answer, 18);
        log.scrollTop = log.scrollHeight;

        // ✅ action 처리 (A안)
        if (data?.action?.type === "panel") {
            const confirmRequired = data.action.confirmRequired === true;

            // 1) confirmRequired=true => 아직 "보여드릴까요?" 단계
            //    패널/카드 렌더링 금지 + 힌트만 표시
            if (confirmRequired) {
                log.insertAdjacentHTML(
                    "beforeend",
                    `<div class="muted"><b>힌트:</b> 원하시면 <b>네</b> 또는 <b>보여줘</b>라고 답해주세요.</div>`
                );
                log.scrollTop = log.scrollHeight;
                return;
            }

            // 2) confirmRequired=false => 실제 렌더링 실행 단계
            const name   = data.action.name;
            const params = data.action.params || {};

            // 패널 로딩
            if (panel) panel.innerHTML = `<div class="muted">불러오는 중...</div>`;

            // 채팅 카드 로딩(배너를 같은 채팅창에도 보여주기)
            const cardHost = appendBotCard(log);
            cardHost.innerHTML = `<div class="muted">불러오는 중...</div>`;

            if (name === "intro") {
                const d = await getJson(HELP_API + "/intro");
                if (panel) renderIntro(panel, d);
                renderIntro(cardHost, d);

            } else if (name === "top10") {
                const sort = params.sort || "rating";
                const cat  = params.category || "all";
                const d = await getJson(
                    HELP_API + "/top10?sort=" + encodeURIComponent(sort) +
                    "&category=" + encodeURIComponent(cat)
                );
                if (panel) renderTop10(panel, d);
                renderTop10(cardHost, d);

            } else if (name === "policy") {
                const d = await getJson(HELP_API + "/policy");
                if (panel) renderPolicy(panel, d);
                renderPolicy(cardHost, d);

            } else if (name === "pay") {
                const d = await getJson(HELP_API + "/pay");
                if (panel) renderPay(panel, d);
                renderPay(cardHost, d);

            } else {
                const html = `<div class="muted">지원하지 않는 안내입니다.</div>`;
                if (panel) panel.innerHTML = html;
                cardHost.innerHTML = html;
            }

            log.scrollTop = log.scrollHeight;
        }

    } catch (e) {
        botTextEl.textContent = `에러: ${String(e)}`;
    }
}


// 페이지 로딩 시 채팅 바인딩
document.addEventListener("DOMContentLoaded", bindChat);
document.addEventListener("click", async (e) => {

    // 1) cat 버튼이 먼저 우선 처리
    const catBtn = e.target.closest(".cat-btn");
    if (catBtn) {
        e.preventDefault();
        e.stopPropagation();

        const slug = catBtn.dataset.slug;
        const label = catBtn.dataset.label;
        const hintEl = document.getElementById("introHint");

        try {
            await streamSayHint({
                intent: "go_category",
                category: label,
                hintEl
            });
        } catch (_) {
            if (hintEl) {
                hintEl.textContent = "카테고리 버튼을 누르시면 상품을 확인하실 수 있어요.";
            }
        }

        // ✅ 스트리밍 끝난 다음 이동
        window.location.href = CTX + "products?category=" + encodeURIComponent(slug);

        return; // ✅ cat-btn 처리 후 여기서 종료
    }

    // 2) qbtn 처리 (intro / policy / pay / top10)
    const btn = e.target.closest(".qbtn");
    if (!btn) return;

    const action = btn.dataset.action;
    const panel = document.getElementById("panel");
    panel.innerHTML = `<div class="muted">불러오는 중...</div>`;

    try {
        if (action === "intro") {
            const data = await getJson(HELP_API + "/intro");
            renderIntro(panel, data);
        } else if (action === "top10") {
            const data = await getJson(HELP_API + "/top10?sort=rating&category=all");
            renderTop10(panel, data);
        } else if (action === "policy") {
            const data = await getJson(HELP_API + "/policy");
            renderPolicy(panel, data);
        } else if (action === "pay") {
            const data = await getJson(HELP_API + "/pay");
            renderPay(panel, data);
        }
    } catch (err) {
        panel.innerHTML = `
          <div class="box">
            <b>오류</b>
            <div class="muted" style="margin-top:6px;">
              ${escapeHtml(String(err))}
            </div>
          </div>`;
    }
});


