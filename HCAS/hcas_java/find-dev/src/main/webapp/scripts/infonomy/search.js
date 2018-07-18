jQuery(function ($) {
  var $content    = $('.content');
  var $newsPage   = $('#initialContent');
  var $searchPage = $('#searchResults');
  var $agentPage  = $('#agentResults');

  $content.infonomy({
    '.sidebar': {
      'title'         : 'Title',
      'html'          : 'Description',
      'button'        : false,
      'buttonMargin'  : '100px auto 0',
      'height'        : '200px',
      'popupMargin'   : '10px 10px'
    }
  });

  $searchPage.infonomy({
    '.resultsList': {
      'title'         : 'Title',
      'html'          : 'Description',
      'button'        : false,
      'buttonMargin'  : '100px auto 0',
      'height'        : '200px',
      'popupMargin'   : '10px 10px'
    }
  });

  $newsPage.infonomy({
    '.initialContentList': {
      'title'         : 'Title',
      'html'          : 'Description',
      'button'        : false,
      'buttonMargin'  : '100px auto 0',
      'height'        : '200px',
      'popupMargin'   : '10px 10px'
    }
  });

  $agentPage.infonomy({
    '.agentMainContainer': {
      'title'         : 'Title',
      'html'          : 'Description',
      'button'        : false,
      'buttonMargin'  : '100px auto 0',
      'height'        : '200px',
      'popupMargin'   : '10px 10px'
    }
  });
});