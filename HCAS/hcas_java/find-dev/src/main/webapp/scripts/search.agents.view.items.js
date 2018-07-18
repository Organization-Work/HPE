var AgentsItems = {};
jQuery(function($) {
	var tempID  =  this.versionObj;
	alert(tempID + " aagents.view.items")
	var tempID =  localStorage.getItem("storageId");
	alert(tempID);

	var ERROR_LOADING_DOCUMENTS = '<div class="alert">An error occurred loading this agent\'s documents.</div>';
	var ITEM_TEMPLATE = $.resGet('../templates/search/agent.view.agentitem'+tempID+'.template');
	var DELETE_TEMPLATE = $.resGet('../templates/search/agent.view.delete'+tempID+'.template');
	
	var $agentHelp = $('#agentHelp');
	var $grid = $('.agentsGrid');
	var $show = $('#show');

    //  When the grid/view becomes visible
    $('#agentResults:visible, .agentsGrid:visible').livequery(function () {
        //  Arrange items using masonry after the containers have layout
        _.delay(function () {
            $grid.masonry();
        });
    });

    $grid.masonry({
        'itemSelector': '.agentGridItem',
        'columnWidth': 385
    });

	var editing = null;
	
	Agents.$.on(Agents.events.ADDED, function(e, agent) {
		var agentHtml = renderAgents([agent]);
		addAgentToDom(agentHtml);
	});

	Agents.$.on(Agents.events.DELETED, function(e, agent) {
		$grid.find('[data-aid="' + agent.aid + '"]').remove();
		if (editing === agent.aid) {
			closeEdit();
		}
	});

	Agents.$.on(Agents.events.CLEAR, function(e) {
		$grid.find('[data-aid]').remove();
		if (editing) {
			closeEdit();
		}
	});

	$('#createNewAgent').on('click', function () {
		Agents.showAddAgent();
	});
	
	var renderAgents = function(agents) {
		return $(_.template(ITEM_TEMPLATE, {
			agents : agents
		})).each(function (index, val) {
			var item = $(this);
			$('.agentButton-delete', item).popover({
				title: 'Delete Agent',
				content: _.template(DELETE_TEMPLATE, {agent: agents[index]}),
				placement: 'left',
				trigger: 'manual'
			}).attr('title', 'Delete').on('click', function (event) {
				event.stopPropagation();
				var $this = $(this);
				if ($this.is('.disabled')) {
					return;
				}
				if ($('.popover').length === 0) {
					var $content = $('.content');
					var contentClick = function recur() {
						$this.popover('hide');
						$content.unbind('click', recur);
					};
					setTimeout(function () {
						$content.bind('click', contentClick);
						attachPopoverEvents(item, function () {
							$content.unbind('click', contentClick);
						});
					});
					$this.popover('show');
				}
			});
			$('.agentButton-edit', item).on('click', function () {
				AgentEdit.edit(Agents.getAgent(item.data('aid')), {
					success: function () {
						Agents.reloadAgents();
					}
				});
			});
			$('.agentButton-rename', item).on('click', function () {
				RenameAgent.show(Agents.getAgent(item.data('aid')), {
					success: function () {
						Agents.reloadAgents();
					}
				});
			});
			$('.agentButton-open', item).on('click', function () {
				AgentsView.loadAgent(item.data('aid'));
			});
		});
	};

	var attachPopoverEvents = function (item, cancelCallback) {
		$('.popover:not(.processed)').addClass('.processed').each(function () {
			var pop = $(this);
			var btn = $('.agentButton-delete', item);
			pop.find('.reject-btn').on('click', function () {
				cancelCallback();
				btn.popover('hide');
			});
			pop.find('.accept-btn').on('click', function () {
				cancelCallback();
				btn.popover('toggle');
				pop.remove();
				item.find('button').addClass('disabled');
				Agents.deleteAgent(item.data('aid'));
			});
		});
	};
	
	var openEdit = function() {

	};

	var closeEdit = function() {

	};
	
	var addAgentToDom = function (html) {
        var $html = $(html);
        $grid.append($html).masonry('destroy').masonry();
	};

	// AgentsItems;
});