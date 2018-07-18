jQuery(function($) {
    
    var fileItemTmpl = '<% _.each(files, function(file) { %> <li><a class="loglink" href="<%- file.fileUrl %>" title="size: <%- file.sizeVal %>"><%= file.filename %></a></li> <% }); %>';
    
    $('#auditFilesList').on('click', '.loglink', function() {
        $('.loglink.selected').toggleClass('selected', false);
        $(this).toggleClass('selected', true);
        $('#logContentFrame').attr('src', $(this).attr('href'));
        
        return false;
    });
    
    $('#auditRefreshBtn').click(function() {
        $('#logContentFrame').attr('src', 'about:blank');
        loadAuditLogs();
        
        return false;
    });
    
    function loadAuditLogs() {
        var $selectedLog = $('.loglink.selected');
        var lastSelectedLogName = $selectedLog.length > 0 ? $selectedLog.text() : null;
        
        $.ajax({
            dataType : 'json',
            type: 'GET',
            url: "getAuditLogs.do",
        }).done(function(data){
            if (data && !_.isEmpty(data)) {
                var sortedData = _.sortBy(data, 'lastModified').reverse();
                                
                var filesList = _.template(fileItemTmpl, {files: sortedData});
                
                $('#logContentFrame').contents().find('html').html('<span style="color: #3a87ad;"> [Select a log file to view]</span>');
                $('#auditFilesList').empty().append(filesList);
                
                if (lastSelectedLogName) {
                    $('.loglink').each(function() {
                        var $link = $(this);
                        if ($link.text() === lastSelectedLogName) {
                            setTimeout(function () {
                                $('#logContentFrame').attr('src', $link.attr('href'));
                                $link.toggleClass('selected', true);
                            }, 10);                            

                            return false;
                        }
                    });
                }
                
            } else {
                $('#logContentFrame').contents().find('html').html('<span style="color: #3a87ad;"> [No log file found in the web application logs directory.]</span>');
            }
            
            
        }).fail(function() {
            $('#logContentFrame').contents().find('html').html('<span style="color: red;"> Failed to load files.</span>');
        });
    }
    
    loadAuditLogs();
    
});