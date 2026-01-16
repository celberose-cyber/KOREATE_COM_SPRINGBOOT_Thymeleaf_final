<%@ page contentType="text/html; charset=UTF-8" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />

<h2>관리자 크롤링 실행</h2>

<% String msg = (String) request.getAttribute("message"); %>
<% if (msg != null) { %>
<p style="color:green;"><%=msg%></p>
<% } %>

<form method="post" action="<%=request.getContextPath()%>/admin/crawl">
    <label>카테고리</label>
    <select name="category">
        <option value="computer">컴퓨터</option>
        <option value="mouse">마우스</option>
        <option value="keyboard">키보드</option>
        <option value="monitor">모니터</option>
        <option value="speaker">스피커</option>
    </select>
    <button type="submit">크롤링 실행</button>
</form>

<p style="margin-top:12px; color:#666;">
    * robots.txt 및 요청 간격을 준수하고, 수집 결과는 내부 DB에서만 사용합니다.
</p>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />
