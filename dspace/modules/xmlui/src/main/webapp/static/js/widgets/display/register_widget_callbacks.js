(function() {
'use strict';
var elt, i
  , origin = "http://rnathanday.github.io" 
  , downloads = document.getElementsByClassName('dryad-ddw-download')
  , cites = document.getElementsByClassName('dryad-ddw-cite')
  , shares = document.getElementsByClassName('dryad-ddw-share')
  , zooms = document.getElementsByClassName('dryad-ddw-zoom');
function set_onclick(elts, data) {
    for (i = 0; i < elts.length; i++) {
        elts[i].onclick = function(evt) {
            window.parent.postMessage(data, origin);
            evt.preventDefault();
        };
    }
};
set_onclick(cites,     {"action" : "cite",     "data" : document.getElementById("dryad-ddw-citation").cloneNode(true).outerHTML });
set_onclick(shares,    {"action" : "share",    "data" : document.getElementById("dryad-ddw-share").cloneNode(true).outerHTML });

// remove controls from the zoomed widget
var zoomc = document.getElementsByClassName("dryad-ddw")[0].cloneNode(true);
var controls = zoomc.getElementsByClassName('dryad-ddw-control');
for (i = 0; i < controls.length; i++) {
    controls[i].parentNode.removeChild(controls[i]);
}
zoomc.getElementsByClassName('dryad-ddw-frame')[0].classList.add('dryad-ddw-frame-full');
set_onclick(zooms, {"action" : "zoom", "data" : zoomc.outerHTML} );
})();
