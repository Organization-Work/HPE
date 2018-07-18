jQuery(function($) {

  SearchSettings.getDatabases = function(){
      return _.reduce(databases.results, function(arr, val,key){
          if (val) {
              arr.push(key);
          }
          return arr;
      }, []);
  };

  SearchSettings.getDisplayChars = function() {
      return displayChars;
  };

  SearchSettings.getMinScore = function() {
      return minScore;
  };

  var CHAR_DIFFERENCE = 50;
  var MINSCORE_DIFFERENCE = 10;
  var UPDATE_SETTINGS_URL = 'ajax/settings/updateSettings.json';
  var RETRIEVE_SETTINGS_URL = 'ajax/settings/retrieveSettings.json';
  var TOGGLE_BUTTON = ' <button class="btn-switch btn" data-db="<%= name %>"><i class="icon icon-white"></i> <%= (name[0].toUpperCase() + name.slice(1)) %></button> ';

  var $toggleSettings = $('#toggleSettings');
  var $settings = $('#settings');
  var $resultsDbs = $('#resultsDbs');
  var $suggestionsDbs = $('#suggestionsDbs');
  var $results = $('#searchResults');
  var $suggestions = $('#suggestions');
  var $chars = $('#chars');
  var $minScore = $('#minScore');
  var $sidebar = $('.sidebar');
  var $searchOptions = $('.search-options');
  var $filterTabs = $('.filter-tabs');

  /* Toggling the search settings panel */
  $toggleSettings.toggler({
    '.sidebar' : {
      active : 'with-options'
    },
    '& i' : {
      active : 'icon-chevron-up',
      inactive : 'icon-chevron-down'
    }
  }, true);

  $filterTabs.find('.hide-settings').click(function () {
    $sidebar.removeClass('with-options');
    $filterTabs.find('.active').removeClass('active');
    $searchOptions.find('.tabpage.active').removeClass('active');
  });

  $filterTabs.find('.options-tab').click(function () {
    $sidebar.addClass('with-options');
  });

  var databases = {
    results : {},
    suggestions : {}
  };
  var displayChars = 200;
  var minScore = 0;
  var loadedControls = false;

  /* Builds an object detailing the user's current settings */
  var settingsAsJSON = function(dbs, chars) {
    return {
      databases : dbsAsJSON(dbs.results),
      suggestionDatabases : dbsAsJSON(dbs.suggestions),
      displayCharacters : displayChars,
      minScore: minScore
    };
  };

  /* Converts a database array to the format the server requires */
  var dbsAsJSON = function(dbs) {
    return _.chain(dbs).map(function(v, k) {
      return v ? k : null;
    }).reject(_.isNull).value().join(',');
  };

  /* Tells the server the new settings */
  var updateSettings = (function() {
    var xhr;
    return function() {
      if (xhr) {
        xhr.abort();
        xhr = null;
      }
      xhr = $.ajax({
        url : UPDATE_SETTINGS_URL,
        data : settingsAsJSON(databases, displayChars),
        success : function() {
          Util
              .log('Successfully informed the server of new settings.');
        },
        error : function() {
          Util.log('Error informing the server of settings.');
          _.delay(updateSettings, 5000);
        }
      });
    };
  })();

  /* Asks the server for the user's current settings */
  var retrieveSettingsNow = function() {
    $.ajax({
      url : RETRIEVE_SETTINGS_URL,
      success : function(results) {
        var settings = processSettings(results);
        if (!loadedControls) {
          createButtons(settings);
        }
        applySettings(settings);
      },
      error : function() {
        _.delay(retrieveSettings, 5000);
      }
    })
  };
  var retrieveSettings = _.debounce(retrieveSettingsNow, 1000);

  /* Split up the database strings, and cast the display chars */
  var processSettings = function(settingsData) {
    var splitUp = function (v) { return (v && v.split(',')) || []; };
    return ({
      displayCharacters: (+settingsData.displayCharacters) || 200,
      databases: splitUp(settingsData.databases),
      minScore: settingsData.minScore || 0,
      suggestionDatabases: splitUp(settingsData.suggestionDatabases),
      allDatabases: splitUp(settingsData.allDatabases),
      allSuggestionDatabases: splitUp(settingsData.allSuggestionDatabases)
    });
  };

  /* Create the togglable button database switches */
  var createButtons = function(settingsData) {
    $resultsDbs.empty();
    _.each(settingsData.allDatabases, function (v, k) {
      $resultsDbs.append(_.template(TOGGLE_BUTTON, { name: v, on: false }));
    });
    $suggestionsDbs.empty();
    _.each(settingsData.allSuggestionDatabases, function (v, k) {
      $suggestionsDbs.append(_.template(TOGGLE_BUTTON, { name: v, on: false }));
    });
    setupTogglable();
  };

  /* Apply the settings loaded from the server */
  var applySettings = function(settingsData) {
    displayChars = settingsData.displayCharacters;
    $chars.val(displayChars);
    minScore = settingsData.minScore;
    $minScore.val(minScore);

    //  Reset all to false (search databases)
    _.each(databases.results, function(v, k) {
      databases.results[k] = false;
      $resultsDbs.find('[data-db^="' + k + '"]').toggler('off');
      switchDB(k, false, $results, 'results');
    });
    //  Apply new settings (search databases)
    _.each(settingsData.databases, function(s) {
      databases.results[s] = true;
      $resultsDbs.find('[data-db^="' + s + '"]').toggler('on');
      switchDB(s, true, $results, 'results');
    });

    //  Reset all to false (suggestion databases)
    _.each(databases.suggestions, function(v, k) {
      databases.suggestions[k] = false;
      $suggestionsDbs.find('[data-db^="' + k + '"]').toggler('off');
      switchDB(k, false, $suggestions, 'suggestions');
    });
    //  Apply new settings (suggestion databases)
    _.each(settingsData.suggestionDatabases, function(s) {
      databases.suggestions[s] = true;
      $suggestionsDbs.find('[data-db^="' + s + '"]').toggler('on');
      switchDB(s, true, $suggestions, 'suggestions');
    });

    updateSuggestionLabels();
  };

  /* Characters */
  $chars.change(function() {
    updateCharCount(0);
  });

  /* Removes invalid characters from a character count value */
  var correctCharValue = function(v) {
    return ('' + v).replace(/\D+/g, '') | 0;
  };

  /* Updates the character count with a difference
   * Attempts to keep the value valid */
  var updateCharCount = function(diff) {
    var val = correctCharValue($chars.val());
    displayChars = Math.max((val + diff) | 0, CHAR_DIFFERENCE);
    $chars.attr('value', displayChars);
    updateSettings();

    SearchSettings.$.trigger(SearchSettings.DISPLAYCHARSCHANGE, [displayChars]);
  };

  $('#incChars').click(function() {
    updateCharCount(CHAR_DIFFERENCE);
  });

  $('#decChars').click(function() {
    updateCharCount(-CHAR_DIFFERENCE);
  });

  /* Min Score */

  $minScore.change(function(){
      updateMinScore(0);
  });

  $('#incMinScore').click(function(){
      updateMinScore(MINSCORE_DIFFERENCE);
  });

  $('#decMinScore').click(function(){
      updateMinScore(-MINSCORE_DIFFERENCE);
  });

  function updateMinScore(diff) {
      var val = $minScore.val().replace(/[^0-9.]/g, '');
      minScore = Math.max(0, Math.min((Number(val) || 0) + diff, 100));
      $minScore.val(minScore);
      updateSettings();

      SearchSettings.$.trigger(SearchSettings.MINSCORECHANGE, [minScore]);
  }

  /* Databases */
  var setupTogglable = function () {
    $settings.find('.btn-switch').toggler({
      'this' : {
        active : 'btn-success',
        inactive : 'btn-danger'
      },
      '& i' : {
        active : 'icon-ok',
        inactive : 'icon-minus'
      }
    }, true);
  };

  var switchClick = function(parent, dataStore) {
    return function() {
      var $this, db, items, state;
      $this = $(this);
      db = $this.data('db');
      state = $this.toggler('status');
      switchDB(db, state, parent, dataStore);

      SearchSettings.$.trigger(SearchSettings.DATABASECHANGE, [SearchSettings.getDatabases()]);

      updateSettings();
    };
  };

  var switchDB = function(db, state, parent, dataStore) {
    var items;
    items = $('.db-' + db, parent);
    databases[dataStore][db] = state;
    if (state) {
      items.removeClass('squish');
    } else {
      items.addClass('squish');
    }
    if (dataStore === 'suggestions') {
      $results.find('.suggestdb-' + db).toggler(state ? 'on' : 'off');
    }
  };

  //  Click handlers for all switches that may occur
  $resultsDbs.find('.btn-switch').live('click',
      switchClick($results, 'results'));
  $suggestionsDbs.find('.btn-switch').live('click',
      switchClick($suggestions, 'suggestions'));

  var determineValue = function() {
    $resultsDbs.find('[data-db]').each(function() {
      var $this = $(this);
      databases.results[$this.data('db')] = $this.toggler('status');
    });

    $suggestionsDbs.find('[data-db]').each(function() {
      var $this = $(this);
      databases.suggestions[$this.data('db')] = $this.toggler('status');
    });
  };
  determineValue();

  SearchEvents.$.on(SearchEvents.SUGGESTIONS_PROCESSING, function(e, results) {
    _.each(results.mainClusters.concat(results.clusters), function(result, index) {
      result.classes = (databases.suggestions[result.database] ? '' : 'squish');
    });
  });
  
  /*

  SearchEvents.$.on(SearchEvents.RESULTS_PROCESSING, function(e, results, totalResults, searchData) {
    _.each(results, function(result, index) {
      result.classes = (databases.results[result.database] ? '' : 'squish');
    });
  });
  */
  
  var updateSuggestionLabels = function() {
    _.each(databases.suggestions, function(state, db) {
      $results.find('.suggestdb-' + db).toggler(state ? 'on' : 'off');
    });
  };

  SearchEvents.$.on(SearchEvents.LOADED_BOTH, function() {
    updateSuggestionLabels();
  });

  retrieveSettingsNow();
});