<div id="editAgentDialog" class="modal hide fade in" tabindex="-1" role="dialog">
    <div class="modal-header">
        <h3 class="title">Add New Agent</h3>
    </div>
    <div class="modal-body">

        <div class="step well">
            <h3>Step ## &mdash; Find training concepts</h3>
            <div class="input-append">
                <input class="conceptSearch" type="text">
                <button class="btn addConceptBtn">Add as Concept</button>
            </div>
            <div class="conceptListBox listBox">
                <ul class="conceptList"></ul>
            </div>
        </div>

        <div class="step well category-step">
            <h3>Step ## &mdash; Restrict to a category</h3>
            <div class="input-append">
                <input class="categoryQuery" type="text" placeholder="Search here."/>
                <button class="categoryReset btn btn-inverse">Reset</button>
            </div>
            <div class="agent-tree tree-container">
            </div>
        </div>

        <div class="step well">
            <h3>Step ## &mdash; Find documents</h3>
            <div>
                <label>Search:</label>
                <input class="documentSearch" type="text">
            </div>
            <div class="listBox documentListBox">
                <ul class="documentList"></ul>
            </div>
        </div>

        <div class="step well filters">
            <h3>Step ## &mdash; Advanced filtering</h3>
            <div class="btn-group">
                <a class="btn btn-small dropdown-toggle" data-toggle="dropdown" href="#">
                    <i class="icon icon-plus"></i> Add Filter&hellip;
                </a>
                <ul class="dropdown-menu field-list">
                    <li><a class="ignore">Loading&hellip;</a></li>
                </ul>
            </div>
            <div class="filter-list"></div>
        </div>

        <div class="step well">
            <h3>Step ## &mdash; Databases</h3>
            <p>Select all the databases this agent should use.  If none are selected, all available databases will be used.</p>
            <div class="databaseList"></div>
        </div>

        <div class="step well">
            <h3>Step ## &mdash; Viewed Documents</h3>
            <div>
                <label> <input name="unreadOnly" type="radio" value="true" checked> Only show unread documents </label>
            </div>
            <div>
                <label> <input name="unreadOnly" type="radio" value="false"> Show all documents </label>
            </div>
        </div>

        <div class="step well">
            <h3>Step ## &mdash; Settings</h3>
            <div>
                <label> Characters </label>
                <p class="input-prepend input-append">
                    <button class="btn"><i class="icon icon-minus"></i></button>
                    <input class="span1" name="dispChars" value="50" min="50" step="50" type="text">
                    <button class="btn"><i class="icon icon-plus"></i></button>
                </p>
            </div>
            <div>
                <label> Min Score </label>
                <p class="input-prepend input-append">
                    <button class="btn"><i class="icon icon-minus"></i></button>
                    <input class="span1" name="minScore" value="0" min="0" max="100" step="5" type="text">
                    <button class="btn"><i class="icon icon-plus"></i></button>
                </p>
            </div>
        </div>

        <div class="step well">
            <h3>Step ## &mdash; Start Date</h3>
            <div>
                <div>
                    <label> <input class="noStartDate" name="usedate" type="radio" value="false" checked> Don't use a start date </label>
                </div>
                <div>
                    <label>
                        <input class="useStartDate tog-box" name="usedate" type="radio" value="true">
                        Use a start date
                        <p class="tog-left">
                            Year:  <input class="startDate year span1" type="number" value="" min="0">
                            Month: <input class="startDate month span1" type="number" value="" min="1">
                            Day:   <input class="startDate day span1" type="number" value="" min="1"> </p> </label>
                </div>
            </div>
        </div>

        <div class="step well">
            <h3>Step ## &mdash; Name your Agent</h3>
            <div><input class="agentName" type="text"></div>
        </div>

    </div>
    <div class="modal-footer">
        <div class="notifications">
            <span class="hide alert alert-error">Problem occurred when saving.</span>
            <div class="progress progress-striped active">
                <div class="bar"></div>
            </div>
        </div>
        <button class="cancelBtn btn"><i class="icon-trash"></i> Cancel</button>
        <button class="saveBtn btn btn-primary"><i class="icon-ok icon-white"></i> Save Agent</button>
    </div>
</div>