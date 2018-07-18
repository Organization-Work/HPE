jQuery(function($) {

	/* -- Configuration -- */
	var AGENT_ICON_TEMPLATE = '<li class="btn-group"><button class="btn btn-inverse" data-aid="<%= aid %>"><%= name %></button></li>';
	var AGENT_NOTIFICATION_TEMPLATE = '<button class="notify btn btn-danger" data-aid="ref4"><span class="tooltip-arrow"></span><%= value %></button>';
	var AGENT_LOAD_URL = "ajax/agents/getAgents.json";
	var AGENT_CREATE_URL = "ajax/agents/createAgent.json";
	var AGENT_DELETE_URL = "ajax/agents/deleteAgent.json";
	var AGENT_UNREAD_URL = "ajax/agents/getUnreadCount.json";
	var AGENT_LOADING_MESSAGE = "Loading agents...";
	var AGENT_ERROR_LOADING_MESSAGE = "Unable to load agents.";

    var REFRESH_INTERVAL = 120000;

	// <ul> of agent <li><button>...</button></li>
	var $agentslist = $("#agentslist");
	var $addAgent = $("#addAgent");
	var $addAgentDialog = $("#addAgentDialog");

	// Internal agent data store
	/*
	 * Type: { aid: { aid: String, name: String, documents: [String], sources:
	 * [String], startDate: String } }
	 */
	var agents = {};

	/**
	 * Renders a single agent icon
	 */
	var renderAgentIcon = function(agent) {
		return _.template(AGENT_ICON_TEMPLATE, agent);
	};

	/**
	 * Renders a collection of agents as icons
	 */
	var renderAgentIcons = function(items) {
		return _.map(items, renderAgentIcon).join("");
	};

	/**
	 * Removes all agent icons
	 */
	var clearAgentBar = function() {
		agents = {};
		$agentslist.html("");
		window.Agents.$.trigger(window.Agents.events.CLEAR, []);
	};

	/**
	 * Selects an agent icon given the corresponding agent data object
	 */
	var getAgentIcon = function(agent) {
		return $agentslist.find("[data-aid^=" + agent.aid + "]");
	};

	/**
	 * Renders and sets the agent icons on the agent bar
	 */
	var setAgentsOnBar = function(items) {
		clearAgentBar();
		_.each(items, function(item, idx, arr) {
			addAgentToBar(item, idx !== arr.length - 1);
		});
	};

	/**
	 * Renders and adds a single agent icon to the agent bar
	 */
	var addAgentToBar = function(agent, preventRefresh) {
		if (isValidAgent(agent)) {
			agents[agent.aid] = agent;
			var icons = $(renderAgentIcon(agent));
			$agentslist.append(icons);
			icons.find('button').css('max-width', 0).animate({'max-width': 500, duration: 500});
			window.Agents.$.trigger(window.Agents.events.ADDED, [agent]);
		}

        if (!preventRefresh) {
            UnreadRefresher.forceRefresh();
        }
	};

	/**
	 * Removes an agent from the data store
	 */
	var deleteAgentFromBar = function(agent) {
		window.Agents.$.trigger(window.Agents.events.DELETED, [agent]);
		getAgentIcon(agent).parent().remove();
		delete agents[agent.aid];
	};

	/**
	 * Determines if an agent data object is considered valid
	 */
	var isValidAgent = function(agent) {
		if (!agent || $.trim(agent.name) === '') {
			return false;
		}
		// TODO:...other agent validity constraints...
		return true;
	};

	/**
	 * Requests the creation of a new agent
	 */
	var createAgent = function(agent) {
		if (isValidAgent(agent)) {
			$.ajax({
				url : AGENT_CREATE_URL,
				data : agent,
				dataType : 'json',
				success : function(agent) {
					addAgentToBar(agent, false);
				},
				error : function(result) {
					Util.log("Error creating agent! Result: ", result);
				}
			});
		}
	};

	var deleteAgent = function(aid) {
		$.ajax({
			url : AGENT_DELETE_URL,
			data : {
				aid : aid
			},
			dataType : 'json',
			success : function(agent) {
				deleteAgentFromBar(agents[aid]);
			},
			error : function(result) {
				Util.log("Error deleting agent! Result: ", result);
			}
		});
	};

	var showLoadingMessage = function() {
		clearAgentBar();
		$agentslist.html("<li><p>" + AGENT_LOADING_MESSAGE + "</p>");
	};

	var showErrorLoadingMessage = function() {
		clearAgentBar();
		$agentslist.html("<li><p>" + AGENT_ERROR_LOADING_MESSAGE + "</p>");
	};

	var reloadAgents = function(done) {
		showLoadingMessage();

		$.ajax({
			url : AGENT_LOAD_URL,
			dataType : 'json',
			success : function(results) {
				setAgentsOnBar(results);
				UnreadRefresher.start();
				done && done(results);
			},
			error : function(result) {
				clearAgentBar();
				Util.log("Failed to load the agents! Result: ", result);
			}
		});
	};

	/**
	 * Loads the agents for the current user
	 */
	var loadAgents = function() {

		reloadAgents();
	};

	var clearBadges = function() {
		$agentslist.find(".notify").remove();
	};

	var createBadge = function(badge) {
		return _.template(AGENT_NOTIFICATION_TEMPLATE, {
			value : (badge > 100) ? "100+" : badge
		});
	};

	var setBadges = function(badges) {
		clearBadges();
		_.each(badges, function(badge, aid) {
			if (aid in agents) {
				if ((badge | 0) !== 0) {
					getAgentIcon(agents[aid]).after(createBadge(badge));
				}
			}
		});
	};

	/**
	 * 
	 */
	var UnreadRefresher = (function() {
		var active = false, refreshInterval = REFRESH_INTERVAL, timeoutId;

		var start = function() {
			if (active) {
				return;
			}
			active = true;
			refresh();
		};

		var stop = function() {
			if (!active) {
				return;
			}
			active = false;
			if (timeoutId) {
				clearTimeout(timeoutId);
				timeoutId = null;
			}
		};

		var refresh = function() {
			if (!active) {
				return;
			}

			$.ajax({
				url : AGENT_UNREAD_URL,
				success : function(results) {
					var badgeData = {};
					_.each(results, function(v) {
						badgeData[v.aid] = v.count;
					});
					setBadges(badgeData);
				},
				error : function(result) {
					clearBadges();
					Util.log("Error getting unread numbers: ", result);
				},
				complete : function() {
					// Loop
					if (!active) {
						return;
					}
					timeoutId = setTimeout(refresh, refreshInterval);
				}
			});
		};

		return {
			start : start,
			stop : stop,
			setRefreshInterval : function(value) {
				refreshInterval = value;
			},
			forceRefresh: _.debounce(function () {
				stop();
				start();
			}, 500)
		};
	})();

	$addAgentDialog.modal({
		backdrop : true,
		keyboard : false,
		show : false
	});

	$addAgent.on("click", function() {
		AgentEdit.create({}, {
			success: function (data) {
				Agents.reloadAndOpen(data);
			}
		});
	});

	$agentslist.delegate('button[data-aid]', 'mousedown', function() {
		var $this = $(this);
		var aid = $this.data('aid');
		$this.data('deletion', setTimeout(function() {
			if (confirm('Delete agent: `' + $this.text() + '`?')) {
				deleteAgent(aid);
			}
		}, 500));
	}).delegate('button[data-aid]', 'mouseup', function() {
		clearTimeout($(this).data('deletion'));
	});

	$agentslist.delegate('.btn:not(.notify)', 'click', function() {
		$agentslist.find('.btn:not(.notify)').removeClass('active');
		$(this).addClass('active');
	});

	var $agentsTab = $('[href=#agentResults]');
	$agentslist.delegate('.btn[data-aid]', 'click', function() {
		$agentsTab.tab('show');
		var aid = $(this).data('aid');
		AgentsView.loadAgent(aid);
	});

	/**
	 * On startup load agents
	 */
	_.defer(loadAgents);

	// Release an agent api
	window.Agents = {
		addAgent : addAgentToBar,
		deleteAgent : deleteAgent,
		isValidAgent : isValidAgent,
		reloadAgents : reloadAgents,
		reloadAndOpen : function (data) {
			reloadAgents(function (results) {
				for (var i = 0, m = results.length; i < m; i += 1) {
					var agent = results[i];
					if (agent.name === data.name) {
						AgentsView.loadAgent(agent.aid);
						break;
					}
				}
			});
		},
		showAddAgent: function () {
			$addAgent.trigger('click');
		},
		getAgent : function(aid) {
			return agents[aid];
		},
		deselectAgent: function () {
			$agentslist.find('.btn.active').removeClass('active');
		},
		unreadRefresher: UnreadRefresher
	};
	window.Agents.$ = $(window.Agents);
	window.Agents.events = {
		ADDED: 'ADDED',
		DELETED: 'DELETED',
		CLEAR: 'CLEAR'
	};
});
