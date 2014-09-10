
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.app.xmlui.aspect.dryadwidgets.display;

import java.lang.reflect.Constructor;
import java.util.HashMap;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.io.IOException;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.generation.AbstractGenerator;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

import org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.BaseBitstreamHandler;
import org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.DefaultBitstreamHandler;
import org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.Text_Plain;

/**
 * This generator uses a DOI to locate a bitstream object and convert its content
 * to a type handlable by the Data Display Widget's data frame.
 * The publisher parameter must be provided, but is not currently recorded here.
 *
 * @author Nathan Day
 */
public class WidgetDisplayBitstreamGenerator extends AbstractGenerator {
    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WidgetDisplayBitstreamGenerator.class);

    private static final String DOFid = "DataONE-formatId";

    private static HashMap<String, Class> formatHandlers;
    static {
        formatHandlers = new HashMap<String, Class>();
        formatHandlers.put("text/plain", org.dspace.app.xmlui.aspect.dryadwidgets.display.bitstreamHandler.Text_Plain); 
    }
    
    @Override
    public void generate() throws IOException, SAXException, ProcessingException {
        String referrer     = parameters.getParameter("referrer","");
        String doi          = parameters.getParameter("doi","");
        String object_url   = parameters.getParameter("object-url",""); // value="http://localhost:{request:serverPort}/mn/object/{request-param:doi}/bitstream"
        if (doi == null || doi.equals("")) {
            throw new IllegalArgumentException("Empty or null doi provided to WidgetDisplayBitstreamGenerator");
        }

        URL objUrl = new URL(object_url);
        HttpURLConnection connection;
        String dataOneFormat;
        int responseCode;
        
        // Do an initial HTTP HEAD request from Data-One service to determine
        // - if requested resource exists
        // - if the requested resource has a handlable content type
        // If successful on both counts, GET the resource
        try {
            connection = (HttpURLConnection) objUrl.openConnection();
            connection.setRequestMethod("HEAD");
            responseCode = connection.getResponseCode();
            dataOneFormat = connection.getHeaderField(DOFid);
        } catch (Exception e) {
            throw new IOException("Failed to connect to DataOne service");
        }
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new ResourceNotFoundException("Resource not found for doi: " + doi);
        } else if (dataOneFormat.equals(null) || dataOneFormat.equals("")) {
            throw new IOException("Unavailable content type for doi: " + doi);
        }
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        // check for content-specific handler
        BaseBitstreamHandler handler = null;
        if (WidgetDisplayBitstreamGenerator.formatHandlers.containsKey(dataOneFormat)) {
            try {
                Constructor ctor = WidgetDisplayBitstreamGenerator.formatHandlers.get(dataOneFormat)
                                  .getConstructor(BufferedReader.class, ContentHandler.class, LexicalHandler.class, String.class);
                handler = (BaseBitstreamHandler) ctor.newInstance(bufferedReader, contentHandler, lexicalHandler, dataOneFormat);
            } catch (Exception e) {
                log.error("Failed to instantiate default bitstream handler for format '" + dataOneFormat + "':  " + e.toString());
                throw new ProcessingException("Bitstream handler error for doi: " + doi);            
            }
        }
        if (handler == null) {
            try {
                handler = new DefaultBitstreamHandler(bufferedReader, contentHandler, lexicalHandler, dataOneFormat);
            } catch (Exception e) {
                log.error("Failed to instantiate default bitstream handler: " + e.toString());
                throw new ProcessingException("Bitstream handler error for doi: " + doi);
            }
        }
        try {
            handler.generate();
            handler.finalize();
        } catch (Exception e) {
            log.error("Failed to generate bitstream content: " + e.toString());
            throw new ProcessingException("Bitstream generator error for doi: " + doi);

        // TODO: work out this cleanup scope
        } finally {
            connection.disconnect();
            objUrl = null;
        }
    }    
}
