<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:ddw="http://datadryad.org/api/v1/widgets/display" 
    exclude-result-prefixes="ddw"
    version="1.0">
    
    <xsl:output method="html"/>
    
    <xsl:variable name="type" select="/ddw:document/ddw:type"/>
    <xsl:param name="doi"/>
    
    <xsl:template match="/">
        <html>
            <xsl:choose>
                <xsl:when test="$type = 'text/plain'">
                    <xsl:call-template name="text-plain"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="unsupported"/>
                </xsl:otherwise>
            </xsl:choose>
        </html>
    </xsl:template>

    <xsl:template match="ddw:document"/>
    <xsl:template match="ddw:type"/>
    
    <xsl:template name="unsupported">
        <head></head>
        <body>
            <div>This content type is unsupported. Please visit the
                <a href="http://dx.doi.org/{$doi}">Dryad site</a>
                to view the data.
            </div>
        </body>
    </xsl:template>
    
    <xsl:template name="text-plain">
        <head></head>
        <body>
<pre>
<xsl:value-of select="/ddw:document/ddw:data"/>
</pre>
        </body>
    </xsl:template>
    
</xsl:stylesheet>