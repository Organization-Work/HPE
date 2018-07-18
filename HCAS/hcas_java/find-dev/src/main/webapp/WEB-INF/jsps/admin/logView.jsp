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


<page:applyDecorator page="/WEB-INF/decorators/adminSubNav.jsp"
                     name="adminSubNav" />
<div id="logviewMain">
    <div class="logview-container">
        <div class="logview-filelist">
            <div class="header">
                <span class="header">Audit Logs: </span>
                <button class="btn btn-mini btn-info pull-right" id="auditRefreshBtn" title="Refresh files list"><i class="icon-white icon-refresh"></i> Refresh</button>
            </div>
            <ul id="auditFilesList">
            </ul>
            
        </div>
        
        <div class="logview-filecontent">
            <iframe id="logContentFrame"></iframe>
        </div>
    
    </div>
</div>

<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/scripts/admin/common-${version}.js"></script>
<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/scripts/admin/logView-${version}.js"></script>
<script>

</script>
</body>
</html>