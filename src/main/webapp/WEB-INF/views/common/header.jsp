<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.example.user.UserDTO" %>
<%
    UserDTO loginUser = (UserDTO) session.getAttribute("LOGIN_USER");
    String ctx = request.getContextPath();
%>

<header style="padding:12px; border-bottom:1px solid #ddd; margin-bottom:16px;">
    <div style="display:flex; justify-content:space-between; align-items:center;">
        <div>
            <a href="<%=ctx%>/" style="font-weight:700; text-decoration:none;">KOREATE SHOP</a>
        </div>

        <nav style="display:flex; gap:10px;">
            <a href="<%=ctx%>/products?category=mouse">마우스</a>
            <a href="<%=ctx%>/products?category=keyboard">키보드</a>
            <a href="<%=ctx%>/products?category=monitor">모니터</a>
            <a href="<%=ctx%>/products?category=speaker">스피커</a>
            <a href="<%=ctx%>/board/list">게시판</a>
            <a href="<%=ctx%>/cart">장바구니</a>
            <a href="<%=ctx%>/chat-page">챗봇</a>

            <% if (loginUser == null) { %>
            <a href="<%=ctx%>/login">로그인</a>
            <a href="<%=ctx%>/register">회원가입</a>
            <% } else { %>
            <span style="color:#555;"><%=loginUser.getName()%>님</span>
            <a href="<%=ctx%>/logout">로그아웃</a>
            <% } %>
        </nav>
    </div>
</header>
