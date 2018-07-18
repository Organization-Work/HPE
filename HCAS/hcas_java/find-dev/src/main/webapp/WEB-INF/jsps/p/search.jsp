<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="sitemesh-decorator" prefix="decorator"%>
<%@ taglib uri="sitemesh-page" prefix="page"%>
<%@ taglib prefix="json" uri="/WEB-INF/tld/json.tld" %>


<html>
<head>
	
    <title>${ config.brandName }</title>
    <link rel="stylesheet" href="../css/search-${config.version}.css" type="text/css">
    <link rel="stylesheet" href="../css/colorbox-${config.version}.css" type="text/css">
    <link rel="stylesheet" href="../css/search.clusters-${config.version}.css" type="text/css">
    <link rel="stylesheet" href="../css/search.agents-${config.version}.css" type="text/css">
    <link rel="stylesheet" href="../css/search.results-${config.version}.css" type="text/css">
    <link rel="stylesheet" href="../css/search.sidebar-${config.version}.css" type="text/css">
    <c:if test="${ parametric.active }">
        <c:if test="${ searchConfig.showNetworkMap }">
    <link rel="stylesheet" href="../css/search.networkmap-${config.version}.css" type="text/css">
        </c:if>
    <link rel="stylesheet" href="../css/search.tableviewer-${config.version}.css" type="text/css">
    <link rel="stylesheet" href="../css/search.topicmap-${config.version}.css" type="text/css">
    <link rel="stylesheet" href="../css/search.sunburst-${config.version}.css" type="text/css">
    <link rel="stylesheet" href="../css/search.trending-${config.version}.css" type="text/css">
    <link rel="stylesheet" href="../css/search.timeline-${config.version}.css" type="text/css">
    <link rel="stylesheet" href="../resources/css/cupertino/jquery-ui-1.8.23.custom.css" type="text/css">
    </c:if>
    <link rel="stylesheet" href="../css/search.agent.edit-${config.version}.css" type="text/css">
    <link rel="stylesheet" href="../css/search.tree-${config.version}.css" type="text/css">
    <link rel="stylesheet" href="../css/search.filters-${config.version}.css" type="text/css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css">
    <link rel="stylesheet" href="../resources/js/jstree/themes/default/style.min.css" />
    <link rel="stylesheet" href="../css/search.filters-${config.version}.css" type="text/css">
    <link rel="stylesheet" href="../resources/css/chosen.min.css" type="text/css">
</head>
<body>

