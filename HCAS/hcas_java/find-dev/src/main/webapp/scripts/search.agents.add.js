var AgentEvents = {};
AgentEvents.$ = $(AgentEvents);



jQuery(function($) {
	
	var tempID  =  this.versionObj;
	alert(tempID + " agents.add")
	var tempID =  localStorage.getItem("storageId");
	alert(tempID);

	var ICON = '<i></i> ';

	var $dialog = $('#addAgentDialog');
	var $agentName = $('#agentName');
	var $training = $dialog.find('#agentTraining');
	var $documentsList = $dialog.find('#agentSearchResults');
	var $conceptsList = $dialog.find('.relatedConceptsList');
	var $gettingStarted = $documentsList.find('.gettingStarted');

	var DOCUMENT_TEMPLATE = $
			.resGet('../templates/search/agent.document'+tempID+'.template');
	var CONCEPT_TEMPLATE = $.resGet('../templates/search/agent.concept'+tempID+'.template');

	var URLS = {
		agentAdd: 'ajax/agents/createAgent.json',
		getRelatedDocuments : 'ajax/agents/getRelatedDocuments.json',
		getRelatedConcepts : 'ajax/agents/getRelatedConcepts.json'
	};

	/* -- State Data -- */
	var previousTraining = null;
	var selectedConcepts = {};
	var selectedDocuments = {};

	var getter = function(url) {
		var last;
		return function(data, fn) {
			if (last) {
				last.abort();
				last = null;
			}
			var my = last = $.ajax({
				async : true,
				url : url,
				data : data,
				success : function(documents) {
					if (last === my) {
						fn(documents);
						last = null;
					}
				}
			});
			return my;
		};
	};

	var getRelatedDocuments = getter(URLS.getRelatedDocuments);
	var getRelatedConcepts = getter(URLS.getRelatedConcepts);

	// on add agent
	// show dialog
	// focus agent name
	// on submit training text
	// get documents
	// get related contents
	// on training change
	// relatedConcepts: getRelatedConcepts(training);
	// relatedDocuments: getRelatedDocuments(training);

	/**
	 * Given a map, generates a function that when called will remove any
	 * maplets with falsy value parts.
	 */
	var objectCleaner = function(obj) {
		return function() {
			_.each(obj, function(v, k) {
				if (!v) {
					obj[k] = undefined;
					delete obj[k];
				}
			});
			return obj;
		};
	};

	/**
	 * Removes maplets with falsy value parts from the selectedConcepts map
	 */
	var cleanSelectedConcepts = objectCleaner(selectedConcepts);

	/**
	 * Removes maplets with falsy value parts from the selectedDocuments map
	 */
	var cleanSelectedDocuments = objectCleaner(selectedDocuments);

	/**
	 * Removes maplets with falsy value parts from selectedConcepts and
	 * selectedDocuments
	 */
	var cleanSelectedData = function() {
		cleanSelectedConcepts();
		cleanSelectedDocuments();
	};

	var updatedTraining = function() {
		var nextTraining = $.trim('' + $training.val());
		if (nextTraining === previousTraining) {
			return;
		}
		previousTraining = nextTraining;
		var data = {
			training : $training.val()
		};
		getRelatedConcepts(data,
				function(results) {
					// Remove the unselected elements
					$conceptsList.find('button:not(.toggler-active)').parent()
							.remove();
					// Remove the unselected concepts
					cleanSelectedConcepts();
					// Render the new unique concepts
					$conceptsList.append(_.template(CONCEPT_TEMPLATE, {
						items : _.reject(results, function(e) {
							// Reject those concepts we already have!
							return selectedConcepts[e];
						})
					}));
					// Process the new elements
					$conceptsList.find('button:not(.processed)').each(
							function() {
								processConceptElement($(this));
							});
					updatedConcepts();
				});
		updatedConcepts();
	};

	/**
	 * Compiles an agent training query from training text and selected concepts
	 */
	var buildTrainingQuery = function() {
		cleanSelectedConcepts();
		return _.compact([ $training.val() ].concat(_.keys(selectedConcepts)))
				.join(" AND ");
	};

	/**
	 * When the concepts have been updated Update the documents using
	 * buildTrainingQuery() as the training search text
	 */
	var updatedConcepts = function() {

		var data = {
			training : buildTrainingQuery()
		};
		
		getRelatedDocuments(data, function(results) {
			$gettingStarted.detach();
			// Remove the unselected document elements
			$documentsList.find('button:not(.toggler-active)').parent()
					.remove();
			// Remove the unselected documents
			cleanSelectedDocuments();
			// Process the document results
			_.each(results, function(item) {
				item.title = Util.cleanHtml(item.title);
				item.summary = Util.cleanHtml(item.summary);
			});
			// Render the new unique documents
			$documentsList.append(_.template(DOCUMENT_TEMPLATE, {
				items : _.reject(results, function(e) {
					// Reject those documents we already have!
					return selectedDocuments[e.title];
				})
			}));
			// Process the new documents
			$documentsList.find('.agentDocument:not(.processed)').each(
					function() {
						processDocumentElement($(this));
					});
		});
	};

	/**
	 * Shows the modal add agent dialog
	 */
	var showDialog = function() {
		$dialog.modal('show');
	};
	/**
	 * Hides the modal add agent dialog
	 */
	var hideDialog = function() {
		$dialog.modal('hide');
	};

	/**
	 * Informs the user about some event that occurred
	 */
	var informUser = function(message) {
		return function() {
			//message && alert(message);
		};
	};
	var informAboutSaving = informUser();
	var informAboutSuccess = informUser('Agent saved.');
	var informAboutError = informUser('Error creating agent.');

	// Various states
	var IDLE = 'IDLE';
	var SAVING = 'SAVING';
	var DISCARDING = 'DISCARDING';

	// Current state
	var state = IDLE;

	/**
	 * Retrieves an array of the related concept elements the user has selected.
	 */
	var readSelectedRelatedConceptElements = function() {
		return $('button.toggler-active', $conceptsList);
	};
	/**
	 * Retrieves an array of the related concepts the user has selected.
	 */
	var readSelectedRelatedConcepts = function() {
		var selectedConcepts = [];
		readSelectedRelatedConceptElements().each(function() {
			selectedConcepts.push($(this).data('concept'));
		});
		return selectedConcepts;
	};

	/**
	 * Retrieves an array of selected document references
	 */
	var readSelectedDocuments = function() {
		var results = [];
		$('button.toggler-active', $documentsList).each(function () {
			results.push($(this).parent().data('document'));
		});
		return results;
	};

	/**
	 * Attempts to submit the agent to the server for creation
	 */
	var save = function() {
		if (state !== IDLE) {
			return;
		}
		state = SAVING;

		var data = {
			name : $agentName.val(),
			training : $training.val(),
			concepts : readSelectedRelatedConcepts(),
			documents : readSelectedDocuments()
		};

		hideDialog();
		informAboutSaving();
		sendAgentToServer(data, function () {
			Agents.reloadAndOpen(data);
		});
	};

	var sendAgentToServer = function(data, done) {
		$.ajax({
            contentType : 'application/json',
            data : JSON.stringify(data),
            dataType : 'json',
            type : 'POST',
            url: URLS.agentAdd,
            success: function (result) {
				//add agent to bar
				informAboutSuccess();
				state = IDLE;
				discard();
				done();
			},
			error: function () {
				informAboutError();
				showDialog();
			}
		});
	};

	/**
	 * Resets the dialog ui Clears lists & input
	 */
	var clearDialog = function() {
		$documentsList.html('');
		$conceptsList.html('');
		$training.val('');
		$agentName.val('');
		$documentsList.append($gettingStarted);
	};

	/**
	 * Resets the agent state
	 */
	var clearData = function() {
		previousTraining = null;
		selectedConcepts = {};
		selectedDocuments = {};
	};

	/**
	 * Discards the current agent
	 */
	var discard = function() {
		if (state !== IDLE) {
			return;
		}
		// unless any of the following calls will be asynchronous,
		// the DISCARDING state is unneeded
		state = DISCARDING;
		hideDialog();
		clearDialog();
		clearData();
		state = IDLE;
	};

	/**
	 * Processes a new document element
	 * 
	 * Updates the button to a toggle button with an icon
	 */
	var processDocumentElement = function($e) {
		$e.addClass('processed');
		$e.find('h2 a').addClass('lightbox');
		$e.find('.tickbox').addClass('btn btn-large').prepend(ICON).toggler({
			'this' : {
				active : 'btn-success'
			},
			'> i' : {
				active : 'icon-thumbs-up icon-white',
				inactive : 'icon-thumbs-down'
			}
		}, true).click(
				function() {
					selectedDocuments[$e.data('document')] = $(this).hasClass(
							'toggler-active');
				});
	};

	/**
	 * Processes a new concept element
	 * 
	 * Updates the button to a toggle button with icon Handle click events on
	 * the button for selection/deselection
	 */
	var processConceptElement = function($el) {
		$el.addClass('btn processed').prepend(ICON).toggler({
			'this' : {
				active : 'btn-success'
			},
			'> i' : {
				active : 'icon-thumbs-up icon-white',
				inactive : 'icon-thumbs-down'
			}
		}, true).click(
				function() {
					selectedConcepts[$el.data('concept')] = $el
							.hasClass('toggler-active');
					updatedConcepts();
				});
	};

	// UI event bindings
	$training.change(updatedTraining).keyup(updatedTraining);
	$dialog.find('.saveAgent').click(save);
	$dialog.find('.discardAgent').click(discard);

});