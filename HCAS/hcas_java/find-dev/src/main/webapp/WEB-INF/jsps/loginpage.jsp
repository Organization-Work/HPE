<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>

<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="X-UA-Compatible" content="IE=Edge">
    <title>Discover &mdash; Login</title>
    <link rel="shortcut icon" type="image/x-icon" href="<%=request.getContextPath()%>/img_logos/cropped-vertica-favicon-1-32x32-${config.version}.png"/>
    <!-- [styles] -->
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/bootstrap.min-${config.version}.css" type="text/css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/2.3.2/css/bootstrap.min.css" type="text/css">
    <link rel="stylesheet"
          href="<%=request.getContextPath()%>/css/layout-${config.version}.css" type="text/css">
    <link rel="stylesheet"
          href="<%=request.getContextPath()%>/css/jquery.infonomy-${config.version}.css"
          type="text/css">
    <link rel="stylesheet"
          href="<%=request.getContextPath()%>/css/loginpage-${config.version}.css"
          type="text/css">
  
</head>
<body>
<div class="login-parent">

    <div class="cookieMessage alert alert-info hidden"> <span class='text'></span> </div>

    <div class="activityIndicator">
        <div class="working">Working&hellip;
            <div class="progress progress-striped active">
                <div class="bar"></div>
            </div>
        </div>
    </div>
    <div class="loginform form form-vertical">
        <div>
            <h2 class="heading">
                <i class="icon icon-home"></i>
                Login to Discover
            </h2>
            <br>
        </div>
        <div class="userpassArea">
            <div>
                <p><label> UserName: <input type="text" name=�username� class="username span5"> </label></p>

                <p><label> Password: <input type="password" name=�password� class="password span5"> </label></p>
            </div>
            <div class="control">
                <div class="btn-group pull-right">
                    <button name =�submit� value=�submit� class="login btn btn-primary"><i class="icon icon-white icon-hand-right"></i> Login</button>
                    <button class="register btn btn-success"><i class="icon icon-white icon-thumbs-up"></i> Register
                    </button>
                </div>
                <button type="reset" class="reset btn"><i class="icon icon-remove"></i> Reset</button>
                <div class="alert alert-error loginError"> Action failed, please check your input and try again.</div>             
            </div>            
        </div>
        <button class="usesso ssoLogin btn btn-info btn-large"> Login with SSO&hellip; </button>
        <c:if test="${ config.displayTnCMessage }">
        	<div><BR>Please Note: By clicking on Login button you agree to the stated Terms and Conditions below</div>
        </c:if>
        
		<%-- <script src="<c:url value="/scripts/biscuits.js"/>"></script>
        <script src="<c:url value="/scripts/jquery-1.10.2.min.js"/>"></script>
        <script src="<c:url value="/scripts/jquery-migrate-1.2.1.min.js"/>"></script>
        <script src="<c:url value="/scripts/cookieMessage.js"/>"></script>
        <script src="<c:url value="/scripts/loginpage.js"/>"></script> --%>
        
        <script src="<%=request.getContextPath()%>/scripts/biscuits-${config.version}.js"/></script>
        <script src="<%=request.getContextPath()%>/resources/js/jquery-1.10.2.min.js"></script>
        <script src="<%=request.getContextPath()%>/resources/js/jquery-migrate-1.2.1.min.js"></script>
        <script src="<%=request.getContextPath()%>/scripts/cookieMessage-${config.version}.js"></script>
        <script src="<%=request.getContextPath()%>/scripts/loginpage-${config.version}.js"></script>

    </div>
    <c:if test="${ config.displayTnCMessage }">
        	<div class="loginmessage">        	
        	<b>Important Message on Privacy and Security</b>
        	<p><p>Patient identifiable information is viewable at the docview level within HCAS and in data export from HCAS. 
        	You may only access patients' clinical record if you are actively involved in their clinical care or as part of a quality improvement process 
        	which requires identified patient information. <p>Access for any other reason is unethical and may be considered professional misconduct.
			<p>All users of HCAS must adhere to their organization's privacy and security procedures.
			<p>All access to patient data is audited.        	
        	</div>
     </c:if>
</div>

</body>
</html>
