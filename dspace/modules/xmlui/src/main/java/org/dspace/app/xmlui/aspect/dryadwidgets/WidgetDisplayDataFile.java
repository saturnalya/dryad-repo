package org.dspace.app.xmlui.aspect.dryadwidgets;

import org.dspace.dataonemn.DataOneMN;

import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.core.ConfigurationManager;

/**
 * Class for returning an HTML view of a data file object.
 *
 * @author Nathan Day
 */
public class WidgetDisplayDataFile extends AbstractLogEnabled {
    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WidgetDisplayDataFile.class);
    private final static String DOI_PREFIX = "doi:";
    
    public String lookup(String pubId, String referrer, Map objectModel) throws SQLException {

            if(referrer == null || referrer.length() == 0) {
                return null;
            }
            if(pubId == null || pubId.length() == 0) {
                return null;
            }

            // Normalize the incoming pubId to what we index in solr
            String solrPubId = null;
            if(!pubId.toLowerCase().startsWith(DOI_PREFIX)) {
                solrPubId = DOI_PREFIX + pubId.substring(DOI_PREFIX.length());
            } else {
                solrPubId = pubId;
            }
            
            // Incoming pubId should identify a publication/article.  See if we have
            // a data package that references this article
            try {
                CommonsHttpSolrServer solrServer;
                String solrService = ConfigurationManager.getProperty("solr.search.server");
                solrServer = new CommonsHttpSolrServer(solrService);
                solrServer.setBaseURL(solrService);

                // Look it up in Solr
                SolrQuery query = new SolrQuery();
                query.setQuery("dc.relation.isreferencedby:\"" + solrPubId + "\" AND DSpaceStatus:Archived AND location.coll:2");

                QueryResponse response = solrServer.query(query);
                SolrDocumentList documentList = response.getResults();
                if(documentList.isEmpty()) {
                    return null;
                }

                SolrDocument document = documentList.get(0);
                String firstDOI = (String)document.getFirstValue("dc.identifier");
                if (firstDOI == null) {
                    log.debug("Found no package associate with doi: " + solrPubId);
                    return null;
                }
                
        } catch (MalformedURLException ex) {
            log.error("Malformed URL Exception when instantiating solr server", ex);
            return null;
        } catch (SolrServerException ex) {
            log.error("Error querying SOLR", ex);
            return null;
        }
    }

}
