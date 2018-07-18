jQuery(function ($) {

    //  UI and State
    var $parent = $('#taxonomy .tree-container');
    var $state = {
        getCategory: SearchEvents.getCategory,
        processTree: function (input) {
            input.options = {
                expanded: false,
                counts: true
            };
            return input;
        }
    };

    //  Event Hooks
    SearchEvents.$.on(
        [SearchEvents.RESET, SearchEvents.SEARCH].join(' '),
        $.TreeView.resetCategoryDocCounts($parent));

    SearchEvents.$.on(
        SearchEvents.SEARCH_REQUEST_SENT,
        $.TreeView.loadAndProcessCategoryDocCounts($parent, $state));

    $parent.on('click', '.node-label', function () {
        var $this = $(this);
        var activate = !$this.hasClass('active');
        $.TreeView.removeCategoryHighlights($parent);
        if (activate) {
            $this.addClass('active');
            SearchEvents.setCategory($this.data('id'));
        } else {
            SearchEvents.removeCategory();
        }
        SearchEvents.attemptSearch();
    });

    var hash = Hash.observe(function (value, sender) {
        $.TreeView.highlightCategory($parent, $state);
    });

    //  Initial Query
    $.TreeView.loadAndProcessTree($parent, $state);

});