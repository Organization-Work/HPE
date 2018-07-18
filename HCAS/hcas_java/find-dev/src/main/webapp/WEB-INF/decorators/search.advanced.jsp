<div id="advancedFilters" class="tabpage active">
    <!--div class="filter-list"></div-->
    <form id="parametricForm" class='filters'>
    </form>
    
</div>

<div id="documentfolders" class="tabpage">
	
</div>

<div class="modal hide loading-dialog" id="loadingDialog">
    <div class="modal-header">
        <!--button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button-->
        <h4 class="title">Loading...</h4>
    </div>
    <div class="modal-body">
        <div class="preloader" id="loadingPreloader">
            <i></i><i></i><i></i><i></i>
            <!--[if lt IE 10]>
            <script type="text/javascript">
                Util.preloader("loadingPreloader");
            </script>
            <![endif]-->
        </div>    
    </div>
</div>

<div class="modal hide confirm-dialog" id="confirmDialog">
    <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
        <h4 class="title">Confirmation</h4>
    </div>
    <div class="modal-body">
        <div><div class="big-icon-exclamation-sign-container"><i class="big-icon-exclamation-sign message-icon"></i></div><div style="width: 85%;float: right;" class="message">Are you sure that you want to do this?</div></div>
    </div>
    <div class="modal-footer">
        <a href="#" class="btn yes-control">Yes</a>
        <a href="#" class="btn no-control">No</a>
    </div>
   
</div>

<div class="modal hide" id="searchSettingsDialog" tabindex="-1" role="dialog">
    <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal">&times;</button>
        <h3>Search Settings</h3>
    </div>
    <div class="modal-body">
        <form class="form-horizontal">
            <div class="control-group">
                <label class="control-label" for="combineLabel">Combine</label>
                <div class="controls">
                    <input type="text" id="combineLabel" name="combine" data-provide="typeahead" data-source='["fieldcheck","simple"]'>
                </div>
            </div>
            <div class="control-group">
                <label class="control-label" for="summaryLabel">Summary</label>
                <div class="controls">
                    <input type="text" id="summaryLabel" name="summary" data-provide="typeahead" data-items="3" data-source='["concept","context","quick","paragraphconcept","paragraphcontext","off"]'>
                </div>
            </div>
            <input type="hidden" id="searchSettingsId">
            <button class="hide" type="submit" value="Submit"></button>
        </form>
    </div>
    <div class="modal-footer">
        <span id="settingsUpdateError" class="error-message pull-left">Error updating the settings</span>
        <button class="btn" data-dismiss="modal"><i class="icon-remove"></i> Cancel</button>
        <button class="btn btn-primary confirm"><i class="icon-ok"></i> Apply</button>
    </div>
</div>

<div class="filter-dialog">
    
    <div class="modal hide filter-dialog-ui">
        <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
            <h4 class="title">Query Filters</h4>
        </div>
        <div class="modal-body">
            <div id="filterSetContextMenu" class="dropdown clearfix filterset-context-menu">
                <ul class="dropdown-menu context-menu" role="menu">
                    <li><a tabindex="-1" class="folder-menu-item" href="#"><i class="icon-remove"></i>Delete</a></li>
                    <li><a tabindex="-1" class="folder-menu-item" href="#"><i class="icon-edit"></i>Rename</a></li>
                </ul>
            </div>
            <form class="filterDialogForm">
                <div class="folderselect">
                    <span class="dialogLabel">Save in:</span>
                    <span class="input-append dropdown">
                        <input id="select-folder" class="span3" type="text" readonly>
                        <button class="btn btn-small dropdown-toggle" data-toggle="dropdown"><span class="caret"></span></button>
                        <button class="btn btn-small create-folder-control" data-toggle="tooltip" title="Create new folder"><i class="icon-folder-close"></i></button>
                        <ul class="dropdown-menu">
                            <li>
                                <div id="folderTree" class="tree foldertree">
                        
                                    <div class = "tree-folder hidden">
                        		        <div class="tree-folder-header">
                        			        <i class="icon-folder-close"></i>
                        			        <div tabindex="-1" class="tree-folder-name"></div>
                        		        </div>
                        		        <div class="tree-folder-content"></div>
                        		        <div class="tree-loader hidden"></div>
                        	        </div>
                        	        <div class="tree-item hidden">
                        		        <i class="tree-dot"></i>
                        		        <div class="tree-item-name"></div>
                        	        </div>
                                </div>
                            </li>
                        </ul>
                    </span>
                </div>
                <div id="alertFilterDialog"></div>
                <div class="tree foldercontent">
                        <div class = "tree-folder hidden">
            		        <div class="tree-folder-header">
            			        <i class="icon-folder-close"></i>
            			        <div tabindex="-1" class="tree-folder-name"></div>
            		        </div>
            		        <div class="tree-folder-content"></div>
            		        <div class="tree-loader hidden"></div>
            	        </div>
            	    <div class="content-loader hidden">Loading...</div>
                    <div class="content-list"></div>
                </div>
                <div class="tree-loader-container"></div>
                <div class="input-prepend control-group">
                    <label class="inline">Name:</label>
                    <input id="savedFilterName" class="span35" type="text">
                </div>
                <div class="input-prepend control-group">
                    <label class="inline">Description:</label>
                    <textarea id="savedFilterDescription" class="span35" rows="2"></textarea>
                </div>
            </form>     
        </div>
        <div class="modal-footer">
            <a href="#" data-dismiss="modal"  class="btn cancel-control">Cancel</a>
            <a href="#" class="btn btn-primary save-control hidden">Save</a>
            <a href="#" class="btn btn-primary load-control hidden">Load</a>
            <span id="saveFilterLoader" class="import-loader-gif"></span>
        </div>
    </div> 
