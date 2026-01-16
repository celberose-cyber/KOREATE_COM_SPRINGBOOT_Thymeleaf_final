<%--
  Created by IntelliJ IDEA.
  User: celbe
  Date: 25. 12. 23.
  Time: 오후 12:12
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Title</title>
</head>
<body>
<form method="post" action="<%=request.getContextPath()%>/login">
    <div>
        이메일: <input name="email" type="email" required />
    </div>
    <div>
        비밀번호: <input name="password" type="password" required />
    </div>
    <button type="submit">로그인</button>
</form>
</body>
</html>
