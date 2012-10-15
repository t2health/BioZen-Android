$(document).bind("mobileinit", function() {
    $.mobile.defaultPageTransition = 'none';
    $.mobile.defaultDialogTransition  = 'none';
    $.mobile.pushStateEnabled = false;
    $.mobile.fixedToolbars.setTouchToggleEnabled(false);
    $.mobile.fixedToolbars.show(true);
    $.mobile.page.prototype.options.domCache = false;
    $.mobile.page.prototype.options.backBtnText  = "Back";
    $.mobile.page.prototype.options.addBackBtn   = true;
    $.mobile.page.prototype.options.backBtnTheme = null;
    
});

var preventBehavior = function(e) {
    e.preventDefault();
};

function close() {
    var viewport = document.getElementById('viewport');
    viewport.style.position = "relative";
    viewport.style.display = "none";
}

$('div[data-role="collapsible"]').live('expand', function (event, ui) {
    var targ = event.currentTarget;
    var offie = $(targ).offset();
    $(window).scrollTop(offie.top - 60);
});

$('div.ui-collapsible-contain').live('expand', function() {
    var lastExpanded;
    $(this).hide().trigger('updatelayout');
    var $expandable = $(this);
    var intervalId = setInterval(function() {
        if (lastExpanded && lastExpanded.has( ".ui-collapsible-heading-collapsed" )) {
            var expandableTop = $expandable.offset().top,
            $window = $(window),
            targetPos = expandableTop - $window.scrollTop() + $expandable.height();
            if (targetPos > $window.height() || expandableTop < $window.scrollTop()) {
                $.mobile.silentScroll(expandableTop);
            }
            clearInterval(intervalId);
            lastExpanded = $expandable;
        } else {
            lastExpanded = $expandable;
        }
    }, 200);
});