</div>
<div id="importPatientsDialog"></div>
    <iframe id="exportResultFrame" class="hidden"></iframe>
    
    <div class="modal hide" id="documentExportDialog" tabindex="-1" role="dialog">
        <div class="modal-header">
            <button type="button" class="close cancel" data-dismiss="modal">&times;</button>
            <h3 id="docExportTitle">Document Folder Export</h3>
        </div>
        <div class="modal-body">
            <form class="form-horizontal fieldsexport" id="fieldsExportForm" method="post" target="exportResultFrame">
                <div class="control-group">
                    <label class="control-label" for="exportName">Name</label>
                    <div class="controls">
                        <input type="text" name="exportName" id="exportName" tabIndex="-1"></input>
                    </div>
                </div>
                <div class="control-group">
                    <label class="control-label" for="exportFormat">Format</label>
                    <div class="controls exportFormatSelect">
                        <select id="exportFormat" name="exportFormat" title="Select export format"  tabIndex="-1">
                            <option value="CSV">CSV</option>
                       <!--       <option value="XML">XML</option>  -->
                        </select>
                    </div>
                </div>
                <div class="control-group  results-export-control maxdocs hide">
                    <label class="control-label" for="exportMaxDocs">Maximum Rows</label>
                    <div class="controls">
                        <div class="input-append">
                            <input type="text" name="exportMaxDocs" id="exportMaxDocs" tabIndex="-1"></input>
                            <span id="exportTotalResults"></span>
                            <div class="export-doc-slider" id="exportDocSlider"></div>
                        </div>
                    </div>
                </div>
                <div class="control-group">
                    <label class="control-label" for="exportFieldsSelect">Export Fields</label>
                    <div class="controls exportfieldsselect">
                        <select id="exportFieldsSelect" class="multiselect" multiple="multiple"  tabIndex="-1">
                        </select>
                    </div>
                </div>
                <div class="exporttablescontainer">
                    <table class="table exporttableheader">
                        <thead>
                            <tr>
                                <th>Document Field</th>
                                <th>Exported Field</th>
                                <th class="span1"><button id="clearExportFields" class="btn btn-mini" >Clear</button></th>
                            </tr>
                        </thead>
                    </table>
                    <div class="fieldsexporttable">
                        <table class="table table-bordered" id="fieldsexporttable">
                            <tbody>
                            </tbody>
                        </table>
                    </div>
                </div>
                <input name="exportSourceFields" id="hiddenExportTargetFields" type="hidden"></input>
                <input name="exportTargetFields" id="hiddenExportSourceFields" type="hidden"></input>
                <input name="exportDocFolderId" id="exportDocFolderId" type="hidden"></input>
                <input name="searchView" id="exportSearchView" type="hidden"></input>
                <input name="exportSearchData" id="exportSearchData" type="hidden"></input>
                
            </form>
        </div>
        <div class="modal-footer">
            <button class="btn cancel" data-dismiss="modal"><i class="icon-remove"></i> Cancel</button>
            <button class="btn btn-primary confirm"><i class="icon-share"></i> Export</button>
        </div>
    </div>




