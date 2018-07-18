<%@ taglib uri="sitemesh-page" prefix="page"%>
<%@ taglib prefix="json" uri="/WEB-INF/tld/json.tld" %>

<script type="text/javascript">
document.body.className += ' agents-on ';
    var AgentConfig = ${json:toJSON( agentConfig )};
</script>

<div class="navbar agentbar navbar-fixed-bottom">
	<div class="navbar-inner" style="border-radius: 0; padding-left: 10px;">
		<span class="brand">Agents:</span>
		<ul class="nav" id="agentslist"></ul>
		<ul class="nav pull-right">
			<li class="divider-vertical"></li>
			<li><button id="addAgent" class="btn btn-success">
					<i class="icon-white icon-plus"></i> Add New Agent
				</button></li>
		</ul>
	</div>
</div>

<div id="renameDialog"
	class="modal hide fade"
	role="dialog" tabindex="-1">
	<div class="modal-header">
		<h3>Rename agent</h3>
	</div>
	<div class="modal-body">
		<div>
			<label for="for0">New Name:</label>
			<input id="for0" class="newName">
		</div>
	</div>
	<div class="modal-footer">
		<div class="errorSaving pull-left error control-group hide"
			style="position: absolute; margin-top: 3px;">
			<span class="help-inline">Error saving changes, try again later.</span>
		</div>
		<div class="indicator pull-left hide progress progress-striped active"
			style="width: 295px; margin-top: 6px; margin-bottom: 0;">
			<div class="bar" style="width: 100%;"></div>
		</div>
		<button class="cancel btn">
			<i class="icon-trash"></i> Discard</button>
		<button class="save btn btn-primary">
			<i class="icon-ok icon-white"></i> Rename Agent</button>
	</div>
</div>

<page:applyDecorator page="/WEB-INF/decorators/agent.edit.jsp" name="agent.edit" />

<div class="modal addAgentDialog hide fade" id="addAgentDialog"
	tabindex="-1" role="dialog">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">&times;</button>
		<h3>Add New Agent</h3>
	</div>
	<div class="modal-body">

		<div class="leftPanel">

			<div class="nonform">
				<div class="control-group">
					<label for="agentName" class="heading">Agent Name</label> <input id="agentName"
						type="text" value="" class="span4">
				</div>
				<div class="control-group">
					<label for="agentTraining" class="heading">Training Text</label>
					<textarea id="agentTraining" class="span4"></textarea>
				</div>
				<!--
				<div class="control-group">
					<label for="agentStartDate" class="heading">Start Date</label>
					<textarea id="agentStartDate" class="span4">[currently not used]</textarea>
				</div>-->

				<div class="control-group">
					<label class="heading">Related Concepts</label>
					<div>
						<ul class="relatedConceptsList"></ul>
					</div>
				</div>
			</div>
		</div>

		<div class="rightPanel" style="padding: 20px;">
			<!-- <h4 class="heading">Related Documents:</h4> -->
			<div id="agentSearchResults">
				
				<div class="gettingStarted">
					<h2>Getting Started</h2>
					<p>To create an agent:</p>
					<ol>
						<li>Name your agent
						<li>Enter your agent's training text
						<!--<li>Select a start date (or leave it blank for no date)-->
						<li>Select related concepts
						<li>Select relevant documents
						<li>Click 'Save Agent'
					</ol>
				</div>
				
			</div>
		</div>

	</div>
	<div class="modal-footer">
		<button class="discardAgent btn" data-dismiss="modal">Cancel</button>
		<button class="saveAgent btn btn-primary">Save Agent</button>
	</div>
</div>