<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="1.0">

    <xsl:output method="html"/>
    
    <xsl:param name="widget-stylesheet"/>
    <xsl:param name="view-count"/>
    <xsl:param name="download-count"/>
    
    <xsl:param name="dryad-file-doi"/>
    
    <xsl:param name="publication-doi-doi"/>
    <xsl:param name="publication-doi-url"/>
    
    <xsl:param name="request-origin"/>
    
    <!-- was: http://datadryad.org/bitstream/handle/10255/dryad.35374/SuppTable10.csv?sequence=1 
        to be: cocoon:
    -->
    <xsl:param name="download-link"/>
    
    <xsl:variable name="banner-img-url"/>

    <xsl:template match="/">
        <html>
            <head>
                <xsl:call-template name="head-content"/>
            </head>
            <body id="dryad-ddw-body">
                <xsl:call-template name="body-content"/>
                <xsl:call-template name="make-meta"/>
                <xsl:call-template name="script-content"/>        
            </body>
        </html>
    </xsl:template>

    <xsl:template name="make-head">
        <link href="//maxcdn.bootstrapcdn.com/font-awesome/4.1.0/css/font-awesome.min.css" rel="stylesheet" type="text/css"></link>
        <link href="//fonts.googleapis.com/css?family=Source+Sans Pro:200,300italic,300,400italic,400,600italic,600,700italic,700,900italic,900" rel="stylesheet" type="text/css"></link>
        <link type="text/css" rel="stylesheet" href="{$widget-stylesheet}"></link>
    </xsl:template>

    <xsl:template name="body-content">
        <div class="dryad-ddw">
            <div class="dryad-ddw-header">
                <div class="dryad-ddw-banner">
                    <ul>
                        <li>
                            <a target="_blank" href="http://datadryad.org/widgets/dataPackageForPub?referrer=BMC&amp;pubId=doi%3A10.1038%2Fhdy.2011.108">
                                <img src="http://rnathanday.github.io/dryad-data-display-widget/script/bannerForPub.png" alt="Data in Dryad"></img>
                            </a>
                        </li>
                        <li><b>doi:10.1038/hdy.2011.108</b></li>
                    </ul>
                </div>
                <div class="dryad-ddw-title">
                    <h1>SuppTable10</h1>
                    <ul>
                        <li><b><xsl:value-of select="$view-count"/></b> views</li>
                        <li><b><xsl:value-of select="$download-count"/></b> downloads</li>
                    </ul>
                </div>
            </div>
            <div class="dryad-ddw-body">
                <div class="dryad-ddw-frame">
                    <iframe class="dryad-ddw-data">
                        <xsl:call-template name="data-content"/>
                    </iframe>
                </div>
                <div class="dryad-ddw-control">
                    <ul>
                        <li><a class="dryad-ddw-zoom" title="Zoom"><i class="fa fa-expand"></i></a></li>
                        <li><a class="dryad-ddw-share" title="Share"><i class="fa fa-share-alt"></i></a></li>
                        <li><a class="dryad-ddw-download" title="Download" href="{$download-link}"><i class="fa fa-download"></i></a></li>
                        <li><a class="dryad-ddw-cite" title="Cite"><i class="fa fa-quote-left"></i></a></li>
                    </ul>
                </div>
            </div>
            <div class="dryad-ddw-footer">
                <div class="dryad-ddw-control">
                    <ul>
                        <li><a class="dryad-ddw-zoom" title="Zoom"><i class="fa fa-expand"></i></a></li>
                        <li><a class="dryad-ddw-share" title="Share"><i class="fa fa-share-alt"></i></a></li>
                        <li><a class="dryad-ddw-download" title="Download" href="{$download-link}"><i class="fa fa-download"></i></a></li>
                        <li><a class="dryad-ddw-cite" title="Cite"><i class="fa fa-quote-left"></i></a></li>
                    </ul>
                </div>
                <p>Summary of the most likely informative SNPs from the Son’s exome data as judged by their observed frequency in HapMap.</p>
            </div>
        </div>
    </xsl:template>

    <xsl:template name="make-meta">
        <div id="dryad-ddw-meta" class="dryad-ddw-hide" style="display:none !important;">
            <div id="dryad-ddw-citation" class="dryad-popup dryad-ddw dryad-ddw-citation">
                <div class="dryad-ddw-citation">
                    <img src="http://rnathanday.github.io/dryad-data-display-widget/script/bannerForPub.png" alt="Data in Dryad"></img>
                    <p>When using this data, please cite the original publication:</p>
                    <p class="shade">
                        Bradshaw WE, Emerson KJ, Holzapfel CM (2012) Genetic correlations and the evolution of photoperiodic time measurement within a local population of the pitcher-plant mosquito, Wyeomyia smithii. Heredity 108: 473–479. <a href="http://dx.doi.org/10.1038/hdy.2011.108">http://dx.doi.org/10.1038/hdy.2011.108</a>
                    </p>
                    <p>Additionally, please cite the Dryad data package:</p>
                    <p class="shade">
                        Bradshaw WE, Emerson KJ, Holzapfel CM (2011) Data from: Genetic correlations and the evolution of photoperiodic time measurement within a local population of the pitcher-plant mosquito, Wyeomyia smithii. Dryad Digital Repository. <a href="http://dx.doi.org/10.5061/dryad.87ht85rs">http://dx.doi.org/10.5061/dryad.87ht85rs</a>                        
                    </p>
                    <p>Download the Dryad data package citation in the following formats:</p>
                    <ul class="dryad-ddw-citation">
                        <li><a href="http://datadryad.org/resource/doi:10.5061/dryad.87ht85rs/citation/ris">RIS</a> 
                            <span><i18n:text>xmlui.DryadItemSummary.risCompatible</i18n:text></span>
                        </li>
                        <li><a href="http://datadryad.org/resource/doi:10.5061/dryad.87ht85rs/citation/bib">BibTex</a> 
                            <span><i18n:text>xmlui.DryadItemSummary.bibtexCompatible</i18n:text></span>
                        </li>
                    </ul>
                </div>
            </div>
            <div id="dryad-ddw-share" class="dryad-popup dryad-ddw dryad-ddw-share">
                <div class="dryad-ddw-share">
                    <img src="{$banner-img-url}" alt="Data in Dryad"></img>
                    <ul class="dryad-ddw-share">
                        <li><xsl:call-template name="reddit-item"/></li>
                        <li><xsl:call-template name="twitter-item"/></li>
                        <li><xsl:call-template name="facebook-item"/></li>
                        <li><xsl:call-template name="mendeley-item"/></li>            
                    </ul>
                </div>
            </div>
        </div>
    </xsl:template>
    
    <xsl:template name="reddit-item">
        <a href="http://reddit.com/submit" onclick="window.open('http://reddit.com/submit?url='+encodeURIComponent('http://dx.doi.org/doi:10.5061/dryad.87ht85rs')+'&amp;title=Data+from%3A+Robustness+of+compound+Dirichlet+priors+for+Bayesian+inference+of+branch+lengths.+','reddit','toolbar=no,width=550,height=550'); return false"><img alt="Reddit" src="http://reddit.com/static/spreddit7.gif" border="0px;"></img></a>
    </xsl:template>
    
    <xsl:template name="twitter-item">
        <iframe id="twitter-widget-0" scrolling="no" frameborder="0" allowtransparency="true" src="http://platform.twitter.com/widgets/tweet_button.1406859257.html#_=1407626732783&amp;count=none&amp;id=twitter-widget-0&amp;lang=en&amp;original_referer=http%3A%2F%2Fdatadryad.org%2Fresource%2Fdoi%3A10.5061%2Fdryad.87ht85rs%2F1&amp;size=m&amp;text=USYB-2011-142.SupplData%20from%3A%20Robustness%20of%20compound%20Dirichlet%20priors%20for%20Bayesian%20inference%20of%20branch%20lengths.%20-%20Dryad&amp;url=http%3A%2F%2Fdx.doi.org%2Fdoi%3A10.5061%2Fdryad.87ht85rs&amp;via=datadryad" class="twitter-share-button twitter-tweet-button twitter-share-button twitter-count-none" title="Twitter Tweet Button" data-twttr-rendered="true"></iframe>        
    </xsl:template>
    
    <xsl:template name="facebook-item">
        <iframe src="http://www.facebook.com/plugins/like.php?href=http%3A%2F%2Fdx.doi.org%2Fdoi%3A10.5061%2Fdryad.87ht85rs&amp;layout=button_count&amp;show_faces=false&amp;width=100&amp;action=like&amp;colorscheme=light" scrolling="no" frameborder="0" style="border:none; overflow:hidden; width:80px;height:21px;" allowtransparency="true"></iframe>
    </xsl:template>
    
    <xsl:template name="mendeley-item">
        <a href="http://www.mendeley.com/import/?url=http://datadryad.org/resource/doi:10.5061/dryad.87ht85rs">
            <img alt="Mendeley" src="http://www.mendeley.com/graphics/mendeley.png" border="0px;"></img>
        </a>        
    </xsl:template>
    
    <xsl:template name="script-content">
        <script type="text/javascript"><![CDATA[
            (function() {
            'use strict';
            ]]>
            var origin = '<xsl:value-of select="$request-origin"/>'
            <![CDATA[
              , downloads = document.getElementsByClassName('dryad-ddw-download')
              , cites = document.getElementsByClassName('dryad-ddw-cite')
              , shares = document.getElementsByClassName('dryad-ddw-share')
              , zooms = document.getElementsByClassName('dryad-ddw-zoom')
              , elt, i;
            function set_onclick(elts, data) {
                for (i = 0; i < elts.length; i++) {
                    elts[i].onclick = function(evt) {
                        window.parent.postMessage(data, origin);
                        evt.preventDefault();
                    };
                }
            };
            set_onclick(cites,  {"action" : "cite",  "data" : document.getElementById("dryad-ddw-citation").cloneNode(true).outerHTML });
            set_onclick(shares, {"action" : "share", "data" : document.getElementById("dryad-ddw-share").cloneNode(true).outerHTML });
            var zoomc = document.getElementsByClassName("dryad-ddw")[0].cloneNode(true);
            var controls = zoomc.getElementsByClassName('dryad-ddw-control');
            for (i = 0; i < controls.length; i++) {
                controls[i].parentNode.removeChild(controls[i]);
            }
            zoomc.getElementsByClassName('dryad-ddw-frame')[0].classList.add('dryad-ddw-frame-full');
            set_onclick(zooms, {"action" : "zoom", "data" : zoomc.outerHTML} );
            })();
        ]]></script>
    </xsl:template>
    
    <xsl:template name="data-content">
        
    </xsl:template>

</xsl:stylesheet>
