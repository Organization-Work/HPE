var AgentEdit = {};
jQuery(function ($) {
	
	var tempID  =  this.versionObj;
	alert(tempID + " treeeview")
	var tempID =  localStorage.getItem("storageId");
	alert(tempID);
	
    'use strict';

    //  This is our dialog
    var $parent = $('#editAgentDialog');
    //  Helper function for finding dialog child elements
    var $ui = function () {
        return $parent.find.apply($parent, arguments);
    };

    //  Locate the ui elements
    var $title = $ui('.title');
    var $agentName = $ui('.agentName');
    var $conceptSearch = $ui('.conceptSearch');
    var $addConceptBtn = $ui('.addConceptBtn');
    var $conceptList = $ui('.conceptList');
    var $categoryQuery = $ui('.categoryQuery');
    var $categoryReset = $ui('.categoryReset');
    var $categoryTree = $ui('.agent-tree');
    var $documentSearch = $ui('.documentSearch');
    var $documentList = $ui('.documentList');
    var $databaseList = $ui('.databaseList');
    var $startDates = $ui('.startDate');
    var $noStartDate = $ui('.noStartDate');
    var $useStartDate = $ui('.useStartDate');
    var $saveBtn = $ui('.saveBtn');
    var $cancelBtn = $ui('.cancelBtn');
    var $indicator = $ui('.progress');
    var $error = $ui('.alert');
    var $unreadOnly = $ui('input[name=unreadOnly]');
    var $minScore = $ui('input[name=minScore]');
    var $dispChars = $ui('input[name=dispChars]');
    var $filters = $parent.find('.filters');

    function buttons(el, regex) {
        var min = el.prop('min');
        var max = el.prop('max') || Number.POSITIVE_INFINITY;
        var step = Number(el.prop('step'));

        el.change(function(){
            clean(0);
        });

        el.siblings('.btn').click(function(){
            clean($('.icon', this).hasClass('icon-plus') ? step : -step);
        });

        function clean(offset) {
            el.val(Math.min(max, Math.max(min, offset + (Number(el.val().replace(regex, '')) || 0))));
        }
    }

    buttons($minScore, /[^.0-9]/gi);
    buttons($dispChars, /\D/gi);

    // Only use parametrics if it's been enabled (imported)
    var useParametrics = $.fn.parametricfilter;
    if (!useParametrics) {
        $filters.addClass('hide');
    }
    if (!$.TreeView) {
        $ui('.category-step').addClass('hide');
    }

    // renumber the steps
    $parent.find('.step').not('.hide').children('h3').text(function(idx, oldText){
        return oldText.replace('##', idx + 1);
    });

    var unreadOnlyDefault = AgentConfig.defaultUnreadOnly;

    //  Templating
    var ICON = '<i></i> ';
    var CONCEPT_TEMPLATE = $.resGet('../templates/search/agent.edit.concept'+tempID+'.template');
    var DOCUMENT_TEMPLATE = $.resGet('../templates/search/agent.document'+tempID+'.template');
    var URL_ROOT = 'ajax/agents/';
    var URLS = {
        getRelatedConcepts: URL_ROOT + 'getRelatedConcepts.json',
        getRelatedDocuments: URL_ROOT + 'getRelatedDocuments.json',
        getDocumentsFromReferences: URL_ROOT + 'getDocumentsFromReferences.json',
        createAgent: URL_ROOT + 'createAgent.json',
        editAgent: URL_ROOT + 'editAgent.json',
        getDatabaseOptions: URL_ROOT + 'getDatabaseOptions.json'
    };

    //  Dialog State
    var canSave = false;
    var savingURL = null;
    var savingXHR = null;

    //  Agent Model
    var concepts = {};
    var documents = {};
    var loadedModel = {};
    var categoryListPromise;
    var selectedCategory = null;

    //  Callbacks
    var callbacks = {};
    var setCallbacks = function (data) {
        callbacks.success = data.success || $.noop;
    };

    /* Load up the database options */
    (function () {
        var makeDatabaseButton = function (text) {
            var result = $('<button>');
            result.text(text[0].toUpperCase() + text.slice(1))
                .attr('data-db', text)
                .addClass('btn')
                .prepend(' ')
                .prepend(
                    $('<i>').addClass('icon'));
            result.toggler({
                'this': {
                    active: 'btn-success'
                },
                '& i': {
                    active: 'icon-ok icon-white',
                    inactive: 'icon-minus'
                }
            }, true).toggler('on');
            return result;
        };
        var insertDatabaseOptions = function (result) {
            var all = [];
            $databaseList.empty();
            _.each(result, function (db) {
                all.push(makeDatabaseButton(db));
                all.push(' ');
            });
            $databaseList.append(all);
        };
        $.ajax({ url: URLS.getDatabaseOptions }).done(insertDatabaseOptions);
    })();

    //  Set up the dialogs modal properties
    $parent.modal({
        show: false,
        backdrop: 'static'
    });


    /* Resets the agent creation form - modal and ui */
    var reset = function () {
        resetXHR();
        resetUI();
        resetModel();
    };

    /* Resets the agent/dialog model */
    var resetModel = function () {
        canSave = false;
        concepts = {};
        documents = {};
        selectedCategory = null;
    };

    /* Resets the agent creation ui */
    var resetUI = function () {
        $agentName.val('');

        $categoryQuery.val('');
        categoryListPromise && categoryListPromise.done(function () {
            $categoryTree.find('.visible,.active').removeClass('visible active');
        });

        $conceptSearch.val('');
        $conceptList.html('');

        $documentSearch.val('');
        $documentList.html('');

        $databaseList.find('.btn').toggler('off');
        $startDates.val('');
        $noStartDate.prop('checked', true);

        $unreadOnly.filter('[value='+unreadOnlyDefault+']').prop('checked', true);

        $parent.scrollTop(0);

        hideIndicator();
        hideError();
        disableControls();
    };

    var resetXHR = function () {
        searchConcepts.reset();
        searchDocuments.reset();
    };

    /* Loads the model of an agent into the dialog */
    var loadModel = function (model) {
        activeFilters = [];
        loadedModel = model;
        $agentName.val(model.name || '');
        _.each(model.concepts || [], addConcept);
        addDocuments(model.documents);
        setDatabases(model.databases);
        setStartDate(model);
        $unreadOnly.filter('[value='+model.unreadOnly+']').prop('checked', true);
        useParametrics && $filters.parametricfilter('loadFilters', model.filters);
        if (model.categoryId && categoryListPromise) {
            categoryListPromise.done(function () {
                $categoryTree.find('.visible,.active').removeClass('visible active');
                var node = $categoryTree.find('[data-id="' + model.categoryId + '"]');
                node.addClass('active');
                node.parents('.tree-node').addClass('visible');
            });
        }
        $dispChars.val(model.dispChars != null ? model.dispChars : SearchSettings.getDisplayChars());
        $minScore.val(model.minScore != null ? model.minScore : SearchSettings.getMinScore());

        if (!model.documents && !model.aid && model.concepts && model.concepts.length) {
            $documentSearch.val(_.compact(model.concepts).join(' AND '));
            performDocumentSearch();
        }
    };

    var setDatabases = function (databases) {
        if (databases) {
            _.each(databases || [], function (db) {
                $databaseList.find('[data-db^="'+db+'"]').toggler('on');
            });
        }
    };

    var setStartDate = function (model) {
        var date, year, month, day;
        if (!model) { return; }
        if (model.startDate) {
            date = new Date(model.startDate);
            year = date.getFullYear();
            month = date.getMonth() + 1;
            day = date.getDate();
            $startDates.filter('.year').val(year);
            $startDates.filter('.month').val(month);
            $startDates.filter('.day').val(day);
            $useStartDate.prop('checked', true);
        } else {
            $noStartDate.prop('checked', true);
        }
    };

    var asConceptSet = function (seq) {
        var set = {};
        _.each(seq, function (v) {
            set[v] = true;
        });
        return set;
    };

    /* Disables ui elements */
    var disableControls = function () {
        $ui('input, button').prop('disabled', true);
    };

    /* Enables ui elements */
    var enableControls = function () {
        $ui('input, button').prop('disabled', false);
    };

    /* Displays the progress indicator */
    var showIndicator = function () {
        var bar = $indicator.find('.bar');
        bar.removeClass('btn-success btn-warning').css('width', '0%');
        _.delay(function () {
            bar.css('width', '50%');
        });
        $indicator.fadeIn('slow');
    };
    /* Removes the progress indicator from view */
    var hideIndicator = function () {
        $indicator.hide();
    };

    /* Shows the error message */
    var showError = function () {
        $error.fadeIn('slow');
    };
    /* Hides the error message */
    var hideError = function () {
        $error.hide();
    };

    /* Initiates the saving process,
     * Prepares for and responds to agent saving */
    var save = function () {
        if (!canSave) {
            return;
        }

        var model = getModel();
        if (!isValidModel(model)) {
            return;
        }

        enterSavingState();

        submitAgent(prepareModel(model, loadedModel), {
            success: function () {
                $indicator.find('.bar').addClass('btn-success').css('width', '100%');
                _.delay(function () {
                    hide();
                    callbacks.success(model);
                }, 600);
            },
            error: function () {
                $indicator.find('.bar').addClass('btn-danger').css('width', '0%');
                _.delay(function () {
                    enterSavingFailedState();
                }, 600);
            }
        });
    };

    var prepareModel = function (model, loadedModel) {
        var result = $.extend({}, model);
        if (loadedModel.aid && 'name' in loadedModel && model.name !== loadedModel.name) {
            delete result['name'];
            result.name = loadedModel.name;
            result.newName = model.name;
        }

        if (!result.removeStartDate) {
            result.startDate = new Date(model.startDate.year|0, model.startDate.month - 1, model.startDate.day).getTime();
        }

        return result;
    };

    /* Configures the UI into a state indicating saving */
    var enterSavingState = function () {
        canSave = false;

        disableControls();
        hideError();
        showIndicator();
    };

    /* Configures the UI into a state indicating a failed save */
    var enterSavingFailedState = function () {
        hideIndicator();
        showError();
        enableControls();

        canSave = true;
    };

    /* Collects all the agent data together
     * into a single model object */
    var getModel = function () {
        var filters = updateFilters();
        var model = $.extend({}, loadedModel, {
            concepts: _.keys(concepts),
            dispChars: $dispChars.val(),
            categoryId: selectedCategory,
            documents: _.keys(documents),
            filters: filters,
            minScore: $minScore.val(),
            name: $agentName.val(),
            unreadOnly: $unreadOnly.filter('[value=true]').is(':checked')
        });
        if ($noStartDate.prop('checked')) {
            model.removeStartDate = true;
        }
        else {
            model.startDate = getStartDate();
        }

        model.databases = [];
        $databaseList.find('.toggler-active').each(function () {
            model.databases.push($(this).data('db'));
        });

        return model;
    };


    var getStartDate = function () {
        var year, month, day;
        year = $startDates.filter('.year').val(); // keep as string for length check
        month = $startDates.filter('.month').val() | 0;
        day = $startDates.filter('.day').val() | 0;
        return ({year: year, month: month, day: day});
    };

    /* Checks if a model is valid enough to send to the server */
    var isValidModel = function (model) {
        var result = true;
        //  Non-empty model name
        result = result && ($.trim(model.name) !== '');
        //  At least one concept
        result = result && (_.keys(concepts).length > 0);
        //  Either no start date, or a valid start date
        if (!model.removeStartDate) {
            result = (result
                && model.startDate
                && model.startDate.year.length === 4
                && model.startDate.month >= 1
                && model.startDate.month <= 12);
        }
        return result;
    };

    /* Submits the agent to the server for */
    var submitAgent = function (model, response) {
        savingXHR = $.ajax($.extend({
            contentType : 'application/json',
            data : JSON.stringify(model || loadModel()),
            dataType : 'json',
            type : 'POST',
            url: savingURL
        }, response));
        return savingXHR;
    };

    /* Adds a new concept */
    var addConcept = function (concept) {
        if (!(concept in concepts)) {
            concepts[concept] = true;
            $conceptList.prepend(renderConcepts([concept]));
            processNewConcepts(true);
        }
    };

    /* Adds documents to the document list */
    var addDocuments = function (docs) {
        var docsToAdd = [];
        _.each(docs, function (doc) {
            if (!(doc in documents)) {
                documents[doc] = true;
                docsToAdd.push(doc);
            }
        });
        if (docsToAdd.length > 0) {
            $.ajax({
                url: URLS.getDocumentsFromReferences,
                data: {
                    references: docsToAdd
                },
                success: function (results) {
                    $documentList.prepend(renderDocuments(
                        _.uniq(results, false, function (item) {
                            return item.reference;
                        })));
                    processNewDocuments(true);
                }
            });
        }
    };

    /* Renders multiple concepts */
    var renderConcepts = function (conceptsToRender) {
        return _.template(CONCEPT_TEMPLATE, {
            items: conceptsToRender
        });
    };

    /* Renders multiple documents */
    var renderDocuments = function (documents) {
        return _.template(DOCUMENT_TEMPLATE, {
            items: documents
        });
    };


    /*  Load/configure the taxonomy tree category selection box.  */
    if ($.TreeView) (function () {

        //  TreeView State
        var inverseNameMap = null;
        var $state = {
            processTree: function (input) {
                input.options = {
                    expanded: true,
                    counts: false
                };
                return input;
            }
        };

        //  Event Hooks
        $categoryTree.on('click', '.node-label', function () {
            var $this = $(this);
            $categoryTree.find('.active').removeClass('active');
            $this.addClass('active');
            selectedCategory = $this.data('id');
        });

        $categoryQuery.on('change', function () {
            var value = $categoryQuery.val();
            display(inverseNameMap[value]);
        });

        $categoryReset.on('click', function () {
            categoryListPromise.done(function () {
                selectedCategory = null;
                $categoryTree.find('.visible,.active').removeClass('visible active');
            });
        });

        var invertMap = function (map) {
            var result = {};
            _.each(map, function (v, k) {
                result[v] = (result[v] || []).concat(k);
            });
            return result;
        };

        var display = function (matches) {
            categoryListPromise.done(function () {
                var first = true;
                selectedCategory = null;
                $categoryTree.find('.visible').removeClass('visible');
                _.each(matches, function (id) {
                    var child = $categoryTree.find('[data-id="'+id+'"]');
                    if (child.length) {
                        if (first) {
                            child.addClass('active');
                            selectedCategory = id;
                            first = false;
                        }
                        child.parents('.tree-node').addClass('visible');
                    }
                });
            });
        };

        //  Initial Query
        categoryListPromise = $.TreeView.loadAndProcessTree($categoryTree, $state).done(function (data) {
            if (data.success) {
                inverseNameMap = invertMap(data.result.names);
                $categoryQuery.typeahead({
                    source: _.keys(inverseNameMap)
                });
            }
        });

    })();



    /* Builds a query search given a url and a callback object
     *
     * If a query is the same as the the previous one supplied
     * Then no new search is performed
     *
     * If a new search is to be performed and a previous search
     * is currently underway, the previous one is aborted.
     */
    var buildQuerySearch = function (url, callbacks) {
        var fn = function (query) {
            if (fn.previous === query) {
                return;
            }
            fn.previous = query;

            if (fn.xhr) {
                fn.xhr.abort();
                fn.xhr = null;
            }

            fn.xhr = $.ajax({
                url: url,
                data: {
                    training: query
                },
                success: callbacks.success || $.noop,
                error: callbacks.error || $.noop
            });

            return fn.xhr;
        };
        fn.previous = null;
        fn.xhr = null;
        fn.reset = function () {
            if (fn.xhr) {
                fn.xhr.abort();
                fn.xhr = null;
            }
            fn.previous = null;
        };
        return fn;
    };

    /* Gets related concepts from the server */
    var searchConcepts = buildQuerySearch(URLS.getRelatedConcepts, {
        success: function (results) {
            updateConcepts(results);
        },
        error: function (err) {

        }
    });

    /* Gets related documents from the server */
    var searchDocuments = buildQuerySearch(URLS.getRelatedDocuments, {
        success: function (results) {
            updateDocuments(results);
        },
        error: function (err) {

        }
    });

    /* Updates the concepts box with new items */
    var updateConcepts = function (newConcepts) {
        // remove unselected concepts
        $conceptList.find('button').not('.toggler-active').parent().remove();
        // for each new concept
        //   if not already in list
        //     add to list
        //TODO
        $conceptList.append(renderConcepts(_.reject(newConcepts, function (v) {
            return(v in concepts);
        })));
        processNewConcepts(false);
    };

    /* Updates the documents list */
    var updateDocuments = function (newDocs) {
        // remove unselected documents
        $documentList.find('button').not('.toggler-active').parent().remove();
        // for each new document
        //   if not already in list
        //     add to list
        //TODO
        $documentList.append(renderDocuments(_.reject(newDocs, function (v) {
            return(v.reference in documents);
        })));
        processNewDocuments(false);
    };

    /* Prepares new concept elements */
    var processNewConcepts = function (initialState) {
        var btns = $conceptList.find('button').not('.processed').addClass('processed');
        btns.toggler({
            'this': {
                active: 'btn-success'
            }
        }, true).click(function () {
                var $this = $(this);
                var concept = $this.data('concept');
                if ($this.toggler('status')) {
                    concepts[concept] = true;
                } else {
                    delete concepts[concept];
                }
            });
        if (initialState) {
            btns.trigger('click');
        }
    };

    /* Prepares new document elements */
    var processNewDocuments = function (initialState) {
        var docs = $documentList.find('.agentDocument').not('.processed').addClass('processed');
        docs.find('h2 a').addClass('lightbox');
        var btns = docs.find('.tickbox').addClass('btn btn-large').prepend(ICON);
        btns.toggler({
            'this': {
                active: 'btn-success'
            },
            '> i': {
                active: 'icon-thumbs-up icon-white',
                inactive: 'icon-thumbs-down'
            }
        }, true).click(function () {
                var $this = $(this);
                var doc = $this.parent().data('document');
                if ($this.toggler('status')) {
                    documents[doc] = true;
                } else {
                    delete documents[doc];
                }
            });
        if (initialState) {
            btns.trigger('click');
        }
    };

    /* Shows the agent dialog */
    var show = function (title, url, initialData, callbacks) {
        reset();
        enableControls();

        $title.text(title);
        savingURL = url;
        setCallbacks(callbacks);
        loadModel(initialData);

        canSave = true;
        $parent.modal('show');
        return AgentEdit;
    };

    /* Hides the agent dialog */
    var hide = function () {
        reset();
        canSave = false;
        $parent.modal('hide');
        return AgentEdit;
    };

    /* Executes a concept search for the entered term */
    var performConceptSearch = function () {
        searchConcepts($conceptSearch.val());
    };

    /* Executes a document search for the entered term */
    var performDocumentSearch = function () {
        searchDocuments($documentSearch.val());
    };

    /* Concept Search Box Events */
    $conceptSearch.change(performConceptSearch).keyup(performConceptSearch);

    $addConceptBtn.click(function () {
        var value = $conceptSearch.val();
        if ($.trim(value) !== '') {
            addConcept(value);
        }
    });

    /* Document Search Box Events */
    $documentSearch.change(performDocumentSearch).keyup(performDocumentSearch);


    $saveBtn.click(save);
    $cancelBtn.click(hide);

    /* Release methods under namespace */
    AgentEdit.show = show;
    AgentEdit.hide = hide;
    AgentEdit.edit = function (model, callbacks) {
        return AgentEdit.show('Edit Agent', URLS.editAgent, model, callbacks);
    };
    AgentEdit.create = function (model, callbacks) {
        return AgentEdit.show('Create Agent', URLS.createAgent, model, callbacks);
    };

    var activeFilters = [];

    if (useParametrics) {
        $filters.parametricfilter({
            fieldIdPrefix: 'FILTER_AGENT_'
        }).on('parametricfilterupdate', function(evt, filters){
            activeFilters = filters;
        }).parametricfilter('loadFieldsAndFiltersFromDeferred', SearchEvents.getParametricsXhr());
    }

    function updateFilters() {
        return _.foldl(activeFilters || [], function (v, f) { return f(v); }, {});
    }
});