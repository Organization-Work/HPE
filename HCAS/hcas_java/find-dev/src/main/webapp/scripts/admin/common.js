
$(document).ready(function () {

    $(document).ready(function() {
        if (location.hash !== '') $('a[href="' + location.hash + '"]').tab('show');
        return $('a[data-toggle="tab"]').on('shown', function(e) {
            return location.hash = $(e.target).attr('href').substr(1);
        });
    });

    $.fn.showAndFadeOut = function (timeout) {
        var that = this
        var fadeOutTime = 5000
        if (timeout && typeof timeout === "number") {
            fadeOutTime = timeout
        }
        that.fadeIn(function () {
            setTimeout(function () {
                that.fadeOut()
            }, fadeOutTime)
        })
    }

    $.fn.serializeObject = function() {
        var o = {};
        var a = this.serializeArray();
        $.each(a, function() {
            if (o[this.name] !== undefined) {
                if (!o[this.name].push) {
                    o[this.name] = [o[this.name]];
                }
                o[this.name].push(this.value || '');
            } else {
                o[this.name] = this.value || '';
            }
        });
        return o;
    };

    $.fn.enableAndAddClass = function(classToRemove, classToAdd) {
        if(!classToAdd) {
            classToAdd = classToRemove
            classToRemove = null
        }
        if(classToAdd && typeof classToAdd === "string") {
            this.removeAttr('disabled')
            if(classToRemove) {
                this.removeClass(classToRemove)
            }
            this.addClass(classToAdd)
        }
    }

    $.fn.disableAndRemoveClass = function(classToAdd, classToRemove) {
        if(!classToRemove) {
            classToRemove = classToAdd
            classToAdd = null
        }
        if(classToRemove && typeof classToRemove === "string") {
            this.attr('disabled', true)
            if(classToAdd) {
                this.addClass(classToAdd)
            }
            this.removeClass(classToRemove)
        }
    }


})
