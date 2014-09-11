/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

/**
 *
 * @author Nathan Day
 */
public abstract class BaseBitstreamHandler {
    
    /*
        A bitstream handler generates an XML with this structure:

        <document>
            <type></type>
            <data></data>
        </document>
    
        The generated XML document will be handled by an XSLT transformer,
        so it must be well-formed. Lexically risky content shuld be 
        escaped in a <!CDATA[]]> declaration.
    */
    private final String NSURI = "http://datadryad.org/api/v1/widgets/display";
    private final String NSNAME = "ddw";
    private final String docEltName = "document";
    private final String typeEltName = "type";
    private final String dataEltName = "data";
    
    protected BufferedReader bufferedReader;
    protected ContentHandler contentHandler;
    protected LexicalHandler lexicalHandler;
    public BaseBitstreamHandler(BufferedReader bufferedReader, ContentHandler contentHandler, LexicalHandler lexicalHandler, String format) throws SAXException {
        this.bufferedReader = bufferedReader;
        this.contentHandler = contentHandler;
        this.lexicalHandler = lexicalHandler;
        contentHandler.startPrefixMapping(NSNAME, NSURI);
        contentHandler.startDocument();
        contentHandler.startElement(NSURI, docEltName, NSNAME + ":" + docEltName, null);
        contentHandler.startElement(NSURI, typeEltName, NSNAME + ":" + typeEltName, null);
        contentHandler.characters(format.toCharArray(), 0, format.length());
        contentHandler.endElement(NSURI, typeEltName, NSNAME + ":" + typeEltName);
        contentHandler.startElement(NSURI, dataEltName, NSNAME + ":" + dataEltName, null);
    }
    public void finalize() throws SAXException {
        contentHandler.endElement(NSURI, dataEltName, NSNAME + ":" + dataEltName);
        contentHandler.endElement(NSURI, docEltName, NSNAME + ":" + docEltName);
        contentHandler.endDocument();
    }
    public abstract void generate() throws SAXException, IOException;
}
