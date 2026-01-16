let typingToken = 0;

async function typeText(el, text, speed = 20) {
    const myToken = ++typingToken;
    el.textContent = "";
    for (let i = 0; i < text.length; i++) {
        if (myToken !== typingToken) return; // 다른 요청이 오면 중단
        el.textContent += text[i];
        await new Promise(r => setTimeout(r, speed));
    }
}

async function send() {
    const input = document.getElementById("chatInput");
    const log = document.getElementById("chatLog");
    const msg = (input.value || "").trim();
    if (!msg) return;

    // 사용자 메시지
    log.innerHTML += `<div><b>나:</b> ${escapeHtml(msg)}</div>`;
    log.scrollTop = log.scrollHeight;
    input.value = "";

    // ✅ 봇 메시지 자리(placeholder) 미리 만들어두기
    const botLine = document.createElement("div");
    botLine.innerHTML = `<b>봇:</b> <span class="botText">...</span>`;
    log.appendChild(botLine);
    log.scrollTop = log.scrollHeight;

    const botTextEl = botLine.querySelector(".botText");

    try {
        const url = (ctx.endsWith("/") ? ctx : ctx + "/") + "api/chat";
        const body = new URLSearchParams();
        body.set("message", msg);

        const res = await fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8" },
            body: body.toString()
        });

        const data = await res.json();
        const answer = (data && data.answer) ? data.answer : "(응답 없음)";

        // ✅ 한 글자씩 출력
        await typeText(botTextEl, answer, 18);
        log.scrollTop = log.scrollHeight;

    } catch (e) {
        botTextEl.textContent = "";
        botLine.innerHTML = `<div style="color:red;"><b>에러:</b> ${escapeHtml(String(e))}</div>`;
        log.scrollTop = log.scrollHeight;
    }
}

function escapeHtml(s) {
    return (s ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#039;");
}

document.addEventListener("DOMContentLoaded", () => {
    const btn = document.getElementById("sendBtn");
    const input = document.getElementById("chatInput");
    if (btn) btn.addEventListener("click", send);
    if (input) input.addEventListener("keydown", (e) => {
        if (e.key === "Enter") send();
    });
});


function escapeHtml(s) {
    return s
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#039;");
}

// 버튼/엔터키 바인딩
document.addEventListener("DOMContentLoaded", () => {
    const btn = document.getElementById("sendBtn");
    const input = document.getElementById("chatInput");

    if (btn) btn.addEventListener("click", send);
    if (input) {
        input.addEventListener("keydown", (e) => {
            if (e.key === "Enter") send();
        });
    }
});
