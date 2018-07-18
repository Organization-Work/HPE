$(document).ready(function(){
	
    var $superUserPasswordResetButton = $("#updateSuperPassword");

    $('#superPasswordForm').find('.input').keypress(function(e){
        e.preventDefault()
        // Hitting enter will now submit the password reset
        if ( e.which == 13 ) {
            // Enter key = keycode 13
            return $superUserPasswordResetButton.click();
        }
    });

    function setupSuperUserResetButton () {
        $superUserPasswordResetButton.click(function () {
           $("#failedToChangeSuper").hide()
           var $newSuperPassword = $("#newSuperPassword");
		   var $newSuperConfirmPassword = $("#newSuperConfirmPassword");
           if(!$newSuperPassword.val() || !$newSuperConfirmPassword.val() || $newSuperPassword.val() !== $newSuperConfirmPassword.val()) {
		       return  alert("Passwords do not match");
		   }
           if(!$newSuperPassword.val() || validatePassword($newSuperPassword.val()) !== null) {
               return  alert("Password must contain between 8-32 character, have one capital letter and one number")
           }

           var paramsObject = {
               newPassword: $("#newSuperPassword").val(),
               username: "superuser"
           }

           resetUserPassword(paramsObject, function () {
               $("#superPasswordForm").hide()
               $("#superResetConfirm").html("Password has been reset, click logout and log back in with the new password.")
               $("#superResetConfirm").show()
               $("#updateSuperPasswordLogout").show()
               $superUserPasswordResetButton.hide()
           },
           function () {
               $("#failedToChangeSuper").show()
           })
       })
    }

    if(getParameterByName('defaultSuperUserWarn')) {
        setupSuperUserResetButton()
        return $('#superUserChangePassword').modal({backdrop:'static',keyboard:false, show:true});
    }

    setAjaxLoadTimers()
    setupUserTable(function () {
        setUpClickHanlers()
});


    function setupUserTable(callback) {
        var userRoute = CONTEXT_PATH + "/api/admin/users.json",
            $datatable = $('#userTable');
        
        $.ajax({url: userRoute
        }).done(function(data) {
                var unknownString = "Unknown"
                var users = [];
                var parsedUsers = JSON.parse(data.result)
                _.forEach(parsedUsers, function (user) {

                    if(user.locked === undefined) {
                        user.locked = "Unknown"
                    }

                    var userArray = [
                            user.uid,
                            user.username,
                            user.firstname,
                            user.lastname,
                            user.email,
                            user.lastLoggedIn,
                            user.locked
                        ]
                    userArray.push("<a href= '" + CONTEXT_PATH + "/admin/userdetails.do?username="+user.username+"'> Edit </a>");
                    users.push(userArray);
                })

                $datatable.dataTable({
                    "aaData": users,
                    "aoColumns": [
                        { "sTitle": "UID" },
                        { "sTitle": "Username" },
                        { "sTitle": "First Name" },
                        { "sTitle": "Second Name" },
                        { "sTitle": "Email" },
                        { "sTitle": "Last Login Time" },
                        { "sTitle": "Locked" },
                        { "sTitle": "Details" }
                    ]
                
                })
        	});
            callback && callback();
    }

    function setUpClickHanlers() {
        var userRoute = CONTEXT_PATH + "/api/admin/users.json",
            $create = $("#createNewUser"),
            $newUserName = $("#newUsername"),
            $newUserPassword = $("#newUserPassword"),
            $newUserFirstName = $("#newUserFirstName"),
            $newUserLastName = $("#newUserLastName"),
            $newUserEmail = $("#newUserEmail")

        $create.click(function () {
            $("#createdUser").hide()
            $("#failedToCreateUser").hide()
            if(!$newUserName.val() || !$newUserPassword.val()) {
               return  alert("Please enter valid details.")
            }
            
			var $newUserConfirmPassword = $("#newUserConfirmPassword");
			if($newUserPassword.val() !== $newUserConfirmPassword.val()) {
               return  alert("Passwords do not match.")
            }
			
            var validated = validatePassword($newUserPassword.val())

            if(validated) {
                return alert(validated)
            }
            var paramsObject = {
                username: $newUserName.val(),
                password: $newUserPassword.val(),
                firstname: $newUserFirstName.val() || "N/A",
                lastname: $newUserLastName.val() || "N/A",
                email: $newUserEmail.val() || "N/A"
            }
            var params = $.param(paramsObject)
            $.ajax({url: userRoute + "?" + params,
                type: 'POST'
            }).done(function (data) {
                    if(data.success) {
                        $("#newUserForm").trigger("reset")
                        return $("#createdUser").fadeIn().css("display","inline-block");
                    }
                    return $("#failedToCreateUser").fadeIn().css("display","inline-block");
                })
        })

        $('#newUser').on('hidden', function () {
            return location.reload();
        })


    }

    function setAjaxLoadTimers (passedtime) {
        // Will show the loading modal if an ajax request takes longer than 250 milliseconds
        var timer;
        var time = passedtime || 250
        $(document).ajaxStart(function () {
            // Clears the timer (if it exists) and starts the timer
            timer && clearTimeout(timer);
            timer = setTimeout(function () {
                    $('#loadingDialog').show();
                },
                time);
        }).ajaxStop(function () {
                // Stops the timer and hides the modal
               setTimeout(function () {
                    clearTimeout(timer);
                    $('#loadingDialog').hide();
                },500);
            });
    }

    function getParameterByName(name) {
        name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
        var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
            results = regex.exec(location.search);
        return results == null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
    }

});