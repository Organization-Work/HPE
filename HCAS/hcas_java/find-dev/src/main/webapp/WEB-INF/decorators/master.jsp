<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="sitemesh-decorator" prefix="decorator"%>
<%@ taglib uri="sitemesh-page" prefix="page"%>
<%@ taglib prefix="json" uri="/WEB-INF/tld/json.tld" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@'idol.properties'['discover.version']" var="version"/>
<!doctype html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
<meta http-equiv="X-UA-Compatible" content="IE=edge" />

<title><decorator:title /></title>
<link rel="shortcut icon" type="image/x-icon" href="<%=request.getContextPath()%>/img_logos/cropped-vertica-favicon-1-32x32-${version}.png"/>

<!-- [styles] -->
<link rel="stylesheet"
	href="<%=request.getContextPath()%>/css/bootstrap.min-${version}.css"
	type="text/css">
<!-- <link rel="stylesheet"
	href="https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/2.3.2/css/bootstrap.min.css"
	type="text/css"> -->
<link rel="stylesheet"
	href="<%=request.getContextPath()%>/css/typeaheadjs-${version}.css"
	type="text/css">	
<link rel="stylesheet"
    href="<%=request.getContextPath()%>/css/bootstrap-multiselect-${version}.css"
    type="text/css">
    <link rel="stylesheet"
          href="<%=request.getContextPath()%>/css/bootstrap-datetimepicker.min-${version}.css"
          type="text/css">
<link rel="stylesheet"
	href="<%=request.getContextPath()%>/css/fuelux_preloader-${version}.css" type="text/css">

<link rel="stylesheet"
	href="<%=request.getContextPath()%>/css/fuelux_tree-${version}.css" type="text/css">
   
<link rel="stylesheet"
	href="<%=request.getContextPath()%>/css/layout-${version}.css" type="text/css">
<link rel="stylesheet"
	href="<%=request.getContextPath()%>/css/jquery.infonomy-${version}.css"
	type="text/css">

<decorator:head />
</head>
<body class="body search">

    <div class="cookieMessage alert alert-info hidden"> [Cookie Message]. <a class="close">&times;</a> </div>

    <div class="modal hide help-about" id="helpAboutDialog">
        <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
            <spring:eval expression="@'idol.properties'['discover.showBrandLogo']" var="showBrandLogo"/>
            <c:if test="${ showBrandLogo }">
            	<span class="brand"><img src="<%=request.getContextPath()%>/img_logos/verticalogo-${version}.png" alt="HPE Logo"/> <b><decorator:title /></b></span>
            </c:if>
        </div>
        <div class="modal-body">
            <p><b>Version:</b> ${ config.version }</p>    
        </div>
        <div class="modal-footer">
            <a href="#" class="btn btn-primary" data-dismiss="modal">OK</a>
        </div>
       
    </div>

	<div class="navbar navbar-fixed-top">
		<div class="navbar-inner">
			<spring:eval expression="@'idol.properties'['discover.showBrandLogo']" var="showBrandLogo"/>
			<c:if test="${ showBrandLogo }">
				<span class="brand"><img src="<%=request.getContextPath()%>/img_logos/verticalogo-${version}.png" alt="HPE Logo"/> <decorator:title /></span>
			</c:if>
			<page:applyDecorator page="/WEB-INF/decorators/navigation.jsp"
				name="navigation" />

			<div style="display: none; line-height: 37px; font-weight: bold; color: red; float: left; font-size: 18px;">
				Internal BETA
			</div>

			<ul class="nav nav-right">
                  <li class="dropdown">
                    <a class="dropdown-toggle" data-toggle="dropdown" href="#">Help</a>
                    <ul class="dropdown-menu" role="menu">
                        <li><a href="#" id='helpButton' tabindex="-1">Help Discover</a></li>
                        <li class="divider"></li>
                        <li><a tabindex="-1" href="#" id="helpAbout">About Discover</a></li>
                    </ul>
                </li>
                <li><a class="userUsername"> <i class="icon-white icon-user"></i> <%=session.getAttribute("username")%> </a></li>
                <li><a class="navLink" data-logout="true" href="<%=request.getContextPath()%>/logout.do"><i class="icon-white icon-off"></i> Logout</a></li>
			</ul>
		</div>
	</div>
	<spring:eval expression="@'idol.properties'['discover.helpFile']" var="helpFile"/>
	<script type="text/javascript">
    	var HelpFile = "${helpFile}";       
    </script>
   <%--  <script src="<c:url value="/scripts/biscuits.js"/>"></script>
    <script src="<c:url value="/scripts/jquery-1.10.2.min.js"/>"></script>
    <script src="<c:url value="/scripts/jquery-migrate-1.2.1.min.js"/>"></script>
	<script src="<c:url value="/scripts/jquery.json-2.3.min.js"/>"></script>
    <script src="<c:url value="/scripts/jquery.livequery.js"/>"></script>
	<script src="<c:url value="/scripts/jquery.toggler.js"/>"></script>
    <script src="<c:url value="/scripts/cookieMessage.js"/>"></script>
	<script src="<c:url value="/scripts/infonomy/jquery.infonomy.js"/>"></script>
	<script src="<c:url value="/scripts/underscore.js"/>"></script>
	<script src="<c:url value="/scripts/underscore.partition.js"/>"></script>
    <script src="<c:url value="/scripts/bootstrap-2.3.2.min.js"/>"></script>
    <script src="<c:url value="/scripts/typeahead.bundle.js"/>"></script>
    <script src="<c:url value="/scripts/bootstrap-multiselect.js"/>"></script>
    <script src="<c:url value="/scripts/bootstrap-datetimepicker.min.js"/>"></script>
	<script src="<c:url value="/scripts/html5shiv.js"/>"></script>
	<script src="<c:url value="/scripts/hash.js"/>"></script>
	<script src="<c:url value="/scripts/app.js"/>"></script> --%>
	
	 <script src="<%=request.getContextPath()%>/scripts/biscuits-${version}.js"/></script>
	 <script src="<%=request.getContextPath()%>/resources/js/jquery-1.10.2.min.js"/></script>
	 <script src="<%=request.getContextPath()%>/resources/js/jquery-migrate-1.2.1.min.js"/></script>
	 <script src="<%=request.getContextPath()%>/scripts/jquery.json-2.3.min-${version}.js"/></script>
	 <script src="<%=request.getContextPath()%>/scripts/jquery.livequery-${version}.js"/></script>
	 <script src="<%=request.getContextPath()%>/scripts/jquery.toggler-${version}.js"/></script>
	 <script src="<%=request.getContextPath()%>/scripts/cookieMessage-${version}.js"/></script>
	 <script src="<%=request.getContextPath()%>/scripts/infonomy/jquery.infonomy-${version}.js"/></script>
	 <script src="<%=request.getContextPath()%>/scripts/underscore-${version}.js"/></script>
	 <script src="<%=request.getContextPath()%>/scripts/underscore.partition-${version}.js"/></script>
	 <script src="<%=request.getContextPath()%>/scripts/bootstrap-2.3.2.min-${version}.js"/></script>
	 <script src="<%=request.getContextPath()%>/scripts/typeahead.bundle-${version}.js"/></script>
	 <script src="<%=request.getContextPath()%>/scripts/bootstrap-multiselect-${version}.js"/></script>
	 <script src="<%=request.getContextPath()%>/scripts/bootstrap-datetimepicker.min-${version}.js"/></script>
	 <script src="<%=request.getContextPath()%>/scripts/html5shiv-${version}.js"/></script>
	 <script src="<%=request.getContextPath()%>/scripts/hash-${version}.js"/></script>
	 <script src="<%=request.getContextPath()%>/scripts/app-${version}.js"/></script>
	 <script>var CONTEXT_PATH = "${pageContext.request.contextPath}"</script>
	
	
    <script>var CONTEXT_PATH = "${pageContext.request.contextPath}"</script>
    <div class="content scrollableToTop">
		<decorator:body />
	</div>

	<div id="scrollToTop">
		<a class="btn nav-btn" href="#"><i class=icon-chevron-up></i></a>
	</div>
	
	
