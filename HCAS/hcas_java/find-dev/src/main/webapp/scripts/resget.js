jQuery(function ($) {
  $.resGet = _.memoize(function (url) {
    var result;
    $.ajax({
      async: false,
      url: url,
      complete: function (data) {
        result = data;
      }
    });
    return result.responseText;
  });
});