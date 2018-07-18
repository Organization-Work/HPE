var versionObj = $( "#buildVersion" ).val(); // getting hidden value from searchjsp for versioning the script

var require = {
	 baseUrl: '../resources/js',
    // can be useful for preventing caching. stick a p4 number on it?
//    urlArgs: 'r=' +  (new Date()).getTime(),
    paths: {
        bootstrap: '../../scripts/bootstrap-2.3.2.min'+versionObj+'',
        bootstrapmultiselect: '../../scripts/bootstrap-multiselect'+versionObj+'',
        bootstrapdatetimepicker: '../../scripts/bootstrap-datetimepicker.min'+versionObj+'',
        d3: 'd3.v2',
        d3patch: 'd3.patch',
        jquery: 'jquery-1.10.2.min',
        jquerymigrate: 'jquery-migrate-1.2.1.min',
        jqueryui: 'jquery-ui-1.8.23.custom.min',
        jquerytouchpunch: 'jquery.ui.touch-punch.min',
        jqueryhashchange: 'jquery.ba-hashchange.min',
        json2: 'json2-min',
        messageformat: 'messageformat.min',
        raphael: 'raphael-min',
        underscore: '../../scripts/underscore'+versionObj+'',
        'Autn/i18n': 'Autn/i18n.js?locale=' + encodeURIComponent(locale)
    },
    shim: {
        backbone: {
            //These script dependencies should be loaded before loading backbone.js
            deps: ['underscore', 'jquery', 'jquerymigrate', 'json2'],
            //Once loaded, use the global 'Backbone' as the module value.
            exports: 'Backbone'
        },
        bootstrap: ['jquerymigrate'],
        bootstrapmultiselect: ['bootstrap'],
        bootstrapdatetimepicker: ['bootstrap'],
        jquerymigrate: ['jquery'],
        jqueryui: ['jquerymigrate'],
        jqueryhashchange: ['jquerymigrate'],
        jquerytouchpunch: ['jqueryui'],
        d3: ['d3patch', 'polyfill'],
        messageformat: {
            exports: 'MessageFormat'
        },
        underscore: {
            exports: '_'
        }
    }
};