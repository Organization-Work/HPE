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
<!-- 		<script>
		var versionGlyphWhiteicons = $('#glyphiconWhiteImg').val();
		//alert(versionGlyphWhiteicons + "version");
		console.log("versionGlyphWhiteicons" + versionGlyphWhiteicons)
		$('head').append('<style>[class^="icon-"],[class*=" icon-"]{background: url(../img/'+ versionGlyphWhiteicons + ')!important}</style>');
		
		$('head').append('<style>.icon-white,.nav-pills>.active>a>[class^="icon-"],.nav-pills>.active>a>[class*=" icon-"],.nav-list>.active>a>[class^="icon-"],.nav-list>.active>a>[class*=" icon-"],.navbar-inverse .nav>.active>a>[class^="icon-"],.navbar-inverse .nav>.active>a>[class*=" icon-"],.dropdown-menu>li>a:hover>[class^="icon-"],.dropdown-menu>li>a:focus>[class^="icon-"],.dropdown-menu>li>a:hover>[class*=" icon-"],.dropdown-menu>li>a:focus>[class*=" icon-"],.dropdown-menu>.active>a>[class^="icon-"],.dropdown-menu>.active>a>[class*=" icon-"],.dropdown-submenu:hover>a>[class^="icon-"],.dropdown-submenu:focus>a>[class^="icon-"],.dropdown-submenu:hover>a>[class*=" icon-"],.dropdown-submenu:focus>a>[class*=" icon-"]{background: url(../img/'+ versionGlyphWhiteicons + ')!important}</style>');
		$('head').append('<style>.icon-white,.nav-pills>.active>a>[class^="icon-"],.nav-pills>.active>a>[class*=" icon-"],.nav-list>.active>a>[class^="icon-"],.nav-list>.active>a>[class*=" icon-"],.navbar-inverse .nav>.active>a>[class^="icon-"],.navbar-inverse .nav>.active>a>[class*=" icon-"],.dropdown-menu>li>a:hover>[class^="icon-"],.dropdown-menu>li>a:focus>[class^="icon-"],.dropdown-menu>li>a:hover>[class*=" icon-"],.dropdown-menu>li>a:focus>[class*=" icon-"],.dropdown-menu>.active>a>[class^="icon-"],.dropdown-menu>.active>a>[class*=" icon-"],.dropdown-submenu:hover>a>[class^="icon-"],.dropdown-submenu:focus>a>[class^="icon-"],.dropdown-submenu:hover>a>[class*=" icon-"],.dropdown-submenu:focus>a>[class*=" icon-"]{background: url(../img/'+ versionGlyphWhiteicons + ')!important}</style>')
		</script>  -->  
</head>
<body>
<input type="hidden" id="glyphiconWhiteImg" value="glyphicons-halflings-white-${version}.png">

<page:applyDecorator page="/WEB-INF/decorators/adminSubNav.jsp"
                     name="adminSubNav" />

<div class="container center-block .col-md-8">
    <div class="page-header">
        <h1>Users <small>View all users</small></h1>
    </div>
    
   <%--  <input type="hidden" id="buildVersion_asc" value="sort_asc-${version}.png">
    <input type="hidden" id="buildVersion_desc" value="sort_desc-${version}.png">
    <input type="hidden" id="buildVersion_both" value="sort_both-${version}.png"> --%>
        
    <c:if test="${model.autonomyRepository}">
        
	    <div class="btn-group pull-right">
	        <a class="btn dropdown-toggle" data-toggle="dropdown" href="#">
	            Action
	            <span class="caret"></span>
	        </a>                   
			<ul class="dropdown-menu">
			  <li><a href="#" data-toggle="modal" data-target="#newUser">Create New User</a></li>
			</ul>
	    </div>
    
    </c:if>
    
    <table id="userTable" class="table table-bordered table-striped">
        <thead>
        <tr>
            <th>ID</th>
            <th>Username</th>
            <th>Locked</th>
            <th>Last Time Locked</th>
            <th>Max Agents</th>
            <th>Num Agents</th>
            <th>Last Time Logged In</th>
            <th>More Details</th>
        </tr>
        </thead>
        <tbody>
        </tbody>
    </table>
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

            <form id="newUserForm" role="form-horizontal">
                <div class="form-group">
                    <label for="newusername">Username</label>
                    <input type="text" class="form-control" id="newUsername" placeholder="Username">
                </div>
                <div class="form-group">
                    <label for="newuserpassword">Password</label>
                    <input type="password" class="form-control" id="newUserPassword" placeholder="Password">
                </div>
				<div class="form-group">
                    <label for="newuserconfirmpassword">Confirm Password</label>
                    <input type="password" class="form-control" id="newUserConfirmPassword" placeholder="Confirm Password">
                </div>
                <div class="form-group">
                    <label for="newUserFirstName">First Name</label>
                    <input type="text" class="form-control" id="newUserFirstName" placeholder="First name (optional)">
                </div>
                <div class="form-group">
                    <label for="newUserLastName">Last Name</label>
                    <input type="text" class="form-control" id="newUserLastName" placeholder="Last name (optional)">
                </div>
                <div class="form-group">
                    <label for="newUserEmail">Email</label>
                    <input type="text" class="form-control" id="newUserEmail" placeholder="Email (optional)">
                </div>
            </form>
        </div>
        <div class="modal-footer">
            <button type="button" class="btn btn-primary" id="createNewUser">Create</button>
            <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
        </div>
    </div>
</div>

<div class="modal fade" id="superUserChangePassword" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
    <div class="modal-content">
        <div class="modal-header">
            <h4 class="modal-title">You have not yet changed your superuser password, please change it before continuing.</h4>
        </div>
        <div class="modal-body">
            <span id="failedToChangeSuper" class="hide alert alert-danger createAlert">Failed to update password</span>


            <form id="superPasswordForm" role="form-horizontal">
                <div class="form-group">
                    <label for="newuserpassword">Password</label>
                    <input type="password" class="form-control" id="newSuperPassword" placeholder="Password">
					<label for="newuserconfirmpassword">Confirm Password</label>
                    <input type="password" class="form-control" id="newSuperConfirmPassword" placeholder="Confirm Password">
                </div>
            </form>
            <span id="superResetConfirm" class="hide">Reset superuser password, please note down this password and click logout.</span>
        </div>

        <div class="modal-footer">
            <a type="button" class="btn btn-primary" id="updateSuperPassword">Set</a>
            <a type="button" class="btn btn-success hide" href="<%=request.getContextPath()%>/logout.do" id="updateSuperPasswordLogout">Logout</a>
        </div>
    </div>
</div>

<script>var SSO_ON = "${model.ssoOn}"</script>
<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/scripts/jquery.dataTables.min-${version}.js"></script>
<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/scripts/admin/validatePassword-${version}.js"></script>
<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/scripts/admin-${version}.js"></script>



</body>

</html>