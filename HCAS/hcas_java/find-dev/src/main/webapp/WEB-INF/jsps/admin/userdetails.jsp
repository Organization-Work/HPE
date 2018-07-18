<%--
  Created by IntelliJ IDEA.
  User: nathan
  Date: 26/03/14
  Time: 13:59
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="sitemesh-decorator" prefix="decorator"%>
<%@ taglib uri="sitemesh-page" prefix="page"%>
<spring:eval expression="@'idol.properties'['discover.version']" var="version"/>

<html>
<head>
    <title>Discover Admin Console</title>
    <link rel="stylesheet"
          href="<%=request.getContextPath()%>/css/admin-${version}.css"
          type="text/css">

    <link rel="stylesheet"
          href="<%=request.getContextPath()%>/resources/css/chosen.min.css"
          type="text/css">
</head>
<body>

<div class="modal fade" id="resetPasswordModal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
    <div class="modal-content">
        <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
            <h4 class="modal-title">Set Password</h4>
        </div>
        <div id="newPasswordWarning" class="alert alert-error hide">
            <ul id="newPasswordWarningList">
            </ul>
        </div>
        <div class="modal-body">
            <form role="form-horizontal">
                <div class="form-group">
                    <label for="newPassword">New Password</label>
                    <input type="password" class="form-control" id="newPassword" placeholder="Password">
					<label for="newConfirmPassword">Confirm Password</label>
                    <input type="password" class="form-control" id="newConfirmPassword" placeholder="Confirm Password">
                </div>
            </form>
        </div>
        <div class="modal-footer">
            <button type="button" class="btn btn-primary" id="resetPassword">Set</button>
            <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
        </div>
    </div>
</div>


<%
    String username;
    if(request.getParameter("username") == null) {
        username = "No user found";
    } else {
        username = request.getParameter("username");
    } %>

<page:applyDecorator page="/WEB-INF/decorators/adminSubNav.jsp"
                     name="adminSubNav" />
