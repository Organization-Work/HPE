jQuery(function ($) {
  $.fn.rect = function () {
    var $this = $(this[0]);
    var offset = $this.offset();
    if (offset) {
      var result = {
        left: offset.left,
        top: offset.top,
        width: $this.outerWidth(),
        height: $this.outerHeight()
      };
      return result;
    }
  };
});

jQuery(function ($) {
  var DATA = 'infonomy-data';
  var ACTIVE_ID = 'infonomy-active';
  var MODEL = 'infonomy-model';
  var OVERLAY = '<div class="infonomy-overlay" data-infonomy="{id}"></div>';
  var POPUP = '<div class="infonomy-area infonomy-fader" style="left:{left}px;top:{top}px;width:{width}px;height:{height}px;"><button class="infonomy-btn infonomy-btn-info {popupClass}" style="width:{buttonWidth};margin:{buttonMargin};">{buttonText}</button><div class="infonomy infonomy-popup infonomy-fader {popupClass} {closeOnClick}" style="width:{X};height:{Y};margin:{popupMargin};"><div class="infonomy-popup-content"><button class="infonomy-close-btn infonomy-btn {popupClass}">x</button><h3 class="infonomy">{title}</h3><div class="infonomy infonomy-html">{html}</div></div></div></div>';
  var defaults = {
    buttonText: 'What\'s this?',
    buttonWidth: '100px',
    buttonMargin: 'auto',
    popupMargin: 'auto',
    closeOnClick: false
  };

  var id = 0;

  $.fn.infonomy = function (_command, _other) {
    //  Setup
    if ($.isPlainObject(_command)) {
      return commands.setup(this, _command);
    }
    //  Other command
    return commands[_command](this, _other);
  };

  $('.infonomy-btn').live('click', function () {
    var $this = $(this);
    if ($this.hasClass('infonomy-close-btn')) {
      $this.parents('.infonomy-popup').not('.closeOnClick').toggleClass('infonomy-show');
    } else {
      $this.siblings('.infonomy-popup').toggleClass('infonomy-show');
    }
  });

  $('.infonomy-popup.closeOnClick:not(.infonomy-noBtn)').live('click', function () {
    $(this).toggleClass('infonomy-show');
  });

  var template = function (view, model, pattern, delimiter) {
    return view.replace(pattern||/\{([^\{\}]+)\}/gi, function (match, key) {
      return travel(model, key, delimiter) || '';
    });
  };

  var travel = function (store, path, delimiter) {
    var parts, result, part, i, m;
    parts = path.split(delimiter || '.');
    result = store;
    for (i = 0, m = parts.length; i < m; i += 1) {
      part = parts[i];
      if (part in result) {
        result = result[part];
      } else {
        return undefined;
      }
    }
    return result;
  };

  var pxToInt = function (str) {
    return str.replace(/\D+/gi, '') | 0;
  };

  var commands = {
    setup: function ($elems, options) {
      return $elems.data(DATA, options);
    },
    show: function ($elems) {
      $elems.each(function () {
        var $this = $(this);
        if ($this.data(ACTIVE_ID)) {
          return;
        }
        var parentOffset = {left:0,top:0};
        if ($this.css('position') !== 'static') {
          parentOffset = $this.offset();
        }
        var data = $this.data(DATA) || {};
        var popups = $();
        $.each(data, function (selector, model) {
          var $target = $(selector);
          if ($target.length === 0) { return; }
          if ($target.css('display') === 'none') { return; }
          var rect = $target.rect();
          if (!rect) { return; }
          rect.left -= parentOffset.left - (model.offsetX|0);
          rect.top -= parentOffset.top - (model.offsetY|0);
          var icon = $(template(POPUP, $.extend({
            title: model.title,
            html: model.html,
            X: model.width,
            Y: model.height,
            popupMargin: model.popupMargin || defaults.popupMargin,
            popupClass: model.button ? '' : 'infonomy-noBtn',
            closeOnClick: (model.closeOnClick || defaults.closeOnClick) ? 'closeOnClick' : '',
            buttonText: model.buttonText     || defaults.buttonText,
            buttonWidth: model.buttonWidth   || defaults.buttonWidth,
            buttonMargin: model.buttonMargin || defaults.buttonMargin
          }, rect)));
          popups = popups.add(icon);
        });

        id += 1;
        var $overlay = $(template(OVERLAY, { id: id }));
        $this.append($overlay);
        $overlay.html('').append(popups);
        $this.data(ACTIVE_ID, id);
        setTimeout(function () {
          popups.addClass('infonomy-show');
        });
      });
    },
    hide: function ($elems) {
      $elems.each(function () {
        var $this = $(this);
        var id = $this.data(ACTIVE_ID);
        if (id) {
          $this.data(ACTIVE_ID, false);
          var infonomyContainer = $this.find('[data-infonomy^="'+id+'"]');
          infonomyContainer.find('.infonomy-area').removeClass('infonomy-show');

          setTimeout(function () {
            infonomyContainer.remove();
          }, 500);
        }
      });
    },
    toggle: function ($elems) {
      $elems.each(function () {
        var $this = $(this);
        var id = $this.data(ACTIVE_ID);
        $this.infonomy(id ? 'hide' : 'show');
      });
    }
  };
});