<input type="hidden" id="buildVersion" value="-${config.version}">
<!-- input type="hidden" id="buildVersionImg" value="blue-loading-${config.version}.gif"-->
<form  type="get" action="viewRecord.do" name="formxml">
<input type="hidden" name="firstname" id="firstname" value="-${config.version}">
<input type="hidden" name="onlyVersion" id="onlyVersion" value="-${config.version}">
</form>
<div class="sidebar">

		<div class="search-controls">
			<form id="searchControlForm" class="nonForm">
				<!--   <div class="search-mode">
                    <h2>Search: </h2>
                    <select title="Search mode">
                        <option value="AUTO">Auto</option>
                        <option value="MANUAL">Manual</option>
                    </select>
                </div> -->



				<div class="control-group search-views-control"></div>
				<span class="input-append"> <c:choose>
						<c:when test="${ searchConfig.textSearchEnabled }">
							<input id="searchArea" type="text" class="windowHash"
								placeholder="Type to find">
						</c:when>
						<c:otherwise>
							<input id="searchArea" type="text" class="windowHash"
								placeholder="Type to find" disabled>
						</c:otherwise>
					</c:choose>

					<button id="searchGo" class="btn btn-inverse" type="button">Go</button>
				</span>
				<button id="resetButton" class="btn btn-danger" type="reset">
					<i class="icon-white icon-remove"></i> Reset
				</button>

			</form>
			<!--  
			<div id="filterSyncError" class="hidden">
				Filters have been updated, click <em>'Go'</em> to refresh the
				results.
			</div>
			-->
		</div>
		<div class="nav-tabs-div">	
			<ul class="nav nav-tabs filter-tabs">
				<c:if test="${ parametric.active }">
					<li id="filtersTab" class="active"><a href="#advancedFilters"
						class="options-tab" data-toggle="tab"
						data-addcontrol="#filtersAddControl"> <i class="icon-filter"></i>
							Filters
					</a></li>
				</c:if>
	
				<li id="docfoldersTab"><a href="#documentfolders"
					class="options-tab" data-toggle="tab"
					data-addcontrol="#documentFoldersAddControl"> <i
						class="icon-folder-open"></i>Folders</a></li>
	
				<c:if test="${ config.useSearchSettings }">
	
					<li><a href="#settings" class="options-tab" data-toggle="tab">
							<i class="icon-chevron-down"></i> Settings
					</a></li>
				</c:if>
				<li class="pull-right">
					<div class="btn-group toolbar" id="filtersAddControl">
						<a id="filtersadd"
							class="btn btn-small dropdown-toggle fieldlist-control"
							data-toggle="dropdown" href="#"> <i class="icon icon-plus"></i>
							Add &hellip;
						</a>
	
						<ul class="dropdown-menu" id="field-list" role="menu"
							aria-labelledby="dLabel">
							<li><a class="ignore">Loading&hellip;</a></li>
						</ul>
					</div>
	
					<div class="btn-group toolbar hidden" id="documentFoldersAddControl">
						<a id="documentfoldersadd"
							class="btn btn-small dropdown-toggle docfolder-control" href="#">
							<i class="icon icon-plus"></i> Add &hellip;
						</a>
					</div>
	
				</li>
			</ul>
		</div>
		<div class="search-options shadow">

			<div class="search-filter-contents">

				<c:if test="${ parametric.active }">
					<page:applyDecorator name="search.advanced"
						page="/WEB-INF/decorators/search.advanced.jsp" />
				</c:if>
				<c:if test="${ config.useSearchSettings }">

					<page:applyDecorator name="search.settings"
						page="/WEB-INF/decorators/search.settings.jsp" />
				</c:if>
