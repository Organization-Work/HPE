$(document).ready(function() {

    // #####################################
    // Variable/object definitions
    // #####################################

    var rolesURL = CONTEXT_PATH + "/api/admin/roles.json",
        privilegeURL = CONTEXT_PATH + "/api/admin/privilege.json"

    var rolesTable = {
        $list:  $("#rolesList"),
        setupTable: function () {
            var that = this
            $("#newRole").click(function (e) {
                e.preventDefault()
                $("#createNewRoleModal").modal('show')
//                that.addNewRow();
            })

            $("#model-delete-confirm").click(function () {
                var roleName = $('#role-delete-name-modal').text()
                $.ajax({
                    url: rolesURL + '?' + $.param({"rolename": roleName}),
                    type: 'DELETE',
                    success: function (data) {
                        if(data.success) {
                            that.removeRow(roleName)
                            $("#deletedRole").showAndFadeOut()
                        } else {
                            $("#failedToDeleteRole").showAndFadeOut()
                        }
                    },
                    error: $.noop
                });
            })

            $("#new-role-submit").click(function (e) {
                e.preventDefault();
                $.ajax({
                    url: rolesURL,
                    type: 'POST',
                    data: $("#modal-form").serialize(),
                    success: function () {
                        location.reload();
                    },
                    error: $.noop
                })
            })

            $.getJSON(rolesURL, function (res) {
                if(res.success && res.result) {
                    _.forEach(res.result, function (role) {
                        var row =  that.addNewRow(role);

                    })
                }
            })

        },
        getRow: function (rowName) {
            var rows = $("#rolesTable tbody tr td").filter(function() {
                            return $(this).text() == rowName
                        })
            if(rows.length > 0) {
                return $(rows[0]).parent();
            }
            return null;
        },
        removeRow: function (rowName) {
            var row = this.getRow(rowName)
            if(row) {
                row.remove()
                return true
            }
            return false
        },
        addNewRow: function (role) {
            var row = $(this.getNewRow().prependTo(this.$list));

            $(row.find(".manage-role")[0]).attr("href", CONTEXT_PATH + "/admin/roleDetails.do?roleName=" + role)
            $(row.children(".roleName")[0]).text(role)

            $(row.find(".delete-role")[0]).click(function () {
                $('#role-delete-name-modal').text(role)
                $('#confirmDeleteModal').modal('show');
            })
        },
        getNewRow: function () {
            return $($("#roleRowTemplate").html());
        }
    }

    var privilegeTable = {
        $list:  $("#privilegeList"),
        setupTable: function () {
            var that = this
            $.getJSON(privilegeURL, function (res) {
                _.forEach(res.result, function (role) {
                    var row =  that.addNewRow();
                    $(row.children(".privilegeName")[0]).text(role)
                    $(row.find(".btn")[0]).attr('href', CONTEXT_PATH + "/admin/privilegeDetails.do?privilege=" + role)
                })
            })
        },
        addNewRow: function () {
            return $(this.getNewRow().prependTo(this.$list));
        },
        getNewRow: function () {
            return $($("#privilegeRowTemplate").html());
        }
    }


    // #####################################
    // Main
    // #####################################

    privilegeTable.setupTable();
    rolesTable.setupTable();

});