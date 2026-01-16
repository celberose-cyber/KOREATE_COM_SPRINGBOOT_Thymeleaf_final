<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*, com.example.product.ProductDTO" %>
<%
  String category = (String) request.getAttribute("category");
  List<ProductDTO> list = (List<ProductDTO>) request.getAttribute("list");
%>
<h2>카테고리: <%=category%></h2>

<form action="<%=request.getContextPath()%>/search" method="get">
  <input name="q" placeholder="검색어" />
  <button>검색</button>
</form>

<ul>
  <% for (ProductDTO p : list) { %>
  <li>
    <a href="<%=request.getContextPath()%>/product?id=<%=p.getProductId()%>">
      <%=p.getName()%>
    </a>
    <form action="<%=request.getContextPath()%>/cart" method="post" style="display:inline;">
      <input type="hidden" name="action" value="add"/>
      <input type="hidden" name="productId" value="<%=p.getProductId()%>"/>
      <input type="hidden" name="qty" value="1"/>
      <button>장바구니</button>
    </form>
  </li>
  <% } %>
</ul>
