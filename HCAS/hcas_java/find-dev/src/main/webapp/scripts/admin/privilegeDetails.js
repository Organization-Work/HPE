$(document).ready(function(){
    var initialRoles = getListedRoles(),
        roles = {toAdd: [], toRemove: [], itemName: $("#priviName").text()},
        $privilegeSaveSuccess = $("#privilegeSuccess"),
        $privilegeSaveFailed = $("#privilegeFailed")

    $.getJSON(CONTEXT_PATH + "/api/admin/roles.json", function (res) {
        if(res.result) {
            var $roleSelect = $("#rolesList")
            $roleSelect.chosen({
                width: "600px",
                placeholder_text_multiple: "No roles attached to this privilege. Please select a role."
            })

        } else {
            //$("#rolesList")
        }
    })

    $("#savePrivilege").click(function() {
        disableSaveButton();

        $.ajax({
            url: CONTEXT_PATH + "/api/admin/privilege.json",
            type:"POST",
            data: JSON.stringify(roles),
            contentType:"application/json; charset=utf-8",
            success:  function (data) {
                if(!data.success) {
                    return $privilegeSaveFailed.showAndFadeOut()
                }
                initialRoles = getListedRoles()
                return $privilegeSaveSuccess.showAndFadeOut()
            }
        }).fail(function() {
                enableSaveButton()
                $privilegeSaveFailed.showAndFadeOut()
        })
    });

    $("#rolesList").on('change', function(event) {
        resolveSaveNeeded();
    });

    function getListedRoles() {
        return $("#rolesList").val()
    }

    function resolveSaveNeeded() {
        var currentRoles = getListedRoles()
        roles.toRemove = _.difference(initialRoles, currentRoles)
        roles.toAdd = _.difference(currentRoles, initialRoles)

        setButton()
    }

    function setButton() {

        if(roles.toRemove.length > 0 || roles.toAdd.length > 0) {
            return enableSaveButton()
        }
        disableSaveButton()
    }

    function disableSaveButton() {
        var $saveButton = $('#savePrivilege')
        return $saveButton.disableAndRemoveClass("btn-default", "btn-success")
    }

    function enableSaveButton() {
        var $saveButton = $('#savePrivilege')
        return $saveButton.enableAndAddClass("btn-default", "btn-success")
    }

});