var RenameAgent = {};

jQuery(function ($) {

  // Constants
  var RENAME_URL = 'ajax/agents/renameAgent.json';

  // UI Components
  var $dialog =      $('#renameDialog.modal');
  var $inputTxt =    $('input.newName', $dialog);
  var $cancelBtn =   $('button.cancel', $dialog);
  var $saveBtn =     $('button.save',   $dialog);
  var $errorSaving = $('.errorSaving',  $dialog);
  var $indicator =   $('.indicator',    $dialog);

  // State
  var showing =      false;
  var currentAgent =  null;
  var renameCallbacks = {};

  //  Define the modal
  $dialog.modal({
    keyboard: false,
    backdrop: 'static',
    show:     false
  });

  /* Resets the form */
  var reset = function () {
    $inputTxt.val('');
    currentAgent = null;
    renameCallbacks = {};
    enableControls();
    hideError();
    hideIndicator();
  };

  /* Shows the rename dialog */
  var show = function (agent, callbacks) {
    if (!agent || !agent.aid || !_.isObject(agent)) { throw "Not an agent object."; }
    if (showing) { return; }
    showing = true;
    reset();
    $dialog.modal('show');
    currentAgent = agent;
    $inputTxt.val(agent.name);
    renameCallbacks = callbacks;
  };

  /* Hide the rename dialog */
  var hide = function () {
    $dialog.modal('hide');
    reset();
    showing = false;
  };

  /* Submits the new name to the server */
  var rename = function (callbacks) {
    $.ajax({
      url:      RENAME_URL,
      data:     {aid: currentAgent.aid, newName: $inputTxt.val()},
      success:  callbacks.success || $.noop,
      error:    callbacks.error || $.noop,
      complete: callbacks.complete || $.noop
    });
  };

  /* Returns the validitiy of the new name */
  var validate = function () {
    return $.trim($inputTxt.val()) !== '';
  };

  /* Disable Save & Cancel */
  var disableControls = function () {
    $saveBtn.prop('disabled', true);
    $cancelBtn.prop('disabled', true);
  };

  /* Enable Save & Cancel */
  var enableControls = function () {
    $saveBtn.prop('disabled', false);
    $cancelBtn.prop('disabled', false);
  };

  /* Show Error Message */
  var showError = function () {
    $errorSaving.stop(true, true).fadeIn('slow');
  };

  /* Hide Error Message */
  var hideError = function () {
    $errorSaving.stop(true, true).hide();
  };

  /* Show Activity Indicator */
  var showIndicator = function () {
    $indicator.find('.bar').css('width', '0%');
    $indicator.stop(true, true).fadeIn('slow');
    _.delay(function () {
      $indicator.find('.bar').css('width', '100%');
    });
  };

  /* Hide Activity Indicator */
  var hideIndicator = function () {
    $indicator.stop(true, true).hide();
  };

  /* Attempts to save the new name */
  var save = function () {
    if ($saveBtn.is(':disabled')) { return; }
    if (validate()) {
      disableControls();
      hideError();
      showIndicator();
      rename({
        success: function () {
          (renameCallbacks.success || $.noop)($inputTxt.val());
          hide();
        },
        error: function () {
          showError();
          hideIndicator();
          enableControls();
        }
      });
    }
  };

  // Event handlers
  $saveBtn.click(save);
  $cancelBtn.click(function () {
    (renameCallbacks.cancel || $.noop)();
    hide();
  });

  // Public methods
  RenameAgent.show = show;
  RenameAgent.hide = hide;
});