package org.dspace.content.authority;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Extensible base class to support a core Solr based Authority Control Service.
 *
 * @author Lantian Gai, Mark Diggory, Kevin Van de Velde
 */
public class SolrAuthorityServiceImpl implements AuthorityIndexingService, AuthoritySearchService {

    private static final Logger log = Logger.getLogger(SolrAuthorityServiceImpl.class);


    /**
     * Non-Static CommonsHttpSolrServer for processing indexing events.
     */
    private CommonsHttpSolrServer solr = null;


    /**
     * Non-Static Singelton instance of Configuration Service
     */
    private ConfigurationService configurationService;

    protected CommonsHttpSolrServer getSolr() throws MalformedURLException, SolrServerException {
        if (solr == null) {

            String solrService = ConfigurationManager.getProperty("solr.authority.server");

            log.debug("Solr authority URL: " + solrService);

            solr = new CommonsHttpSolrServer(solrService);
            solr.setBaseURL(solrService);

            SolrQuery solrQuery = new SolrQuery().setQuery("*:*");

            solr.query(solrQuery);
        }

        return solr;
    }

    public void indexContent(AuthorityValue value, boolean force) {
        SolrInputDocument doc = value.getSolrInputDocument();

        try{
            writeDocument(doc);
        }catch (Exception e){
            log.error("Error while writing authority value to the index: " + value.toString(), e);
        }
    }

    /**
     * Unindex a Document in the Lucene Index.
     * @param context the dspace context
     * @param identifier the identifier of the object to be deleted
     * @throws java.sql.SQLException
     * @throws IOException
     */
    public void unIndexContent(Context context, String identifier, boolean commit)
            throws SQLException, IOException {

        try {
            getSolr().deleteById(identifier);
            if(commit)
            {
                getSolr().commit();
            }
        } catch (SolrServerException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void cleanIndex() throws Exception {
        try{
            getSolr().deleteByQuery("*:*");
        } catch (Exception e){
            log.error("Error while cleaning authority solr server index", e);
            throw new Exception(e);
        }
    }

    public void commit() {
        try {
            getSolr().commit();
        } catch (SolrServerException e) {
            log.error("Error while committing authority solr server", e);
        } catch (IOException e) {
            log.error("Error while committing authority solr server", e);
        }
    }

    /**
     * Write the document to the solr index
     * @param doc the solr document
     * @throws java.io.IOException
     */
    private void writeDocument(SolrInputDocument doc) throws IOException {

        try {
            getSolr().add(doc);
        } catch (Exception e) {
            try {
                log.error("An error occurred for document: " + doc.getField("id").getFirstValue() + ", source: " + doc.getField("source").getFirstValue() + ", field: " + doc.getField("field").getFirstValue() + ", full-text: " + doc.getField("full-text").getFirstValue());
            } catch (Exception e1) {
                //shouldn't happen
            }
            log.error(e.getMessage(), e);
        }
    }

    public QueryResponse search(SolrQuery query) throws SolrServerException, MalformedURLException {
        return getSolr().query(query);
    }

    /**
     * Retrieves all the metadata fields which are indexed in the authority control
     * @return a list of metadata fields
     */
    public List<String> getAllIndexedMetadataFields() throws Exception {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("*:*");
        solrQuery.setFacet(true);
        solrQuery.addFacetField("field");

        QueryResponse response = getSolr().query(solrQuery);

        List<String> results = new ArrayList<String>();
        FacetField facetField = response.getFacetField("field");
        if(facetField != null){
            List<FacetField.Count> values = facetField.getValues();
            if(values != null){
                for (FacetField.Count facetValue : values) {
                    if (facetValue != null && facetValue.getName() != null) {
                        results.add(facetValue.getName());
                    }
                }
            }
        }
        return results;
    }
}
