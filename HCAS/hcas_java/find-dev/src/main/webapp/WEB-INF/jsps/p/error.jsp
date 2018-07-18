<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="sitemesh-decorator" prefix="decorator"%>
<%@ taglib uri="sitemesh-page" prefix="page"%>
<%@ taglib prefix="json" uri="/WEB-INF/tld/json.tld" %>
<spring:eval expression="@'idol.properties'['discover.version']" var="version"/>

<html>
<head>
<title>Server Error</title>
<!-- <link rel="stylesheet" href="<c:url value="/css/bootstrap.min.css"/>" type="text/css">  --> 
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/2.3.2/css/bootstrap.min.css" type="text/css">   

<%-- <script src="<c:url value="/scripts/jquery-1.10.2.min.js"/>"></script>
<script src="<c:url value="/scripts/jquery-migrate-1.2.1.min.js"/>"></script>
<script src="<c:url value="/scripts/jquery.json-2.3.min.js"/>"></script>
<script src="<c:url value="/scripts/underscore.js"/>"></script>
<script src="<c:url value="/scripts/bootstrap-2.3.2.min.js"/>"></script> --%>


		<script src="<%=request.getContextPath()%>/resources/js/jquery-1.10.2.min.js"/></script>
		<script src="<%=request.getContextPath()%>/resources/js/jquery-migrate-1.2.1.min.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/jquery.json-2.3.min-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/underscore-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/bootstrap-2.3.2.min-${config.version}.js"/></script>

<style>
	div.error {
		margin: 20px; 
	}
	.error-message {
		padding-top: 5px;
		color: red;
		font-size: 15px;
	}
	
</style>

</head>
<body>
	<div class="error">
		<h2>Server error encountered:</h2>
		<p class="error-message"></p>
	</div>
	
<script type="text/javascript">
	var errorDetails = ${json:toJSON(errorDetails)};
	var errMsg = errorDetails.errorString || errorDetails.message;
	$('.error-message').html(_.escape(errMsg));
</script>
</body>
</html>