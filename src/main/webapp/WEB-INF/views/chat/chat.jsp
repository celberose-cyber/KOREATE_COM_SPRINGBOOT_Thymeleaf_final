<%@ page contentType="text/html; charset=UTF-8" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />

<h2>쇼핑 도우미 챗봇</h2>

<div style="border:1px solid #ccc; padding:12px; width:420px;">
    <div id="chatLog" style="height:240px; overflow:auto; border:1px solid #eee; padding:8px; margin-bottom:8px;"></div>

    <input id="chatInput" type="text" style="width:320px;" placeholder="예: 마우스 찾아줘 / 구매하고싶어" />
    <button onclick="send()">전송</button>
</div>

<script>
    const ctx = "<%=request.getContextPath()%>";
</script>
<script src="<%=request.getContextPath()%>/assets/js/chat.js"></script>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />
