<%@ page contentType="text/html; charset=UTF-8" %>
<jsp:include page="/WEB-INF/views/common/header.jsp" />

<h2>메인 화면</h2>
<ul>
    <li><a href="<%=request.getContextPath()%>/products?category=mouse">상품 목록 보기</a></li>
    <li><a href="<%=request.getContextPath()%>/board/list">게시판</a></li>
    <li><a href="<%=request.getContextPath()%>/chat-page">챗봇</a></li>
    <li><a href="<%=request.getContextPath()%>/admin/crawl">관리자 크롤링 실행</a> (임시)</li>
</ul>

<p style="color:#666;">
    * 본 프로젝트는 공개 페이지를 robots.txt 및 요청 간격을 준수하여 수집하고, 수집 결과는 내부 DB 데이터만 사용합니다.
</p>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />
