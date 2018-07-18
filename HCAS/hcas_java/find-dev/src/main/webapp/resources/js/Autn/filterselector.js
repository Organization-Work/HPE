define(['jquery', 'Autn/i18n', 'jqueryui'], function($, i18n){
    return function FilterSelector(cfg){
        var onFilterChange = cfg.onFilterChange;
        var hierarchyCountKeys = [];

        $('ul.draggable-filter-inactive').sortable({
            items: 'li.draggable-filter-filter',
            connectWith: 'ul.draggable-filter,#chart',
            containment: 'document'
        });

        var activeFilterList = $('ul.draggable-filter-active').sortable({
            items: 'li.draggable-filter-filter',
            connectWith: 'ul.draggable-filter',
            containment: 'document'
        }).bind('sortupdate', function(evt, ui) {
            updateHierarchyKeys();
            onFilterChange && onFilterChange();
        });

        var textbox = $('#querytextbox').val('').keyup(updateTextBox).change(updateTextBox), lastSearchText;
        updateTextBox();

        function updateTextBox() {
            var newFilter = textbox.val() !== (lastSearchText || '');
            textbox.toggleClass('outdated', newFilter);
            $('#searchbtn').toggleClass('disabled', hierarchyCountKeys.length > 0 && !newFilter);
        }

        this.getSearchText = function() {
            return $('#querytextbox').val();
        };

        this.setLastSearchText = function(text, blur) {
            lastSearchText = text;
            blur && $('#querytextbox').blur();
            updateTextBox();
        };

        function updateHierarchyKeys() {
            hierarchyCountKeys = $('li.draggable-filter-filter', activeFilterList).map(function(idx,a){return $(a).data('filtertype')}).toArray();
        }

        this.getHierarchyCountKeys = function() {
            return hierarchyCountKeys;
        };

        this.initializeFilters = function(filters) {
            $('ul.draggable-filter-inactive').append(filters.map(function(field){
                var tag = $('<li></li>');
                tag.addClass('draggable-filter-filter');
                tag.data('filtertype', field.key);
                tag.text(i18n(field.key));
                return tag;
            }));
        };

        this.activateFirstFilter = function() {
            $('li.draggable-filter-filter').first().appendTo(activeFilterList);
            updateHierarchyKeys();
        };
    };
});