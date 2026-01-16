async function send() {
    const input = document.getElementById("chatInput");
    const msg = input.value.trim();
    if (!msg) return;

    append("나", msg);
    input.value = "";

    const res = await fetch(ctx + "/chat", {
        method: "POST",
        headers: {"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8"},
        body: new URLSearchParams({ message: msg })
    });

    const data = await res.json();
    append("봇", data.answer || "(응답 없음)");
}

function append(who, msg) {
    const log = document.getElementById("chatLog");
    const p = document.createElement("div");
    p.textContent = `${who}: ${msg}`;
    log.appendChild(p);
    log.scrollTop = log.scrollHeight;
}

document.getElementById("chatInput")?.addEventListener("keydown", (e) => {
    if (e.key === "Enter") send();
});
