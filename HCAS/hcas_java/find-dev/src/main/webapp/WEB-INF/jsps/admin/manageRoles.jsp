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

    <link rel="stylesheet"
          href="<%=request.getContextPath()%>/css/admin-${version}.css"
          type="text/css">
    <link rel="stylesheet"
          href="<%=request.getContextPath()%>/css/bootstrap-tagsinput-${version}.css"
          type="text/css">
</head>
<body>

<div id="confirmDeleteModal" class="modal hide">
    <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>
        <b>Delete Role</b></span>
    </div>
    <div class="modal-body">
        <p>Are you sure you would like to delete the <b id="role-delete-name-modal">selected</b> role?</p>
    </div>
    <div class="modal-footer">
        <a href="#" id="model-delete-confirm" class="btn btn-danger" data-dismiss="modal">Yes</a>
        <a href="#" class="btn btn-primary" data-dismiss="modal">No</a>
    </div>
</div>

<div id="createNewRoleModal" class="modal hide">
    <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>
        <b>Discover Admin Console</b>
    </div>
    <div class="modal-body">
        <form id="modal-form" accept-charset="UTF-8">
            <input name="name"/>
        </form>
    </div>
    <div class="modal-footer">
        <a href="#" id="new-role-submit" class="btn btn-danger" data-dismiss="modal">Accept</a>
        <a href="#" class="btn btn-primary" data-dismiss="modal">Cancel</a>
    </div>
</div>


<page:applyDecorator page="/WEB-INF/decorators/adminSubNav.jsp"
                     name="adminSubNav" />

<div class="container center-block .col-md-8">
    <div class="page-header">
        <h1>Roles <small>Manage roles</small></h1>
    </div>

    <script id="privilegeRowTemplate" type="text/template">
        <tr class="privilegeRow">
            <td class="privilegeName nameRow"></td>
            <td class="privilegeManage actionsColumn"><a class="btn btn-success">Manage</a></td>
        </tr>
    </script>

    <script id="roleRowTemplate" type="text/template">
        <tr class="roleRow">
            <td class="roleName nameRow"></td>
            <td class="actionsColumn"><a class="btn btn-danger delete-role">Delete</a>  <a class="btn btn-success manage-role">Manage</a></td>
        </tr>
    </script>


    <ul class="nav nav-tabs">
        <li class="active"><a class="tabSelector" href="#rolesTab" data-toggle="tab">Roles</a></li>
        <li><a class="tabSelector" href="#privilegesTab" data-toggle="tab">Privileges</a></li>
    </ul>

    <!-- Tab panes -->
    <div id="failedToDeleteRole" class="alert alert-error hide">Failed to delete role</div>
    <div id="deletedRole" class="alert alert-success hide">Deleted role</div>
    <div class="tab-content">
        <div class="tab-pane active" id="rolesTab">
            <button id="newRole" href="#" class="btn btn-default pull-right">Create new role</button>
            <h2> Roles </h2>
            <table id="rolesTable" class="table">
                <thead>
                <tr>
                    <th>Role Name</th>
                    <th class="actionsColumn">Actions</th>
                </tr>
                </thead>
                <tbody id="rolesList">
                </tbody>
            </table>
        </div>
        <div class="tab-pane" id="privilegesTab">
            <h2> Privileges </h2>
            <table id="privilegeTable" class="table">
                <thead>
                <tr>
                    <th>Privilege Name</th>
                    <th class="actionsColumn">Actions</th>
                </tr>
                </thead>
                <tbody id="privilegeList">
                </tbody>
            </table>
        </div>
    </div>

    <div class="modal fade" id="newUser" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title">Create New User.</h4>
            </div>
            <div class="modal-body">

                <span id="createdUser" class="hide alert alert-success createAlert">Saved user</span>
                <span id="failedToCreateUser" class="hide alert alert-danger createAlert">Failed to saved user</span>

                <form role="form-horizontal">
                    <div class="form-group">
                        <label for="newusername">Username</label>
                        <input type="text" class="form-control" id="newUsername" placeholder="Username">
                    </div>
                    <div class="form-group">
                        <label for="newuserpassword">Password</label>
                        <input type="text" class="form-control" id="newUserPassword" placeholder="Password">
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-primary" id="createNewUser">Create</button>
                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
            </div>
        </div>
    </div>
</div>


<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/scripts/admin/common-${version}.js"></script>
<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/scripts/admin/adminRoles-${version}.js"></script>
<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/scripts/bootstrap-tagsinput-${version}.js"></script>
</body>
</html>