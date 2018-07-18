$(function(){
	
	var tempID  =  this.versionObj;
	var tempID =  localStorage.getItem("storageId");
	var $container = $('#record_container');

    var idx = 0;
    var nHighlights = 0;
    
    var HL_CLASSNAME = 'docquery-highlight';
//    var CONCEPT_METATAG = "SM_ISA";
//    var CONCEPT_ID_ATTR = "CID";
//    var CONCEPT_CHILDID_ATTR = "SID";
    var CONCEPT_METATAG = "IS_A";
    var CONCEPT_ID_ATTR = "CID";
    var CONCEPT_CHILDID_ATTR = "SID";
    var FILTER_HL_TYPE = 'filter';
    var LINK_HL_TYPE = 'hllink';
    var QUERY_HL_TYPE = 'query';
    
    var HL_EMBEDDED_SELECTOR = ".hl-embedded-text";
    var HL_EMBEDDED_SRC_SELECTOR = ".hl-embedded-src";

    $('#tab-content').focus(function() {
        $(this).toggleClass('no-selection', false);
    });
    
    $('#tab-content').blur(function() {
        $(this).toggleClass('no-selection', true);
    });
    
    $('a.external-content-link').click(function() {
        var url = $(this).attr('href');
        var $contentDiv = $(this).closest('div').find('div.external-content');
        
        if ($contentDiv.hasClass('loaded')) {
            $contentDiv.toggleClass('hide');
        } else {
            $contentDiv.empty();
            //show loading...
            $contentDiv.toggleClass('loading', true);
            $.ajax(url, {
                contentType: 'application/json',
            }).done(function(response){
                $contentDiv.toggleClass('error', false);
                
                if (response.success) {
                    $contentDiv.append(response.result);
                    $contentDiv.addClass('loaded');
                } else {
                    $contentDiv.append(response.error);
                    $contentDiv.toggleClass('error', true);
                }
    
            }).error(function(response){
                    $contentDiv.append("Failed to fetch data from server: " + response.statusText);
                    $contentDiv.toggleClass('error', true);
                
            }).always(function() {
                
                $contentDiv.toggleClass('loading', false);
            });
        }
        
        return false;
    });

    var $fieldList = $('#agentstore-fields').on('click', 'h3', function(){
        // allows toggling visibility of specific nodes in the SNOMED supercategory
        $(this).nextUntil('h3').toggle();
    });
    
    $fieldList.on('click', '.highlight-link', function() {
        var linkName = $(this).text();
        var conceptId = $(this).attr('name');
        var hlfield = {name: linkName, value: conceptId, type: LINK_HL_TYPE};
        
        var prevHlField = top.SearchSettings && top.SearchSettings.getDocviewHlField();
        if (!prevHlField || isConceptPlus(prevHlField.field.name) || prevHlField.field.value.indexOf(linkName.toUpperCase()) === -1) {
            $('.hlclicked').removeClass('hlclicked');
            top.SearchSettings.setDocviewHlField({field: hlfield});
            conceptLinksHighlight($(this))
            
        } else {
            scrollTo(idx);
        }
        
        return false;
    });
    

    var $docContainer = $('#cboxContent');
    var $filtersViewer;
    
    var $navTabs = $('.nav-tabs a[data-toggle="tab"]');


    $container.on('click', '.admission h4', function(){
        // clicking on the h4 header of an admission toggles visibility of related information, so you can shrink it
        $(this).siblings().toggle();
    });
    
    $navTabs.click(function() {
        if (top.SearchSettings.setActiveViewTab) {
            top.SearchSettings.setActiveViewTab($(this).attr('href'));
        }
        
    });
    

    var updateOnScroll = true;
    var $scrollerBody = $('.tab-content');

    var $admissionsScroller = $('#admissions-scroller').on('click', '.admission-link', function(){
        var $admission = $(this);
        var admission = $admission.data('admission');
        var $tab = $('.tab-pane.active');
        var $el = $tab.find('.admission[data-admission='+admission+']');

        updateOnScroll = false;

        if (!($scrollerBody[0].scrollHeight > $scrollerBody[0].clientHeight)) {
            // if there's no scroller (too short to scroll), we immediately trigger update of the button
            setActiveLink();
        }
        else if ($el.length) {
            scrollToEl($el, 0, false, setActiveLink);
        }
        else {
            updateOnScroll = true;
        }

        function setActiveLink() {
            $admission.addClass('scrolled-link').siblings('.admission-link').removeClass('scrolled-link');
            updateOnScroll = true;
        }
    });
    
    var findMatchedConceptLinks = _.memoize(function(parentId) {
        return getChildConceptNodes(parentId, []);
    });
    
    

    if ($admissionsScroller.length) {
        $scrollerBody.scroll(showActiveNav);

        $('a[data-toggle=tab]').on('shown', function(){
            var $admission = $('.scrolled-link');
            var admission = $admission.data('admission');

            if (admission) {
                updateOnScroll = false;
                scrollToEl($('.tab-pane.active').find('.admission[data-admission='+admission+']'), 0, true, function(){
                    $admission.addClass('scrolled-link').siblings('.admission-link').removeClass('scrolled-link');
                    updateOnScroll = true;
                });
            }
        });

        showActiveNav();
    }

    function showActiveNav() {
        if (!updateOnScroll) {
            return;
        }

        var $scrollerDom = $scrollerBody[0];
        var pos = $scrollerDom.scrollTop;
        var $targets = $('.active .admission', $scrollerDom);

        if (!$targets.length) {
            return;
        }

        var $selected = $targets.first();

        for (var ii = 0; ii < $targets.length; ++ii) {
            if ($targets[ii].offsetTop > pos) {
                break;
            }
            $selected = $targets.eq(ii);
        }

        var $link = $admissionsScroller.find('.admission-link[data-admission='+$selected.data('admission')+']');
        $link.addClass('scrolled-link').siblings('.admission-link').removeClass('scrolled-link');
    }

    if (top.SearchSettings && !$('#restricted').length) {
        var PROCESSOR = {
            NUMERIC: function(val) {
                var split = _.map(val.split(','), function(a){
                    return Number(a);
                });

                return split.length === 1 ? split[0] : split;
            },
            DATE: function(val, widget){
                var split = _.map(val.split(','), function(a){
                    var date = widget.filterFieldWidget.datepicker1.parseDate(a);
                    // try to parse the date using the datepicker's format; if it fails, just use generic date parsing
                    return date ? date.getTime() : new Date(a).getTime();
                });

                return split.length === 1 ? split[0] : split;
            },
            PARARANGES: function(val) {
                var split = val.split(',');
                split[0] = $.isNumeric(split[0]) ?  Number(split[0]) : -Infinity;
                if (split.length > 1) {
                    split[1] = $.isNumeric(split[1]) ? Number(split[1]) : Infinity;
                }

                return split;
            }
        };

        var OP = {
            GT: function(a, b){return a > b;},
            GE: function(a, b){return a >= b;},
            LT: function(a, b){return a < b;},
            LE: function(a, b){return a <= b;},
            EQ: function(a, b){return a == b;},
            NE: function(a, b){return a != b;},
            RANGE: function(a, b){
                return b.length === 2 && a >= b[0] && a < b[1];
            },
            BEFORE: function(a, b){return a < b;},
            AFTER: function(a, b){return a > b;},
            BETWEEN: function(a, b){
                return b.length === 2 && a >= b[0] && a <= b[1];
            },            
            IS: function(a, b){return a == b;},
            IS_NOT: function(a, b){return a != b;},
            CONTAINS: function(a, b){return a.indexOf(b) !== -1;},
            NOT_CONTAINS: function(a, b){return a.indexOf(b) === -1;},
            IS_RANGE: function(a, b) {
                return b.length === 2 && (a >= b[0] && a < b[1]);
            },
            IS_NOT_RANGE: function(a, b) {
                return b.length === 2 && (a < b[0] || a >= b[1]);
            }
        };

        var filters = top.SearchSettings.getCurrentSearchFilters();
        var query = $.trim(filters.query);
        var hasQuery = query.replace('*', '');
        var filterGroup = filters.filterGroup;

        if (filterGroup || hasQuery) {
            $fieldList.addClass('showing-filters-viewer');

            $filtersViewer = $('<div id="filters-viewer"></div>').insertAfter($fieldList).append(
                '<h5>Query Filters</h5></form>'
            );
        }

        if (hasQuery) {
            $(_.template('<div><fieldset class="filter-query"><span>Query</span><input type="text" id="queryTextInput" value="<%-query%>" disabled></fieldset>', { query: query }))
            .appendTo($filtersViewer).click(function(){
                $('.hlclicked').removeClass('hlclicked');
                
                var $input = $(this).find('input[type="text"]');
                $input.addClass('hlclicked');


                var hlfield = {name: 'querytext', value: $input.val(), type: QUERY_HL_TYPE};
                if (top.SearchSettings) {
                    top.SearchSettings.setDocviewHlField({field: hlfield});
                }
                
                var $toHighlight = $('.autn-highlight');
                
                idx = 0;
                toggleHlNavLink($toHighlight);
                docqueryHighlight($toHighlight, true);

								$(HL_EMBEDDED_SELECTOR).toggleClass('hidden', true);
								$(HL_EMBEDDED_SRC_SELECTOR).toggleClass('hidden', false);
                
                scrollTo(0);
                
                return false;
            });
        }

        if (filterGroup) {
            SearchEvents.getSelectedSearchView = top.SearchEvents.getSelectedSearchView;
            var view = SearchEvents.getSelectedSearchView();

            SearchEvents.getFieldValues = top.SearchEvents.getFieldValues;
            
            //window.ParametricFields = top.window.ParametricFields;
            window.FilterFields = top.window.FilterFields;
            window.SearchConfig = top.window.SearchConfig;
            window.FilterTypeOperators = top.window.FilterTypeOperators;
            
            
            window.FILTER_FIELDS_TYPE = top.window.FILTER_FIELDS_TYPE
            window.FILTER_FIELD_SELECTOR = top.window.FILTER_FIELD_SELECTOR;
            window.FIELD_OPERATOR_SELECTOR = top.window.FIELD_OPERATOR_SELECTOR;
            window.FILTER_FIELD_DATA = top.window.FILTER_FIELD_DATA;
            window.FILTER_OPERATORS = top.window.FILTER_OPERATORS;
            window.PARAMETRIC_FIELDTYPE = top.window.PARAMETRIC_FIELDTYPE;
            window.INDEXED_FIELDTYPE = top.window.INDEXED_FIELDTYPE;
            window.CUSTOM_FIELDTYPE = top.window.CUSTOM_FIELDTYPE;
            
            
            window.SearchSettings = null;

            var SNOMED_FIELD = SearchConfig.searchViews[view].snomedTag;
            var SNOMED_PARENT_FIELD = SearchConfig.searchViews[view].snomedParentTag;

            SearchEvents.setFilters = SearchEvents.attemptSearch = $.noop;
            SearchEvents.getDocumentLists = top.SearchEvents.getDocumentLists;
            SearchEvents.getNonRestrictedLists = SearchEvents.getNonRestrictedLists;
            SearchEvents.getRestrictedLists = top.SearchEvents.getRestrictedLists;

            SearchEvents.getParametricsXhr = _.memoize(function() {
                return $.ajax({
                    url : 'ajax/parametric/getParaFieldValues.json',
                    dataType : 'json',
                    data: {searchView: view }
                });
            });

            $filtersViewer.append('<form id="parametricForm" class="filters edit-disabled">');
            
            $('#parametricForm').on('mouseover,mousemove', 'legend', function() {
                return false;
            });
            
            var fieldlistControl = {};
            fieldlistControl.getFieldValues = top.SearchEvents.getFieldValues;

            var $parametricForm = $('#parametricForm').filterForm({fieldlistControl: fieldlistControl}).on('click', '.filter-item', function(){
                var $filter = $(this);
                var widgetType = $filter.data('widgetName');
                if (!widgetType) {
                    return
                }

                var widget = $filter.data(widgetType);
                var fieldMeta = widget.options.data;

                if (!fieldMeta) {
                    return;
                }
                
                var fieldType;
                
                switch(fieldMeta.fieldType) {
                    case PARAMETRIC_FIELDTYPE:
                        fieldType = fieldMeta.parametric.type;
                        break;
                    case INDEXED_FIELDTYPE:
                        fieldType = fieldMeta.indexed.type;
                        break;
                    case CUSTOM_FIELDTYPE:
                        fieldType = fieldMeta.custom.type;
                        break;
                        
                }    
                

                // we trim out all the leading ADMISSION and PATIENT slashes, if any, to standardize the
                // differences between the field mappings in the ADMISSIONS and PATIENTS database
                var IDOLfield = getFieldNameFromMeta(fieldMeta);
                                
                $('.hlclicked').removeClass('hlclicked');
                $(this).find('input[type="text"]').addClass('hlclicked');

                
                var hlfield = {name: fieldMeta.name, value: fieldMeta.filterValue, type: FILTER_HL_TYPE};
                if (top.SearchSettings) {
                    top.SearchSettings.setDocviewHlField({field: hlfield});
                }
                 
                if (isSnomedField(fieldMeta.name)) {
                    var queriedConceptData = _.find($(CONCEPT_METATAG), function(node) {
                        return $(node).text().trim().toUpperCase() === fieldMeta.filterValue;
                    });
                    
                    var $toClick = null;
                    if (isConcept(fieldMeta.name)) {
                        $toClick = $('a.highlight-link').filter(getConceptHlSelector($(queriedConceptData).attr(CONCEPT_ID_ATTR)));
                        
                    } else {
                        // concept+, find all the concepts
                        var parentId = $(queriedConceptData).attr(CONCEPT_ID_ATTR);
                        var childNodes = findMatchedConceptLinks(parentId);
                        
                        $toClick = $('a.highlight-link').filter(childNodes.join());
                        
                    }
                    conceptLinksHighlight($toClick);

                    
                } else {
                    
                    var preprocessFn = PROCESSOR[fieldType] || function(a) {
                        return a.toUpperCase();
                    };

                    var testValue = preprocessFn($.trim(fieldMeta.filterValue), widget);

                    //noinspection JSUnresolvedVariable
                    var field = FilterFields[view][fieldMeta.name];

                    var compareFn = OP[fieldMeta.fieldOp] || field && field.type === 'range' && function(a, b){
                        // range is implemented low < val <= high
                        var aNum = Number(a);
                        var bRange = b.split(/[,\u2192]/);
                        var upperUnBounded = bRange.length < 2 || _.isEmpty(bRange[1]);
                        
                        return bRange.length === 2 && (upperUnBounded ? aNum > bRange[0] : aNum > bRange[0] && aNum <= bRange[1]);
                    } || function(a, b){
                        return a === b;
                    };

                    if (fieldType === 'DOCUMENTFOLDER') {
                        
                        var $match = $('.documentfolder').filter(function(i,e){
                            return compareFn(preprocessFn(String($(e).data('documentfolder').id), widget), testValue);
                        }).first();

                        toggleHlNavLink(null, true);

                        if ($match.length) {
                            $('#folder-tags').animate({
                                scrollTop: $match[0].offsetTop - 0.5 * $match.height()
                            }, 300, function(){
                                docqueryHighlight($match.find('span'));
                            });
                        }
                    } else {

                        var $potential = $('span[data-field="'+ IDOLfield +'"]');
    
                        var $matched = $potential.filter(function(i, e){
                            return compareFn(preprocessFn($(e).text().trim(), widget), testValue);
                        });
                        
                        var $toHighlight = $matched.length ? $matched.eq(0) : null;
                        toggleHlNavLink($toHighlight);
    
                        if ($matched.length) {
                            idx = 0;
                            nHighlights = 1;
                            docqueryHighlight($toHighlight);
                            scrollTo(0);
                        }
                    }
                    
                    if (top.SearchSettings.setActiveViewTab) {
                        top.SearchSettings.setActiveViewTab('#' + $('.tab-pane.active').id);
                    }
                    
                }
                

                function flashHighlight($toHighlight) {
                    $toHighlight.stop().animate({backgroundColor: 'yellow'}, 50).delay(1000).animate({backgroundColor: 'transparent'}, 300, function(){
                        $toHighlight.css('backgroundColor', '')
                    })
                }
            });

            var filtergroupTemplate = $.resGet('../templates/search/filters.filtergroup'+viewID+'.template');
            $parametricForm.append($(filtergroupTemplate));

            $('fieldset.filter-group').filtergroup({isRoot: true, fieldlistControl: fieldlistControl});

            $parametricForm.find('.rootFilterGroup').data('filtergroup').loadFilters(filterGroup);
            $parametricForm.find('select').prop('disabled', true);
            $parametricForm.find('input').prop('readonly', true);
            
            
            
            
        }
    }

    if (top.SearchEvents && top.SearchEvents.getCheckedFolders) {
        var folders = top.SearchEvents.getCheckedFolders();

        var foldersById = _.reduce(folders, function(memo, folder){
            memo[folder.id] = folder;
            return memo;
        }, {});

        var checked = {};

        _.each(Folders, function(folder) {
            checked[folder.id] = folder;

            if (!foldersById[folder.id]) {
                // this is a folder the document was tagged with, but isn't currently selected.
                // add it to the list of folders to display.
                folder.existing = true;
                folders.push(folder);
            }
        });

        if (folders.length) {
            $fieldList.addClass('showing-folder-tags');
            var $folderTags = $('<div id="folder-tags"></div>').insertAfter($fieldList);

            if ($filtersViewer) {
                $filtersViewer.addClass('showing-folder-tags');
                $folderTags.addClass('showing-filters-viewer')
            }

            folders.sort(function(a, b){
                var al = a.label;
                var bl = b.label;
                return al < bl ? -1 : al > bl ? 1 : 0;
            });

            var documentFolderTemplate = _.template($.resGet('../templates/search/documentfolder.tagger'+viewID+'.template'), undefined, {variable: 'ctx'});
            $folderTags.html(documentFolderTemplate({
                checked: checked,
                folders: folders
            })).on('change', 'input[type=checkbox]', function(){
                var folderMeta = $(this).closest('.documentfolder').data('documentfolder');
                var url = this.checked ? 'ajax/documentfolder/tag.json' : 'ajax/documentfolder/untag.json';

                var reference = Reference;

                $.ajax(url, {
                    type: 'POST',
                    data: {
                        id: folderMeta.id,
                        ref: reference
                    }
                });
            });
        }
    }

    if (top.SearchEvents && top.SearchEvents.getDocuments) {
        var $resultNav = $('#result-nav');
        $resultNav.closest('li').removeClass('hide');

        function onDocsLoaded(){
            var docs = top.SearchEvents.getDocuments();
            var idx = docs.idx;
            var urls = docs.urls;

            $resultNav.html(_.template('<span class="resultcount">Chart <%-idx+1%> out of <%-total ? total : urls.length + " visible"%></span>', {
                urls: urls,
                idx: idx,
                total: docs.total
            }));
            
            var lastActiveTab = top.SearchSettings && top.SearchSettings.getActiveViewTab();
            var isLastTabEq = lastActiveTab && lastActiveTab.substring(1) === $('.tab-pane.active').id;
            if (lastActiveTab && !isLastTabEq) {
               $navTabs.filter('a[href="' + lastActiveTab + '"]').click();
                
            }
                    
            var hlfieldData = (top.SearchSettings) ? top.SearchSettings.getDocviewHlField() : null;
            if (hlfieldData && hlfieldData.field.type === FILTER_HL_TYPE) {
                var $filterItems = $('#parametricForm .filter-item');
                $filterItems.each(function() {
                    var $filter = $(this);
                    var widgetType = $filter.data('widgetName');
                    if (widgetType) {
                        var widget = $filter.data(widgetType);
                        var fieldMeta = widget.options.data;
                        if (hlfieldData.field.name === fieldMeta.name && hlfieldData.field.value === fieldMeta.filterValue) {
                            setTimeout(function() {$filter.click();}, 10);
                            return false;
                        }
                    }
                });
            } else if (hlfieldData && hlfieldData.field.type === LINK_HL_TYPE) {
                var $toClick = $('a.highlight-link').filter(getConceptHlSelector(hlfieldData.field.value));
                if ($toClick.length > 0) {
                    conceptLinksHighlight($toClick);
                }
                
            } else if (hlfieldData && hlfieldData.field.type === QUERY_HL_TYPE) {
                setTimeout(function() {$('#queryTextInput').click();}, 10);
                return false;
            }
        }

        
        top.SearchEvents.$.on(top.SearchEvents.RESULTS_LOADED, onDocsLoaded);
        onDocsLoaded();

        $(window).unload(function(){
            top.SearchEvents && top.SearchEvents.$.off(top.SearchEvents.RESULTS_LOADED, onDocsLoaded);
        })
    }
    
    $('#leftHLBtn,#rightHLBtn').click(function(){
        if (!$(this).hasClass('disabled')) {
            var left = this.id === 'leftHLBtn';
            idx = (idx + nHighlights + (left ? -1 : 1)) % nHighlights;
            $('#currentHlLabel').text(idx + 1);
            scrollTo(idx);
        }
        
        return false;
    });

       
    function isSnomedField(fieldname) {
        return SNOMED_FIELD === fieldname || SNOMED_PARENT_FIELD === fieldname;
    }
    
    function isConcept(fieldname) {
        return SNOMED_FIELD === fieldname;
    }
    
    function isConceptPlus(fieldname) {
        return SNOMED_PARENT_FIELD === fieldname;
    }

    function scrollTo(idx) {
        var $highlights = $('.docquery-highlight');
        $highlights.filter('.active').removeClass('active');
        var $toHighLight = $highlights.eq(idx).addClass('active');
        
        nHighlights = $highlights.length;
        scrollToEl($toHighLight, 0.5);
    }

    function scrollToEl($el, fraction, skipAnimation, callback) {
        $('a[href=#' + $el.closest('.tab-pane').attr('id') + ']').tab('show');

        // if the element is in an admissions block, expand the block so there's something to see
        $el.is(':visible') || $el.closest('.admission').find('h4').siblings().show();

        var scrollTop = $el[0].offsetTop - fraction * $container.height();

        if (skipAnimation) {
            $container[0].scrollTop = scrollTop;
            // callback has to be called asynchronously otherwise it'll be fired before the scroll() event is fired.
            callback && setTimeout(callback, 1);
        }
        else {
            $container.animate({
                scrollTop: scrollTop
            }, 300, undefined, callback);
        }
    }
    
    function docqueryHighlight($toHighlight, isActive) {
        clearHighlight();
        var hlClasses = isActive ? 'docquery-highlight active' : 'docquery-highlight';
        $toHighlight && $toHighlight.addClass(hlClasses);
    }
    
    function clearHighlight() {
    		if ($(HL_EMBEDDED_SRC_SELECTOR).is(':visible')) {
					$(HL_EMBEDDED_SRC_SELECTOR).toggleClass('hidden', true);
					$(HL_EMBEDDED_SELECTOR).toggleClass('hidden', false);
    		}
    		
        $('.docquery-highlight-link').removeClass('docquery-highlight-link');
        $('.docquery-highlight').removeClass('docquery-highlight active');
    }
    
    function toggleHlNavLink($toHighlight, hideLabel) {
        var needHighlight = $toHighlight && $toHighlight.length;
        if (needHighlight || !hideLabel) {
            $('#leftHLBtn,#rightHLBtn').toggleClass('disabled', !needHighlight);
            var currentCount = needHighlight ? 1 : 0;
            var totalCount = needHighlight ? $toHighlight.length : 0;
            $('#currentHlLabel').text(currentCount);
            $('#hlLabel').text('of');
            $('#totalHlLabel').text(totalCount);
            
            $('div.hl-nav-label').toggleClass('zero-hit', !totalCount && !hideLabel);
        } else {
            $('#leftHLBtn,#rightHLBtn').toggleClass('disabled', true);
            $('#currentHlLabel').text('');
            $('#hlLabel').text('\u00A0');
            $('#totalHlLabel').text('');
        }
        
    }
    
    function conceptLinksHighlight($toHighlight) {              
        clearHighlight();
        idx = 0;
        
        var $concepts = null;
        var conceptSelectors = [];
        
        if ($toHighlight && $toHighlight.length) {
            $toHighlight.addClass('docquery-highlight-link');
            // ensure the SNOMED supercategory for the term is visible
            $toHighlight.is(':visible') || $toHighlight.closest('div').prevAll('h3:first').nextUntil('h3').show();
            
            $fieldList.animate({
                scrollTop: $toHighlight[0].offsetTop - 0.5 * $fieldList.height()
            }, 300);
            
            
            // highlight docs
            $toHighlight.each(function() {
                var cid = $(this).attr('name');
                conceptSelectors.push('h[cid="' + $(this).attr('name') + '"]');
            })
            
            $concepts = $(conceptSelectors.join());
            
            $concepts.addClass('docquery-highlight');
            
            if ($toHighlight.length > 1) {
                $concepts.each(function() {
                    var $concept = $(this);
                    
                    var hlTerm = $concept.text();
                    var $children = $concept.find('> .docquery-highlight');
                    while ($children.length > 0) {
                        var $child = $children.first();
                        if ($child.text() === hlTerm) {
                            $child.removeClass('docquery-highlight');
                            $children = $child.find('> .docquery-highlight');
                        } else {
                            break;
                        }
                    }
                    
                });
                
                toggleHlNavLink($concepts.filter('.docquery-highlight'));
                
            } else {
                toggleHlNavLink($concepts);
                
            }
            
            
            scrollTo(idx);
            
        } else {
            $('.docquery-highlight-link').removeClass('docquery-highlight-link');
            toggleHlNavLink($toHighlight);
        }
        
    }
    
    
    function getConceptHlSelector(conceptId) {
        return '[name="' + conceptId + '"]';
    }
    
    function getChildConceptNodes(parentId, childNodesArr) {
        var parentSelector = CONCEPT_METATAG + '[' + CONCEPT_ID_ATTR + '="' + parentId + '"]';
        var childSelector = '[' + CONCEPT_CHILDID_ATTR + ']';
        
        childNodesArr.push(getConceptHlSelector(parentId));
        
        var $childNodes = $(parentSelector).filter(childSelector);
        $childNodes.each(function() {
            getChildConceptNodes($(this).attr(CONCEPT_CHILDID_ATTR), childNodesArr);
        });
        
        return childNodesArr;
        
    }
    
    function getFieldNameFromMeta(fieldMeta) {
        var fieldName = null;
        switch(fieldMeta.fieldType) {
            case PARAMETRIC_FIELDTYPE:
                fieldName = fieldMeta.parametric.name;
                break;
            case INDEXED_FIELDTYPE:
                fieldName = fieldMeta.indexed.name;
                break;
            case CUSTOM_FIELDTYPE:
                fieldName = fieldMeta.custom.name;
                break;
            default:
                fieldName = fieldMeta.name;
        }
        
        return fieldName || fieldMeta.name;
        
        
    }
    
});
