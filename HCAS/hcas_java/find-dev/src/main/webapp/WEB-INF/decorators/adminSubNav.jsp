<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="subnav" role="navigation">
    <ul class="nav nav-pills">
        <li><a href="<%=request.getContextPath()%>/admin/adminview.do">Users</a></li>
        <li><a href="<%=request.getContextPath()%>/admin/manageRoles.do">Roles</a></li>
        <li class="pull-right"><a id="viewLogLink" href="<%=request.getContextPath()%>/admin/logView.do">Logs</a></li>
    </ul>

    <script>
        // Stop enter form closing modals when they contain a form
        $(document).ready(function() {
            $(window).keydown(function(event){
                if(event.keyCode == 13) {
                    event.preventDefault();
                    return false;
                }
            });
        });
    </script>
</div>