<!-- <div id="docFolderTree">Test</div> -->
			</div>
		</div>

		<div id="filtersContextMenu"
			class="dropdown clearfix filters-contextmenu hide">
			<ul class="dropdown-menu context-menu" role="menu">
				<li class="copy-cut"><a tabindex="-1" class="context-menu-item"
					data-action="copy" href="#"><i class="icon-book"></i>Copy</a></li>
				<li class="copy-cut"><a tabindex="-1" id="filtersCut"
					class="context-menu-item" data-action="cut" href="#"><i
						class="icon-random"></i>Cut</a></li>
				<li class="divider copy-cut"></li>
				<li><a tabindex="-1" id="filtersPaste"
					class="context-menu-item paste-item disabled" data-action="paste"
					href="#"><i class="icon-share-alt"></i>Paste</a></li>
				<li><a tabindex="-1" id="filtersGroupPaste"
					class="context-menu-item paste-item disabled"
					data-action="grouppaste" href="#"><i class="icon-share"></i>Paste
						as Group</a></li>
			</ul>
		</div>

	</div>

	<div class="visual-results">
		<div class="pagesnav">
			<ul class="nav nav-tabs">
				<c:if test="${ config.showNews }">
					<li class="active"><a href="#initialContent" data-toggle="tab">
							<i class="icon-globe"></i> News
					</a></li>
				</c:if>

				<li id="filterchart-tab-select" class="${config.showNews ? '' : 'active'}"><a id="filterchart-tab-anchor"
					 href="#filterchart" data-toggle="tab"> <i class="icon-signal"></i>
						Bar Chart
				</a></li>


				<%-- <li id="trending-tab-select" class="${config.showNews ? 'active' : '' }">
					<a href="#trending"  data-toggle="tab"> <i
						class="icon-calendar" id="trending-tab"></i> Trending
				</a></li> --%>

				<li><a href="#topicmap" data-toggle="tab"> <i
						class="icon-tags"></i> Topic Map
				</a></li>

				<li><a href="#tableviewer" data-toggle="tab"> <i
						class="icon-th"></i> Table Viewer
				</a></li>

				<c:if test="${ searchConfig.showNetworkMap }">
					<li><a href="#networkmap" data-toggle="tab"> <i
							class="icon-retweet"></i> Network Map
					</a></li>
				</c:if>

				<li class=""><a href="#searchResults" data-toggle="tab"> <i
						class="icon-search"></i> Results
				</a></li>

				<c:if test="${ config.useAgents }">
					<li><a href="#agentResults" data-toggle="tab"> <i
							class="icon-eye-open"></i> Agents
					</a></li>
				</c:if>

				<li class="pull-right result-view"><label
					for="resultViewSelect"> ${config.uiLabelNames.measurementVariableDropdownLabel}: <select
						name="resultViewSelect" id="resultViewSelect">
							<!-- <option value="simple"></option> -->
							<!-- <option value="fieldcheck"></option> -->
					</select>
				</label></li>

			</ul>
		</div>
		<div id="show" class="main-side shadow scrollable-y scrollableToTop">
			<div id="showPanel" class="paged-content">
				<c:if test="${ config.showNews }">
					<div id="initialContent"
						class="active tabpage hasHelp initial colsPlus">
						<div class="initialContentList"></div>
					</div>
				</c:if>

				<div id="searchResults" class="tabpage hasHelp">
					<!--   div class="resultsIntro">
						<h2>Searching with Find</h2>
						<p>Use the form on the left to find!</p>
					</div -->

					<c:if test="${ config.useAgents }">
						<button id="agentFromSearch" class="btn btn-success">
							<i class="icon-plus icon-white"></i> Create Agent from Search
						</button>
					</c:if>

					<c:if test="${ searchConfig.totalResults }">
					
						<div class="results-control">
							
							<div class="dropdown max-wid-dropdown">
							<span class="totalResults view-results-label">Total Results:</span>
								<a class="btn btn-info btn-mini dropdown-toggle" id="resultActions" data-toggle="dropdown" href="#" 
								style="vertical-align: top; margin: 2px 0 0 10px;">Action</a>
								<ul class="dropdown-menu result-actions pull-right" role="menu"
									aria-labelledby="dLabel">
									<li><a href="#" tabindex="-1" id="resultsExportAction"><i
											class="icon icon-share"></i>Export</a></li>
									<li><a href="#" tabindex="-1" id="resultsTagAction"><i
											class="icon icon-tags"></i>${config.uiLabelNames.resultsActionTagLabel}</a></li>
								</ul>
							</div>
							<div class="dropdown">
                                <label class="results-sort-label" for="results-sort" >Sort:</label>
                                <div class="dropdown-select" >
                                    <select id="results-sort" >                                        
                                    </select>
                                    <div class=asc-arrow>
                                    	<button type="button" class="arrow-box btn btn-info" data-order="asc" style="">
											<span class="icon icon-arrow-up icon-white"></span>
										</button>
                                    </div>
                                </div>
                             </div>
						</div>
					</c:if>

					<div class="resultsList" style="position:absolute; text-align:left; right:0px; top:50px; width:100%"></div>
				</div>

				<div id="filterchart"
					class="tabpage  ${config.showNews ? '' : 'active'}"></div>

				<!-- <div id="trending" class="tabpage"></div> -->

				<div id="topicmap" class="tabpage"></div>

				<div id="tableviewer" class="tabpage"></div>

				<div id="networkmap" class="tabpage"></div>

				<c:if test="${ config.useAgents }">
					<div id="agentResults" class="tabpage hasHelp">
						<div class="agentMainContainer">
							<div id="agentHelp">
								<div>
									<h2>Agents</h2>
									<p>
										<button class="btn btn-large btn-primary" id="createNewAgent">
											<i class="icon-plus icon-white"></i> Create an Agent
										</button>
									</p>
									<p></p>
								</div>

								<div class="agentsGrid"></div>
							</div>

							<div class="agentContainer hidden">
								<div class="agentToolbar btn-group">
									<button class="reload-agent btn btn-warning">
										<i class="icon-refresh icon-white"></i> Reload
									</button>
									<button class="edit-agent btn btn-info">
										<i class="icon-pencil icon-white"></i> Edit
									</button>
									<button class="close-agent btn btn-inverse">
										<i class="icon-remove icon-white"></i> Close Agent
									</button>
								</div>

								<div class="agentDetail form-inline"></div>

								<h2 class="agentResultsHeading">Agent Results</h2>
								<div class="agentDocuments"></div>

							</div>
						</div>
					</div>
				</c:if>
			</div>
		</div>
	</div>

	<div class="modal hide fade" id="showMetaFieldsDialog" tabindex="-1"
		role="dialog">
		<div class="modal-header">
			<button type="button" class="close" data-dismiss="modal">&times;</button>
			<h3 class="meta-title"></h3>
		</div>
		<div class="modal-body"></div>
		<div class="modal-footer">
			<button class="btn btn-success" data-dismiss="modal">
				<i class="icon-ok icon-white"></i> Close
			</button>
		</div>
	</div>
    <c:if test="${ config.useAgents }">
		<!-- Include the agents bar -->
		<page:applyDecorator name="search.agents"
			page="/WEB-INF/decorators/search.agents.jsp" />
	</c:if>
	<script type="text/javascript">
    	var SearchConfig = ${json:toJSON( searchConfig )};       
    </script>
  <script src="<c:url value="../resources/js/jstree/jstree.min.js" />"></script>
	<script src="<%=request.getContextPath()%>/scripts/filter.events-${config.version}.js"/></script>
	<script src="<%=request.getContextPath()%>/scripts/search.events-${config.version}.js"/></script>
	<script src="<%=request.getContextPath()%>/scripts/search.settings.events-${config.version}.js"/></script>
	<script src="<%=request.getContextPath()%>/scripts/jquery.colorbox-min-${config.version}.js"/></script>
	<script src="<%=request.getContextPath()%>/scripts/hammer-${config.version}.js"/></script>
	<script src="<%=request.getContextPath()%>/scripts/jquery.hammer-${config.version}.js"/></script>
	<script src="<%=request.getContextPath()%>/scripts/iframe-viewer-${config.version}.js"/></script>
	<script src="<%=request.getContextPath()%>/scripts/waypoints.min-${config.version}.js"/></script>
	<script src="<%=request.getContextPath()%>/scripts/resget-${config.version}.js"/></script>
	<script src="<%=request.getContextPath()%>/scripts/search.config-${config.version}.js"/></script>
	<script src="<%=request.getContextPath()%>/scripts/search.util-${config.version}.js"/></script>
	<script src="<%=request.getContextPath()%>/scripts/search.settings-${config.version}.js"/></script>
	
	<c:if test="${ config.showNews }">		
		<script src="<%=request.getContextPath()%>/scripts/search.clusters-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/search.recentFrequent-${config.version}.js"/></script>

	</c:if>

	<script src="<%=request.getContextPath()%>/scripts/search.searching-${config.version}.js"/></script>
	<script src="<%=request.getContextPath()%>/scripts/search.top-${config.version}.js"/></script>


	<c:if test="${ parametric.active }">
		<script type="text/javascript">
        var locale = '${pageContext.response.locale.language}';
        var ParametricFields = ${json:toJSON( parametricFields )};
        var FilterFields = ${json:toJSON( filterFields )};
        var FilterTypeOperators = ${json:toJSON( filterTypeOperators )};
    </script>
		
		<script src="<%=request.getContextPath()%>/resources/js/jquery-ui-1.8.23.custom.min.js"/></script>
		<script src="<%=request.getContextPath()%>/resources/js/polyfill.js"/></script>
		<script src="<%=request.getContextPath()%>/resources/js/raphael-min.js"/></script>
		<script src="<%=request.getContextPath()%>/resources/js/underscore.js"/></script>
		<script src="<%=request.getContextPath()%>/resources/js/d3.v3.js"/></script>
		<script src="<%=request.getContextPath()%>/resources/js/Autn/wordwrap.js"/></script>
		<script src="<%=request.getContextPath()%>/resources/js/Autn/reader.js"/></script>
		<script src="<%=request.getContextPath()%>/resources/js/Autn/topicmap.js"/></script>
		<script src="<%=request.getContextPath()%>/resources/js/main.js"/></script>
		<%-- <script src="<%=request.getContextPath()%>/resources/js/totalscriptfiles.js"/></script> --%>
		<script src="<%=request.getContextPath()%>/resources/js/require.js"/></script>

		<script src="<%=request.getContextPath()%>/scripts/fuelux_tree-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/jquery.searchSettingsDialog-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/jquery.parametricfilter-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/filter.contextmenu-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/jquery.filterform-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/jquery.filterItem-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/jquery.baseFilterItem-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/jquery.dateFilterItem-${config.version}.js"/></script>
	
		<script src="<%=request.getContextPath()%>/scripts/jquery.monthpartitionFilterItem-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/jquery.documentfolderFilterItem-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/jquery.matchFilterItem-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/jquery.numericFilterItem-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/jquery.confirmdialog-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/jquery.filtergroup-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/jquery.fieldlist-control-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/jquery.filtersdialog-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/jquery.docFolderCreate-${config.version}.js"/></script>

		<script src="<%=request.getContextPath()%>/scripts/jquery.matchFilterItem-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/jquery.numericFilterItem-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/jquery.confirmdialog-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/jquery.filtergroup-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/jquery.fieldlist-control-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/jquery.filtersdialog-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/jquery.docFolderCreate-${config.version}.js"/></script>

		<script src="<%=request.getContextPath()%>/scripts/jquery.docResultsExport-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/jquery.docResultsTag-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/search.filters-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/search.sunburst-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/search.trending-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/search.topicmap-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/search.tableviewer-${config.version}.js"/></script>
		

		<c:if test="${ searchConfig.showNetworkMap }">
			<script src="<c:url value="../scripts/search.networkmap.js" />"></script>
		</c:if>

	</c:if>

	<c:if test="${ taxonomy.active }">
		
		<script src="<%=request.getContextPath()%>/scripts/jquery.treeview-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/search.taxonomy-${config.version}.js"/></script>
	</c:if>

	<c:if test="${ config.useAgents }">
		
		<script src="<%=request.getContextPath()%>/scripts/jquery.masonry.min-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/search.agents-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/search.agents.edit-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/search.agents.add-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/search.agents.view-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/search.agents.view.items-${config.version}.js"/></script>
		<script src="<%=request.getContextPath()%>/scripts/search.agents.rename-${config.version}.js"/></script>
		
	</c:if>

	<script src="<%=request.getContextPath()%>/scripts/search.document.folders-${config.version}.js"/></script>
	<script src="<%=request.getContextPath()%>/scripts/infonomy/search-${config.version}.js"/></script>
	<script src="<c:url value="../resources/js/chosen.jquery.min.js" />"></script>
  	
	
	 <script>
		   var versionId = $( "#firstname" ).val(); 
		   localStorage.setItem("storageId",versionId);
	 </script>
</body>
</html>