<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="sitemesh-decorator" prefix="decorator"%>
<%@ taglib uri="sitemesh-page" prefix="page"%>
<spring:eval expression="@'idol.properties'['discover.version']" var="version"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>Discover Admin Console</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="">
    <meta name="author" content="">

    <!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
    <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
    <![endif]-->

    <head>
        <link rel="stylesheet"
              href="<%=request.getContextPath()%>/resources/css/chosen.min.css"
              type="text/css">
        <link rel="stylesheet"
              href="<%=request.getContextPath()%>/css/admin-${version}.css"
              type="text/css">
        <title></title>
    </head>
</head>
<body>

<page:applyDecorator page="/WEB-INF/decorators/adminSubNav.jsp"
                     name="adminSubNav" />
<div class="container center-block .col-md-8">
    <div><a href="<%=request.getContextPath()%>/admin/manageRoles.do">< Back</a></div>
    <div class="page-header">
        <h1><div id="roleName">${model.roleName} </div><small>Manage role</small></h1>
    </div>

    <%--<ul class="nav nav-tabs">--%>
        <%--<li class="active"><a href="#usersTab">Users</a></li>--%>
        <%--<li><a href="#privilegesTab">Privileges</a></li>--%>
    <%--</ul>--%>

    <%--<div class="tab-content">--%>
        <%--<div class="tab-pane active" id="usersTab"><h2>Users</h2></div>--%>
        <%--<div class="tab-pane" id="privilegesTab"><h2>Privileges</h2></div>--%>
    <%--</div>--%>

    <div>
        <h2>Users</h2>
        <div id="userSuccess" class="alert alert-success hide"> Successfully saved users</div>
        <div id="userFailed" class="alert alert-error hide"> Failed to save users</div>
        <b>Users that have this role:</b>
        <select id="userList" type="text" placeholder="Edit users" multiple>
            <c:forEach var="user" items="${model.allUsers}">
                <option <c:if test="${model.users.contains(user)}">selected</c:if> value='<c:out value="${user}"/>'><c:out value="${user}"/></option>
            </c:forEach>
        </select>
        <button id="userSave" class="btn" disabled>Save</button>
    </div>
    <div>
        <h2>Privileges</h2>
        <div id="privilegeSuccess" class="alert alert-success hide"> Successfully saved privileges</div>
        <div id="privilegeFailed" class="alert alert-error hide"> Failed to save privileges</div>
        <b>Privileges granted by this role:</b>
        <select id="privilegeList" type="text" placeholder="Edit privileges" multiple>
            <c:forEach var="privilege" items="${model.allPrivileges}">
                <option <c:if test="${model.privileges.contains(privilege)}">selected</c:if> value='<c:out value="${privilege}"/>'><c:out value="${privilege}"/></option>
            </c:forEach>
        </select>
        <button id="privilegeSave" class="btn" disabled>Save</button>
    </div>

</div>
<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/resources/js/chosen.jquery.min.js"></script>
<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/scripts/admin/common-${version}.js"></script>
<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/scripts/admin/roleDetails-${version}.js"></script>
</body>
</html>