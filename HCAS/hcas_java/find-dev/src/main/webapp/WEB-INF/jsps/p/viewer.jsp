<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html><style type="text/css">
    #html, body { width: 100%; height: 100%; overflow: hidden; padding: 0; margin: 0; text-align: center; }
    #padding { height: 40%; }
    a { color: #0088CC; text-decoration: none; }
</style><body>
<c:if test="${not empty param.url}">
    <div id="padding"></div>
    <a href="<c:out value="${param.url}"/>" target="_blank"><spring:message htmlEscape="true" code="idolview.viewer.iframeblocked"/></a>
</c:if>
</body></html>