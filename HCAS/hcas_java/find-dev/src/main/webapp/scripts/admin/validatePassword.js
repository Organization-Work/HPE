// Used in both creating a users and updating a users password.
function validatePassword(password) {
    var errorArray = [],
        regTest = new RegExp("^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])"),
        min = 8,
        max = 32

    if(!regTest.test(password)) {
        errorArray.push("Please ensure the password contains at least one capital letter and one number.")
    }

    if(!(password.length && password.length >= min && password.length <= max)) {
        errorArray.push("Please ensure the password has a length of between " + min + " and " + max)
    }

    return errorArray.length === 0 ? null : errorArray
}

function resetUserPassword (userObject, successCallback, failedCallback) {
    var userRoute = CONTEXT_PATH + "/api/admin/users.json"
    $.ajax({url: userRoute + "?username=" + userObject.username +"&newPassword=" +  userObject.newPassword,
        type: 'PUT'
    }).done(function (data) {
            if(data.success) {
                return successCallback()
            }
            failedCallback()
        })
}






