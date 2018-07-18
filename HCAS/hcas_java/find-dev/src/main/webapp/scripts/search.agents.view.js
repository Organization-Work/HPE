var AgentsView = {};
jQuery(function($) {
	var tempID  =  this.versionObj;
	alert(tempID + " treeeview")
	var tempID =  localStorage.getItem("storageId");
	alert(tempID);

	var URLS = {
		agentGetResults : 'ajax/agents/getResults.json',
		markAsRead : 'ajax/agents/markAsRead.json'
	};
	var CHUNK_SIZE = 10;
	var WAYPOINT_OPTIONS = {
		offset : '10px',
		context : '#show'
	};
	var ERROR_LOADING_DOCUMENTS = '<div class="alert">An error occurred loading this agent\'s documents.</div>';
	var DETAIL_TEMPLATE = $
			.resGet('../templates/search/agent.view.detail'+tempID+'.template');
	var DOCUMENTS_TEMPLATE = $
			.resGet('../templates/search/agent.view.documents'+tempID+'.template');

	var $scrollToTop = $('#scrollToTop');
	var $agentHelp = $('#agentHelp');
	var $agentContainer = $('.agentContainer');
	var $agentTab = $('[href=#agentResults]');
	var $agentDetail = $agentContainer.find('.agentDetail');
	var $agentDocuments = $agentContainer.find('.agentDocuments');
	var $show = $('#show');
	var $reloadAgent = $agentContainer.find('.reload-agent');
	var $editAgent = $agentContainer.find('.edit-agent');
	var $closeAgent = $agentContainer.find('.close-agent');
	var $extraScrollSpace = $('<div style="padding-top:100%;"></div>');

	var agentData = null;
	var documentsData = null;
	var renderCount = 0;
	var noMoreResults = false;
	var canMarkAsRead = false;
	
	var displayAgentPage = function() {
		$agentTab.tab('show');
	};

	var cancelWaypoints = function() {
		$agentDocuments.find('.agentDocument').waypoint('destroy');
	};

	var displayIntro = function() {
		$agentHelp.show();
		cancelWaypoints();
		$agentDocuments.html("");
		$agentContainer.hide();
		agentData = null;
		documentsData = null;
		renderCount = 0;
		canMarkAsRead = false;
	};

	var displayContent = function() {
		$agentHelp.hide();
		$agentContainer.show();
	};

	var storeDocuments = function(documents, offset) {
		if (!offset || offset === 0) {
			documentsData = documents;
			renderCount = 0;
		} else {
			documentsData = documentsData.concat(documents);
		}
	};

	var removeOldDocuments = function() {
		$agentDocuments.find('.agentDocument').fadeOut("fast");
	};

	/**
	 * Loads document from the server loadDocuments :: (String aid, Maybe
	 * Integer offset) -> Ajax ()
	 */
	var lastXHR = null;
	var loadDocuments = function(aid, unreadOnly, offset) {
		// Already loading next page?
		if (lastXHR) {
			return false;
		}
		if (!offset || offset === 0) {
			noMoreResults = false;
			removeOldDocuments();
			canMarkAsRead = true;
		}
		if (noMoreResults) { return false; }
		$extraScrollSpace.detach();
		// Store the XHR as lastXHR
		lastXHR = $.ajax({
			url : URLS.agentGetResults,
			data : {
				aid : aid,
				offset : offset,
                unreadOnly: unreadOnly
			},
			success : function(results) {
				if (results.length === 0) {
					noMoreResults = true;
					$agentDocuments.append($extraScrollSpace);
				} else {
					noMoreResults = false;
					storeDocuments(results, offset);
					if (!offset || offset === 0) {
						beginRendering();
						Agents.unreadRefresher.forceRefresh();
					} else {
						shouldRenderNext();
					}
				}
				ReloadIcon.stop();
			},
			error : function() {
				ReloadIcon.stop();
				if (!offset || offset === 0) {
					$agentDocuments.html();
				}
				$agentDocuments.append(ERROR_LOADING_DOCUMENTS);
			},
			complete : function() {
				// Reset the lastXHR
				lastXHR = null;
			}
		});
	};

	var markDocumentAsRead = function(aid, reference) {
		$.ajax({
			url : URLS.markAsRead,
			data : {
				aid : aid,
				reference : reference
			},
			success : function(result) {
				
			}
		});
	};

	var loadAgent = function(aid) {
		var agent = Agents.getAgent(aid);
		if (!agent) {
			return reset();
		}
		agentData = agent;

		displayAgentPage();

		$agentDetail.html(templateDetail(agent));
		loadDocuments(aid, agent.unreadOnly);

		displayContent();
	};

	var reset = function() {
		Agents.deselectAgent();
		displayIntro();
	};

	var beginRendering = function() {
		$extraScrollSpace.detach();
		cancelWaypoints();
		$agentDocuments.html('');
		renderTillFull();
	};

	var renderTillFull = function() {
		if (!anyLeftToRender()) {
			return;
		}
		while ($show.height() == $show[0].scrollHeight && anyLeftToRender()) {
			renderNextChunk();
		}
		renderNextChunk();
	};

	var templateDocuments = function(documents) {
		return _.template(DOCUMENTS_TEMPLATE, {
			documents : documents,
            hideRead: agentData && !agentData.unreadOnly
        });
	};

	var templateDetail = function(agent) {
		return _.template(DETAIL_TEMPLATE, agent);
	};

	var renderNextChunk = function() {
		if (!documentsData || documentsData.length == 0) {
			return;
		}
		var docsToRender = documentsData.slice(renderCount, renderCount
				+ CHUNK_SIZE);
		renderCount += docsToRender.length;
		$agentDocuments.append(templateDocuments(docsToRender));
		processNewDocuments();
	};

	var loadNextPage = function() {
		if (!documentsData || documentsData.length == 0
				|| renderCount < documentsData.length) {
			return;
		}
		loadDocuments(agentData.aid, agentData.unreadOnly, documentsData.length);
	};

	/**
	 * Actually at the bottom
	 */
	var atBottom = function() {
		return ($show[0].scrollHeight - $show.height()) == $show.scrollTop();
	};

	/**
	 * Within 1000px of the bottom
	 */
	var nearBottom = function() {
		return ($show[0].scrollHeight - $show.height()) <= $show.scrollTop() + 1000;
	};

	var shouldRenderNext = function() {
		if (atBottom()) {
			renderNextChunk();
		}
	};

	var shouldLoadNext = function() {
		if (nearBottom()) {
			loadNextPage();
		}
	};

	$show.on('scroll', function() {
		shouldLoadNext();
		shouldRenderNext();
	});
	$(window).on('resize', shouldRenderNext);

	var processNewDocuments = function() {
		var elems = $agentDocuments.find('.agentDocument:not(.processed)');
		elems.addClass('processed').each(function() {
			var $this = $(this);
			var btn = $this.find('.btn-readstate');
			var readDocWithBtn = function () {
				readDocument(btn);
			};
			btn.prepend('<i class="icon-white"></i> ');
			btn.toggler({
				'this' : {
					active : 'disabled btn-success',
					inactive : 'btn-primary'
				},
				'> i' : {
					active : 'icon-ok',
					inactive : 'icon-envelope'
				}
			});
			$this.find('a.lightbox').on('click', readDocWithBtn).on('opened', readDocWithBtn);
			$this.waypoint(function(event, direction) {
				if ($this.is(':visible') && canMarkAsRead) {
					if (direction === 'down') {
						readDocWithBtn();
					}
				}
			}, WAYPOINT_OPTIONS);
		});
	};

	$agentDocuments.delegate('.btn-readstate', 'click', function() {
		readDocument($(this));
	});

	$reloadAgent.on('click', function() {
		if (agentData) {
			canMarkAsRead = false;
			$scrollToTop.trigger('click');
			_.delay(function () { canMarkAsRead = true; }, 1000);
			loadDocuments(agentData.aid, agentData.unreadOnly, 0);
			ReloadIcon.spin();
		}
	});

	var editAgent = function () {
		AgentEdit.edit(agentData, {
			success: function (data) {
				Agents.reloadAndOpen(data);
			}
		});
	};

	$editAgent.on('click', editAgent);

	var ReloadIcon = (function() {
		var btn = $reloadAgent, cls = 'spin-icon', active = false, loopId = null, interval = 1000;
		var performSpin = function() {
			btn.removeClass(cls);
			setTimeout(function() {
				btn.addClass(cls);
			});
		};
		var spin = function() {
			if (!active) {
				active = true;
				btn.addClass('disabled');
				loop();
			}
		};
		var stop = function() {
			if (active) {
				active = false;
				btn.removeClass('disabled');
				btn.removeClass(cls);
				if (loopId) {
					clearTimeout(loopId);
					loopId = null;
				}
			}
		};
		var loop = function() {
			if (active) {
				performSpin();
				loopId = setTimeout(loop, interval);
			}
		};
		return {
			spin : spin,
			stop : stop
		};
	})();

	$closeAgent.on('click', function() {
		reset();
	});

	var readDocument = function($elem) {
		if (!agentData || $elem.length === 0) {
			return;
		}
		$elem.toggler('on');
		$elem.find('.btn-text').text('Read');
		markDocumentAsRead(agentData.aid, $elem.data('ref'));
	};

	var anyLeftToRender = function() {
		return (renderCount < documentsData.length);
	};

	AgentsView.loadAgent = loadAgent;
	AgentsView.reset = reset;
});