<div class="container center-block .col-md-9">
    <div><a href="<%=request.getContextPath()%>/admin/adminview.do">< Back</a></div>
    <div class="page-header">
        <h1><c:out value="${model.username}"/> <small>See user's information</small></h1>
    </div>
    <div id="actionSuccess" class="alert alert-success hide">
        <ul id="actionSuccessList">

        </ul>
    </div>
    <div class="row">
        <div class="user-details">
            <div class="user-details">
                <form role="form" class="form-horizontal">
                    <div class="control-group">
                        <label class="control-label" for="uid">UID</label>
                        <div id="uid" placeholder="uid"></div>
                    </div>
                    <div class="control-group">
                        <label class="control-label" for="username">Username</label>
                        <div class="controls">
                            <div type="text" class="form-control" id="username"><%=username%></div>
                        </div>
                    </div>
                    <div class="control-group">
                        <label class="control-label" for="firstName">First Name</label>
                        <div class="controls">
                            <div type="text" class="form-control" id="firstName" placeholder="Name" value="x, y, z"></div>
                        </div>
                    </div>
                    <div class="control-group">
                        <label class="control-label" for="lastName">Last Name</label>
                        <div class="controls">
                            <div type="text" class="form-control" id="lastName" placeholder="Name" value="x, y, z"></div>
                        </div>
                    </div>
                    <div class="control-group">
                        <label class="control-label" for="email">Email</label>
                        <div class="controls">
                            <div type="text" class="form-control" id="email" placeholder="Name" value="x, y, z"></div>
                        </div>
                    </div>
                    <div class="control-group">
                        <label class="control-label" for="locked">Locked</label>
                        <div class="controls">
                            <div type="text" class="form-control" id="locked" placeholder="Locked" value=""></div>
                        </div>
                    </div>
                    <div class="control-group">
                        <label class="control-label" for="lastlogged">Last Time User Logged In</label>
                        <div class="controls">
                            <div type="text" class="form-control" id="lastlogged" placeholder="Name" value="x, y, z"></div>
                        </div>
                    </div>
                    <div id="roleSaveSuccess" class="alert alert-success hide"> Successfully saved roles</div>
                    <div id="roleSaveFailed" class="alert alert-error hide">
                        <c:choose>
	                        <c:when test="${model.username == 'superuser'}">
	                            Superuser roles can not be modified.
	                        </c:when>
	                        <c:otherwise>
	                           This user can not have protected roles.
	                        </c:otherwise>
                        </c:choose>
                    </div>
                    <b>Users Roles:</b>
                    <select id="rolesList" type="text" placeholder="Edit roles" multiple>
                        <c:forEach var="role" items="${model.allRoles}">
                            <option <c:if test="${model.roles.contains(role)}">selected</c:if> value='<c:out value="${role}"/>'><c:out value="${role}"/></option>
                        </c:forEach>
                    </select>
                    <button id="savePrivilege" class="btn btn-default" disabled>Save</button>

                </form>
            </div>
        </div>
        <div class="actions col-md-2">         
        
	         <div class="btn-group pull-right">
	             <a class="btn dropdown-toggle" data-toggle="dropdown" href="#">
	                 Action
	                 <span class="caret"></span>
	             </a>
	             <ul class="dropdown-menu">
	                 <c:if test="${model.autonomyRepository}">
	                    <li><a id="deleteUser">Delete User</a></li>
	                    <li><a id="showresetPasswordModal">Change Password</a></li>
	                 </c:if>
	                 <li><a id="showUpdateUserModal">Update User</a></li>
	                 <li><a id="showUnlockUserModal"><c:choose> <c:when test="${model.locked}"> Unlock User</c:when><c:otherwise>Lock User</c:otherwise></c:choose></a></li>
	                 
	             </ul>
	         </div>        
            
        </div>
    </div>

    <div class="modal fade" id="updateUserModal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title">Update User.</h4>
            </div>
            <div class="modal-body">

                <span id="updateUserAlert" class="hide alert alert-success createAlert">Saved user</span>
                <span id="failedToUpdateUserAlert" class="hide alert alert-danger createAlert">Failed to saved user</span>

                <form id="updateUserForm" role="form-horizontal">
                    <div class="form-group">
                        <label for="newUserFirstName">First Name</label>
                        <input type="text" class="form-control" id="newUserFirstName" name="firstname" placeholder="First name (optional)">
                    </div>
                    <div class="form-group">
                        <label for="newUserLastName">Last Name</label>
                        <input type="text" class="form-control" id="newUserLastName" name="lastname" placeholder="Last name (optional)">
                    </div>
                    <div class="form-group">
                        <label for="newUserEmail">Email</label>
                        <input type="text" class="form-control" id="newUserEmail" name="email" placeholder="Email (optional)">
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-primary" id="updateUserButton">Update</button>
                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
            </div>
        </div>
    </div>

    <div id="confirmDeleteModal" class="modal hide">
        <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>
            <b>Discover Admin Console</b></span>
        </div>
        <div class="modal-body">
            <p>Are you sure you would like to delete the this user?</p>
        </div>
        <div class="modal-footer">
            <a href="#" id="model-delete-confirm" class="btn btn-danger" data-dismiss="modal">Yes</a>
            <a href="#" class="btn btn-primary" data-dismiss="modal">No</a>
        </div>
    </div>
 <div id="unlockModal" class="modal hide">
        <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>
            <b>Discover Admin Console</b></span>
        </div>
        <div class="modal-body">
        
             <p>Are you sure you would like to <span id="unlock-user"></span> this user?</p>
            
        </div>
        <div class="modal-footer">
            <a href="#" id="model-update" class="btn btn-danger" data-dismiss="modal">Yes</a>
            <a href="#" class="btn btn-primary" data-dismiss="modal">No</a>
            
        </div>
    </div>



</div>
<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/resources/js/chosen.jquery.min.js"></script>
<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/scripts/validate.min-${version}.js"></script>
<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/scripts/admin/validatePassword-${version}.js"></script>
<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/scripts/admin/common-${version}.js"></script>
<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/scripts/userDetails-${version}.js"></script>

</body>
</html>