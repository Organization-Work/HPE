$(document).ready(function(){
    var $username = $("#username"),
        userRoute = CONTEXT_PATH + "/api/admin/users.json",
        initialRoles = getListedRoles(),
        roles = {toAdd: [], toRemove: [], username: $username.text()},
        $roleSaveSuccess = $("#roleSaveSuccess"),
        $roleSaveFailed = $("#roleSaveFailed")


     

    $("#rolesList").chosen({width: "95%"})
    if($username.length > 0) {
        $.ajax({
                url: userRoute + "?username=" + $username.text()
            }).done(function(data) {
                var user = {}
                var UNKNOWN_STRING = "Unknown";
                var $locked = $("#locked"),
                    $lastlogged = $("#lastlogged"),
                    $uid = $("#uid"),
                    $firstName = $("#firstName"),
                    $lastName = $("#lastName"),
                    $email = $("#email");
                	$unlockText = $("#unlock-user");

                if(data && data.result) {
                    user = JSON.parse(data.result)
                }

                $lastlogged.text(user[0].lastLoggedIn || UNKNOWN_STRING)
                $uid.text((typeof user[0].uid === "undefined" || user[0].uid === null) ? UNKNOWN_STRING : user[0].uid )
                $locked.text((typeof user[0].locked === "undefined" || user[0].locked === null)  ? UNKNOWN_STRING : user[0].locked)
                $unlockText.text(user[0].locked === false ? "Lock": "Unlock")
                $("#newUserEmail").val(user[0].email || UNKNOWN_STRING)
                $email.text(user[0].email || UNKNOWN_STRING)
                $("#newUserFirstName").val(user[0].firstname || UNKNOWN_STRING)
                $firstName.text(user[0].firstname || UNKNOWN_STRING)
                $("#newUserLastName").val(user[0].lastname || UNKNOWN_STRING)
                $lastName.text(user[0].lastname || UNKNOWN_STRING)
            })

        $("#savePrivilege").click(function() {
            disableSaveButton();

            $.ajax({
                url: userRoute,
                type:"POST",
                data: JSON.stringify(roles),
                contentType:"application/json; charset=utf-8",
                success:  function (data) {
                    if(!data.success) {
                        return $roleSaveFailed.showAndFadeOut()
                    }
                    initialRoles = getListedRoles()
                    $roleSaveSuccess.showAndFadeOut()
                }
            }).fail(function() {
                    enableSaveButton()
                    $roleSaveFailed.showAndFadeOut()
                })
        });

        $("#rolesList").on('change', function(event) {
            resolveSaveNeeded();
        });

       /* function getListedRoles() {
            return $("#rolesList").chosen().val()
        }*/

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
        // #####################################
        // Generic warning functions
        // #####################################

        function clearAlert($warningWrapper, $warningList) {
            $warningList.empty()
            $warningWrapper.hide()
            return $warningList;
        }

        function showAlert($warningWrapper, $warningList, errorArray) {
            _.forEach(errorArray, function (error) {
                $warningList.prepend("<li>" + error + "</li>")
            })
            $warningWrapper.fadeIn()
            return $warningWrapper
        }

        // #####################################
        // Global Success
        // #####################################

        function clearGlobalSuccess() {
            var $list = $("#actionSuccessList")
            var $alert = $("#actionSuccess")
            clearAlert($alert, $list)
            return $list;
        }

        function showGlobalSuccess(successArray) {
            var $list = clearGlobalSuccess(),
                $alert = $("#actionSuccess")
            return showAlert($alert, $list, successArray)
        }

        // #####################################
        // Reset password
        // #####################################

        function clearPasswordWarnings() {
            var $warningList = $("#newPasswordWarningList")
            var $newPasswordWarning = $("#newPasswordWarning")
            clearAlert($newPasswordWarning, $warningList)
            return $warningList;
        }

        function showPasswordErrors(errorArray) {
            var $warningList = clearPasswordWarnings(),
                $newPasswordWarning = $("#newPasswordWarning")
            showAlert($newPasswordWarning, $warningList, errorArray)
            return $("#resetPasswordModal")
        }

        // #####################################
        // Click handlers
        // #####################################

        var $delete = $("#model-delete-confirm"),
            $deleteModalButton = $("#deleteUser")

        $deleteModalButton.click(function () {
            $("#confirmDeleteModal").modal("show")
        })

        $delete.click(function () {
            $.ajax({url: userRoute + "?username=" + $username.text(),
                type: 'DELETE'
            }).done(function (data) {
                    if(data.success) {
                       return window.location.replace(CONTEXT_PATH + "/admin/adminview.do");
                    }
                    alert("Failed to delete user.")
                })
        })

        var $showPasswordModal = $("#showresetPasswordModal")

        $showPasswordModal.click(function () {
            $("#resetPasswordModal").modal("show");
        })
        $("#showUnlockUserModal").click(function(){$("#unlockModal").modal("show")})
        var $lockUserModal = $("#model-update");
        $lockUserModal.click(function(){
        	var username =$username.text();
        	var unlock=$unlockText.text() == "Lock"? false: true;
        	 $.ajax({url: userRoute + "?username=" + username +"&unlock=" +  unlock,
                 type: 'PUT'
             }).done(function (data) {
                 if(data.success) {
                	
                     $("#unlockModal").modal("hide");
                     
                     window.location.reload();
                     
                     
                     
                 }
                
             })
        })

       var $resetPassword = $("#resetPassword");
	   

        $resetPassword.click(function () {
            var userObject = {}
            userObject.newPassword = $("#newPassword").val();
            userObject.username = $username.text();
            if(userObject.newPassword !== $("#newConfirmPassword").val()) {			    
				return showPasswordErrors(["Passwords do not match."])
			}
			
            var validated = validatePassword(userObject.newPassword)

            if(validated) {
                return showPasswordErrors(validated)
            }

            $.ajax({url: userRoute + "?username=" + userObject.username +"&newPassword=" +  userObject.newPassword,
                type: 'PUT'
            }).done(function (data) {
                if(data.success) {
                    $("#resetPasswordModal").modal("hide")
                    return showGlobalSuccess(["Updated password."])
                }
               showPasswordErrors(["Failed to update password."])
            })
        })

        var $showUpdateModal = $("#showUpdateUserModal")

        $showUpdateModal.click(function () {
            $("#updateUserModal").modal("show")
        })

        var $updateUserButton = $("#updateUserButton")

        $updateUserButton.click(function () {
           var updateObject =  $("#updateUserForm").serializeObject();
            updateObject.username = $username.text()

            $.ajax({url: userRoute,
                type: 'PUT',
                contentType: "application/json",
                data: JSON.stringify(updateObject)
            }).done(function (data) {
                    if(data.success) {
                        window.location.reload();
                    }
                    $("failedToUpdateUserAlert").show()
                })

        })

    };
    function getListedRoles() {
        return $("#rolesList").chosen().val()
    }

});

