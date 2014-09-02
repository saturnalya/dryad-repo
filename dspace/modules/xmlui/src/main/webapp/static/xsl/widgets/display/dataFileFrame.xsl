<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="1.0">
    
    <xsl:output method="text"/>
    
    <xsl:param name="ddwcss"        select="'http://rnathanday.github.io/dryad-data-display-widget/script/large_widget.css'"/>
    <xsl:param name="jqlib"         select="'http://ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js'"/>
    <xsl:param name="lblib"         select="'http://rnathanday.github.io/dryad-data-display-widget/jquery.magnific-popup.js'"/>
    <xsl:param name="file-bitsream" select="'http://localhost/~rnathanday/Developer/dryad-data-display-widget/examples/dryad.87ht85rs/widget_content.html'"/>
    <xsl:param name="wrapper-id"    select="'#dryad-ddw-frame1'"/>
    <xsl:param name="frame-height"  select="'550px'"/>
    <xsl:param name="frame-width"   select="'1000px'"/>
    
    <xsl:template match="/">
        <xsl:call-template name="js"/>
    </xsl:template>

    <xsl:template name="js">
        <![CDATA[
(function(w, d) {
'use strict';
]]>
var ddwcss = '<xsl:value-of select="$ddwcss"/>'
, jqlib    = '<xsl:value-of select="$jqlib"/>'
, lblib    = '<xsl:value-of select="$lblib"/>'
, wid      = '<xsl:value-of select="$wrapper-id"/>'
, bssrc    = '<xsl:value-of select="$file-bitsream"/>
, height   = '<xsl:value-of select="$frame-height"/>'
, width    = '<xsl:value-of select="$frame-width"/>'
<![CDATA[
, minJQ = ['1.7.2',1,7,2] // jQuery 1.7.2+ required for lightbox library
, pudel = 150  // lightbox close delay, ms.
, pucls = 'mfp-zoom-in' // css class for lightbox
, frcls = 'dryad-ddw'
, jQuery;
if (wid === undefined || wid === '') return;
if (w.jQuery === undefined || !testJQversion(w.jQuery.fn.jquery)) {
    load_js(jblib, function(script) { 
        if (script.readyState) {        // IE
            script.readystatechange = function() {
                if (this.readyState === 'complete' || this.readyState == 'loaded') {
                    noConflictHandler();                
                }
            }
        } else {
            script.onload = noConflictHandler;
        }
    });
} else {
    jQuery = w.jQuery;
    dryadJQLoaded();
}
function testJQversion(jqv) {
    if (jqv === undefined) return false;
    var vs = jqv.match(/(\d+)\.(\d+)\.(\d+)/); // e.g., ["1.3.2", "1", "3", "2"]
    return    (parseInt(vs[1]) == minJQ[1] && parseInt(vs[2]) >= minJQ[2])  // jQuery 1.*
           || (parseInt(vs[1]) > minJQ[1])                                  // jQuery 2.*
}
function noConflictHandler() {
    jQuery = w.jQuery.noConflict();
    dryadJQLoaded();
}
function open_popup(content) {
    if (jQuery === undefined || !jQuery.hasOwnProperty('magnificPopup')) return;
    jQuery.magnificPopup.open({
        removalDelay: pudel,
        mainClass: pucls,
        items: {
            src: content,
            type: 'inline'
        }
    });
}
// download a URL using a hidden iframe element
function download_url(url) {
    var hiddenIFrameID = 'hiddenDownloader',
    iframe = document.getElementById(hiddenIFrameID);
    if (iframe === null) {
        iframe = document.createElement('iframe');
        iframe.id = hiddenIFrameID;
        iframe.style.display = 'none';
        document.body.appendChild(iframe);
    }
    iframe.src = url;
}
function handle_message(e) {
    if (e.origin !== d.location.protocol + '//' + d.location.host) return;
    if (e.data.hasOwnProperty('action')) {
        if (e.data.action === 'download') {
            if (!e.data.hasOwnProperty('data')) return;
            download_url(e.data.data);
        } else if (e.data.action === 'cite') {
            if (!e.data.hasOwnProperty('data')) return;
            open_popup(e.data.data);
        } else if (e.data.action === 'share') {
            if (!e.data.hasOwnProperty('data')) return;
            open_popup(e.data.data);      
        } else if (e.data.action === 'zoom') {
            if (!e.data.hasOwnProperty('data')) return;
            open_popup(e.data.data);
        }
    }
};
function load_js(url, cb) {
    var script = d.createElement('script');
    script.setAttribute('type', 'text/javascript');
    script.setAttribute('src', url);
    if (cb !== undefined) cb(script);
    (d.getElementsByTagName('script')[0]).insertBefore(script, null);
}
function load_css(url) {
    var link = d.createElement('link');
    link.setAttribute('rel', 'stylesheet');
    link.setAttribute('type', 'text/css');
    link.setAttribute('href', 'url');
    (d.getElementsByTagName('script')[0]).insertBefore(link, null);
}
function dryadJQLoaded() {
    jQuery(d).ready(function($) {
        var wrapper = document.getElementById(wid);
        if (wrapper === null) return;
        var frame = document.createElement('iframe');
        frame.setAttribute('class', frcls);
        frame.setAttribute('src', bssrc);
        frame.setAttribute('width', width);
        frame.setAttribute('height', height);
        w.addEventListener('message', handle_message, false);
        load_css(ddwcss);
        load_js(lblib);
        wrapper.appendChild(frame);
    });
}
})(window,document);
]]>
    </xsl:template>
    
    
</xsl:stylesheet>