<style>
	.navbar-inner{
	height: 40px;
    padding-right: 20px;
    padding-left: 20px;
    background-color: #2c2c2c;
    background-image: -moz-linear-gradient(top,#333,#222);
    background-image: -ms-linear-gradient(top,#333,#222);
    background-image: -webkit-gradient(linear,0 0,0 100%,from(#333),to(#222));
    background-image: -webkit-linear-gradient(top,#333,#222);
    background-image: -o-linear-gradient(top,#333,#222);
    background-image: linear-gradient(top,#333,#222);
    background-repeat: repeat-x;
    -webkit-border-radius: 4px;
    -moz-border-radius: 4px;
    border-radius: 4px;
    filter: progid:dximagetransform.microsoft.gradient(startColorstr='#333333',endColorstr='#222222',GradientType=0);
    -webkit-box-shadow: 0 1px 3px rgba(0,0,0,0.25), inset 0 -1px 0 rgba(0,0,0,0.1);
    -moz-box-shadow: 0 1px 3px rgba(0,0,0,0.25),inset 0 -1px 0 rgba(0,0,0,0.1);
    box-shadow: 0 1px 3px rgba(0,0,0,0.25), inset 0 -1px 0 rgba(0,0,0,0.1);
}
.navbar .nav>li>a{
	text-shadow:none;
}
.navbar .nav>li>a:hover{
	color:#FFF;
}
li.active > .navLink {
    background: #222 !important;
	color:#FFF!important;
}
.navbar .brand{
	color:#999;
	text-shadow:none;
}
.icon-tag {
    background-position: 0 -48px !important;
}
.icon-hdd {
    background-position: 0 -144px !important;
}
.icon-chevron-up {
    background-position: -288px -120px !important;
}
.icon-filter {
    background-position: -408px -144px !important;
}
.icon-folder-open {
    background-position: -408px -120px !important;
}
.icon-plus {
    background-position: -408px -96px!important;
}
.icon-remove {
    background-position: -312px 0!important;
}
.icon-refresh {
    background-position: -240px -24px!important;
}
.icon-signal {
    background-position: -408px 0!important;
}
.icon-tags {
    background-position: -25px -48px!important;
}
.icon-th {
    background-position: -240px 0!important;
}
.icon-off {
    background-position: -384px 0!important;
}
.icon-user {
    background-position: -168px 0!important;
}
.icon-eye-open {
    background-position: -96px -120px!important;
}
.icon-search {
    background-position: -48px 0!important;
}
.icon-arrow-right {
    background-position: -264px -96px!important;
}
.icon-chevron-down {
    background-position: -313px -119px!important;
}
.icon-list-alt {
    background-position: -264px -24px!important;
}
.icon-edit {
    background-position: -96px -72px!important;
}
.icon-trash {
    background-position: -456px 0!important;
}
	</style>
	
</body>
</html>
