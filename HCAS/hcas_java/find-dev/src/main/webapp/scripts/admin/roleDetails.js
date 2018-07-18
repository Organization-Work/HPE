$(document).ready(function() {
   var roleName = $("#roleName").text(),
        userObject = {
            initial : getListedUsers(),
            saveObject : {toAdd: [], toRemove: [], itemName: roleName},
            getCurrent: getListedUsers
        },
        privObject = {
            saveObject : {toAdd: [], toRemove: [], itemName: roleName},
            initial : getListedPrivileges(),
            getCurrent: getListedPrivileges
        },
       $userSaveSuccess = $("#userSuccess"),
       $userSaveFailed = $("#userFailed"),
       $privilegeSaveSuccess = $("#privilegeSuccess"),
       $privilegeSaveFailed = $("#privilegeFailed")

    setupLists(userObject, privObject);


    function setupLists(userObject, privObject) {
        var $userList = $("#userList"),
            $privilegeList = $("#privilegeList")
        $userList.chosen({
            width: "600px",
            placeholder_text_multiple: "No user attached to this role. Please select a user."
        })
        $privilegeList.chosen({
            width: "600px",
            placeholder_text_multiple: "No privilege attached to this role. Please select a privilege."
        })

        $userList.on('change', function() {
            resolveSaveNeeded(userObject.initial, userObject.getCurrent(), userObject.saveObject, $("#userSave"));
        });
        $privilegeList.on('change', function() {
            resolveSaveNeeded(privObject.initial, privObject.getCurrent(), privObject.saveObject, $("#privilegeSave"));
        });

        $("#privilegeSave").click(function() {
            var $this= $(this)
            disableSaveButton($this);

            $.ajax({
                url: CONTEXT_PATH + "/api/admin/roles.json?type=privilege",
                type:"POST",
                data: JSON.stringify(privObject.saveObject),
                contentType:"application/json; charset=utf-8",
                success:  function (data) {
                    if(!data.success) {
                        return $privilegeSaveFailed.showAndFadeOut()
                    }
                    privObject.initial = privObject.getCurrent()
                    return $privilegeSaveSuccess.showAndFadeOut()
                }
            }).fail(function() {
                    enableSaveButton($this)
                    $privilegeSaveFailed.showAndFadeOut()
            })
        });

        $("#userSave").click(function() {
            var $this= $(this)
            disableSaveButton($this);

            $.ajax({
                url: CONTEXT_PATH + "/api/admin/roles.json?type=user",
                type:"POST",
                data: JSON.stringify(userObject.saveObject),
                contentType:"application/json; charset=utf-8",
                success:  function (data) {
                    if(!data.success) {
                        return $userSaveFailed.showAndFadeOut()
                    }
                    userObject.initial = userObject.getCurrent()
                    $userSaveSuccess.showAndFadeOut()
                }
            }).fail(function() {
                    enableSaveButton($this)
                    $userSaveFailed.showAndFadeOut()
            })
        });


    }

    function getListedPrivileges() {
        return $("#privilegeList").val()
    }

    function getListedUsers() {
        return $("#userList").val()
    }

    function resolveSaveNeeded(initial, current, saveObject, $saveButton) {
        saveObject.toRemove = _.difference(initial, current)
        saveObject.toAdd = _.difference(current, initial)
        setButton($saveButton, saveObject)
    }

    function setButton($saveButton, saveObject) {

        if(saveObject.toRemove.length > 0 || saveObject.toAdd.length > 0) {
            return enableSaveButton($saveButton)
        }
        disableSaveButton($saveButton)
    }

    function disableSaveButton($saveButton) {
        return $saveButton.disableAndRemoveClass("btn-default", "btn-success")
    }

    function enableSaveButton($saveButton) {
        return $saveButton.enableAndAddClass("btn-default", "btn-success")
    }

})