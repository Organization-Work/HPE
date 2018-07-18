jQuery(function ($) {
	var tempID  =  this.versionObj;
	alert(tempID + " treeeview")
	var tempID =  localStorage.getItem("storageId");
	alert(tempID);

    /*  Constants  */

    var API_NAME = 'TreeView';

    var URLS = {
        getCategoryData: 'ajax/taxonomy/getCategoryData.json',
        getCategoryDocCounts: 'ajax/taxonomy/getCategoryDocCounts.json',
        nodeTemplate: '../templates/search/taxonomyTree'+tempID+'.template'
    };

    var TEMPLATES = {
        node: _.template($.resGet(URLS.nodeTemplate))
    };

    var ERROR = {
        docCountsNotLoaded: '<div class="alert">Related document counts not loaded.</div>',
        noTaxonomyLoaded: '<div class="alert">No taxonomy loaded.</div>'
    };

    var NODE_LABEL_CLASS = 'node-label';
    var NODE_LABEL_SELECTOR = '.'+NODE_LABEL_CLASS;

    var RELATED_COUNTER_CLASS = 'related-counter';
    var RELATED_COUNTER_SELECTOR = '.'+RELATED_COUNTER_CLASS;

    var UNIMPORTANT_CLASS = 'unimportant';
    var UNIMPORTANT_CLASS_SELECTOR = '.'+UNIMPORTANT_CLASS;

    var TREE_NODE_SELECTOR = '.tree-node';


    /*  Ajax  */

    var docCountXHR = null;

    var loadTree = _.memoize(function () {
        return $.ajax(URLS.getCategoryData);
    });

    var cancelDocCountXHR = function ($data) {
        if ($data.docCountXHR) { $data.docCountXHR.abort(); }
        $data.docCountXHR = null;
    };

    var loadCategoryDocCounts = function ($data, request) {
        cancelDocCountXHR($data);
        $data.docCountXHR = $.ajax({
            url: URLS.getCategoryDocCounts,
            data: JSON.stringify(request.search),
            type: 'POST',
            contentType: 'application/json',
            dataType: 'json'
        });
        return $data.docCountXHR;
    };


    /*  Display and Rendering  */

    var setTreeContent = function ($tree, content) {
        return $tree.html(content);
    };

    var renderTree = function ($tree, $data, data) {
        var input = $data.processTree ? $data.processTree(data.result) : data.result;
        return setTreeContent($tree, TEMPLATES.node(input));
    };

    var resetCategoryDocCounts = function ($tree) {
        return function () {
            $tree.find(RELATED_COUNTER_SELECTOR)
                .text('');
            $tree.find(TREE_NODE_SELECTOR)
                .removeClass(UNIMPORTANT_CLASS);
        };
    };

    var displayRelatedDocCounts = function ($tree) {
        return function (data) {
            var result = data.result;

            $tree.find(TREE_NODE_SELECTOR).addClass(UNIMPORTANT_CLASS);
            $tree.find(RELATED_COUNTER_SELECTOR).each(function () {
                var $this = $(this);
                var docCount = result[$this.data('id')];
                $this.text((docCount | 0) + '/');
                if (docCount > 0) {
                    $this.parentsUntil($tree)
                        .removeClass(UNIMPORTANT_CLASS);
                }
            });
        };
    };

    var removeCategoryHighlights = function ($tree, nodes) {
        nodes = nodes || $tree.find(NODE_LABEL_SELECTOR);
        nodes.removeClass('active');
    };

    var highlightCategory = function ($tree, $data, nodes) {
        if (!$data.getCategory) { return; }
        nodes = nodes || $tree.find(NODE_LABEL_SELECTOR);
        removeCategoryHighlights($tree, nodes);
        var category = $data.getCategory();
        if (category) {
            nodes.filter(function () { return $(this).data('id') === category; })
                .addClass('active')
                .parent().parents(TREE_NODE_SELECTOR)
                .children('input').prop('checked', true);
        }
    };


    /*  Processing  */

    var ifSuccessfulResponse = function (response, data) {
        return (response.status === 200) && data.success;
    };

    var processResponse = function ($tree, $data) {
        return function (data, status, response) {
            ifSuccessfulResponse(response, data)
                ? renderTree($tree, $data, data)
                : setTreeContent($tree, ERROR.noTaxonomyLoaded);
            if ($data.docCountXHR) {
                $data.docCountXHR.always(processRelatedDocCounts($tree));
            }
            highlightCategory($tree, $data);
        };
    };

    var processRelatedDocCounts = function ($tree) {
        return function (data, status, response) {
            return ifSuccessfulResponse(response, data)
                ? displayRelatedDocCounts($tree)(data)
                : resetCategoryDocCounts($tree)();
        };
    };


    /*  Composed Functionality  */

    var loadAndProcessTree = function ($tree, $data) {
        return loadTree().always(processResponse($tree, $data || {}));
    };

    var loadAndProcessCategoryDocCounts = function ($tree, $data) {
        return function (e, data) {
            return loadCategoryDocCounts($data, data).always(processRelatedDocCounts($tree));
        };
    };

    var api = {
        resetCategoryDocCounts: resetCategoryDocCounts,
        loadAndProcessCategoryDocCounts: loadAndProcessCategoryDocCounts,
        removeCategoryHighlights: removeCategoryHighlights,
        highlightCategory: highlightCategory,
        loadAndProcessTree: loadAndProcessTree
    };

    $[API_NAME] = api;

});