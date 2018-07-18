<%@ page import="com.autonomy.find.services.admin.AdminService" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!-- The navigation bar -->

<ul class="nav mainNav">
	<li><a class="navLink" href="<%=request.getContextPath()%>/p/search.do"><i class="icon-white icon-search"></i> Search</a></li>
    <spring:eval expression="@'idol.properties'['find.showTopicmap']" var="showPage"/>
    <c:if test="${showPage}">
        <li><a class="navLink" href="<%=request.getContextPath()%>/p/topicmap.do"><i class="icon-white icon-tags"></i> Topic Map</a></li>
    </c:if>
    <spring:eval expression="@'idol.properties'['find.showVisualiser']" var="showPage"/>
    <c:if test="${showPage}">
        <li><a class="navLink" href="<%=request.getContextPath()%>/p/visualiser.do"><i class="icon-white icon-remove-circle"></i> Visualiser</a></li>
    </c:if>
    <spring:eval expression="@'idol.properties'['find.showThemetracker']" var="showPage"/>
    <c:if test="${showPage}">
        <li><a class="navLink" href="<%=request.getContextPath()%>/p/themetracker.do"><i class="icon-white icon-screenshot"></i> Theme Tracker</a></li>
    </c:if>
    <spring:eval expression="@'idol.properties'['find.showIdolview']" var="showPage"/>
    <c:if test="${showPage}">
        <li><a class="navLink" href="<%=request.getContextPath()%>/p/idolview.do?iframe=true"><i class="icon-white icon-asterisk"></i> Sunburst</a></li>
    </c:if>
    <spring:eval expression="@'idol.properties'['find.showDocGraph']" var="showPage"/>
    <c:if test="${showPage}">
        <li><a class="navLink" href="<%=request.getContextPath()%>/p/nodegraph.do"><i class="icon-white icon-random"></i> Nodegraph</a></li>
    </c:if>
    <spring:eval expression="@'idol.properties'['find.showNodeGraph']" var="showPage"/>
    <c:if test="${showPage}">
        <li><a class="navLink" href="<%=request.getContextPath()%>/p/docgraph.do"><i class="icon-white icon-random"></i> Docgraph</a></li>
    </c:if>
    <% if (session.getAttribute(AdminService.Privileges.ADMIN.getPrivilegeName()) != null) { %>
        <li><a class="navLink" href="<%=request.getContextPath()%>/admin/adminview.do"><i class="icon-white icon-eye-open"></i> Admin Console</a></li>
    <%}%>
</ul>