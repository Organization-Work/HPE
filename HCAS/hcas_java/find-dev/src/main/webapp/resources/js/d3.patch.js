if (typeof CSSStyleDeclaration === 'undefined') {
    try {
      document.createElement("div").style.setProperty("opacity", 0, "");
    } catch (error) {
        window.CSSStyleDeclaration = function(){};
        CSSStyleDeclaration.prototype = {
            setProperty: function() {
                alert('CSSStyleDeclaration not supported');
            }
        };
    }
}