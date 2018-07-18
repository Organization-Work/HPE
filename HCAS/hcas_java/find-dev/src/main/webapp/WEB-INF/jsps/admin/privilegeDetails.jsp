<%@ page contentType="text/html;charset=UTF-8"  %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="sitemesh-decorator" prefix="decorator"%>
<%@ taglib uri="sitemesh-page" prefix="page"%>
<spring:eval expression="@'idol.properties'['discover.version']" var="version"/>
<!DOCTYPE html>
<html>
<head>
    <link rel="stylesheet"
          href="<%=request.getContextPath()%>/resources/css/chosen.min.css"
    type="text/css">
    <link rel="stylesheet"
          href="<%=request.getContextPath()%>/css/admin-${version}.css"
          type="text/css">
    <title></title>
</head>
<body>

<page:applyDecorator page="/WEB-INF/decorators/adminSubNav.jsp"
                     name="adminSubNav" />

<div class="container center-block .col-md-8">
    <div><a href="<%=request.getContextPath()%>/admin/manageRoles.do">< Back</a></div>
<c:choose>
    <c:when test="${model.allRoles != null && model.privilegeName != null}">
    <div class="page-header">
        <h1><div id="priviName">${model.privilegeName}</div> <small>Manage privilege</small></h1>
       </div>
        <div id="privilegeSuccess" class="alert alert-success hide"> Successfully saved privileges</div>
        <div id="privilegeFailed" class="alert alert-error hide"> Failed to save privileges</div>

        <b>Roles that contain this privilege:</b>
        <select id="rolesList" type="text" placeholder="Edit roles" multiple>
        <c:forEach var="role" items="${model.allRoles}">
            <option <c:if test="${model.roles.contains(role)}">selected</c:if> value='<c:out value="${role}"/>'><c:out value="${role}"/></option>
        </c:forEach>
        </select>

        <button id="savePrivilege" class="btn btn-default" disabled>Save</button>
    </c:when>

    <c:otherwise>
        Sorry, we could not find this privilege
        <br />
    </c:otherwise>
</c:choose>
</div>
<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/resources/js/chosen.jquery.min.js"></script>
<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/scripts/admin/common-${version}.js"></script>
<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/scripts/admin/privilegeDetails-${version}.js"></script>

</body>
</html>