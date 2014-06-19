package org.dspace.app.authority;

import org.dspace.content.authority.AuthoritySearchService;
import org.dspace.content.authority.AuthorityValue;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.content.AuthorityObject;
import org.dspace.content.Concept;
import org.dspace.content.Scheme;
import org.dspace.content.Term;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import org.apache.commons.cli.CommandLine;

import org.apache.commons.cli.Options;


import org.dspace.core.I18nUtil;
import org.dspace.utils.DSpace;

/**
 * Utility to transfer properties file into database tables.
 *
 * @author Lantian Gai, Mark Diggory
 */
public final class DryadJournalConceptImporter {


    /** DSpace Context object */
    private Context context;
    // Reading DryadJournalSubmission.properties
    public static final String FULLNAME = "fullname";
    public static final String METADATADIR = "metadataDir";
    public static final String INTEGRATED = "integrated";
    public static final String PUBLICATION_BLACKOUT = "publicationBlackout";
    public static final String NOTIFY_ON_REVIEW = "notifyOnReview";
    public static final String NOTIFY_ON_ARCHIVE = "notifyOnArchive";
    public static final String JOURNAL_ID = "journalID";
    public static final String SUBSCRIPTION_PAID = "subscriptionPaid";


    public static final String ALLOWREVIEWWORKFLOW = "allowReviewWorkflow";
    public static final String PARSINGSCHEME = "parsingScheme";
    public static final String EMBARGOALLOWED = "embargoAllowed";
    public static final String SPONSORNAME = "sponsorName";
    public static final String NOTIFYWEEKLY = "notifyWeekly";

    public static final String[] properties = new String[]{"fullname", "metadataDir","integrated", "publicationBlackout","notifyOnArchive", "journalID","subscriptionPaid","allowReviewWorkflow", "parsingScheme","embargoAllowed","sponsorName","notifyWeekly"};

    /**
     * For invoking via the command line.  If called with no command line arguments,
     * it will negotiate with the user for the administrator details
     *
     * @param argv
     *            command-line arguments
     */
    public static void main(String[] argv)
            throws Exception
    {
        CommandLineParser parser = new PosixParser();
        Options options = new Options();

        DryadJournalConceptImporter ca = new DryadJournalConceptImporter();
        options.addOption("t", "test", true, "test mode");

        CommandLine line = parser.parse(options, argv);
        ca.importAuthority(line.getOptionValue("t"));
    }



    /**
     * constructor, which just creates and object with a ready context
     *
     * @throws Exception
     */
    private DryadJournalConceptImporter()
            throws Exception
    {
        context = new Context();
    }

    /**
     * Create the administrator with the given details.  If the user
     * already exists then they are simply upped to administrator status
     *
     * @throws Exception
     */
    private void importAuthority(String test)
            throws Exception
    {
        // Of course we aren't an administrator yet so we need to
        // circumvent authorisation
        context.setIgnoreAuthorization(true);

        try {
            SolrQuery queryArgs = new SolrQuery();
            queryArgs.setQuery("*:*");
            queryArgs.setRows(-1);
            QueryResponse searchResponse = getSearchService().search(queryArgs);
            SolrDocumentList authDocs = searchResponse.getResults();
            int max = (int) searchResponse.getResults().getNumFound();

            queryArgs.setQuery("*:*");
            if(test!=null)
            {
                queryArgs.setRows(Integer.parseInt(test));
            }
            else
            {
                queryArgs.setRows(max);
            }

            searchResponse = getSearchService().search(queryArgs);
            authDocs = searchResponse.getResults();
            Date date = new Date();


            Scheme authScheme = Scheme.findByIdentifier(context, "prism.publicationName");
            if(authScheme==null){
                authScheme = Scheme.create(context,"prism.publicationName");
                authScheme.addMetadata("dc","title",null,"en","Dryad Journal Authority",null,1);
                authScheme.setLastModified(date);
                authScheme.setCreated(date);
                authScheme.setLang("en");
                authScheme.setStatus("Published");
                authScheme.update();
            }

            context.commit();
            //Get all journal configurations
            java.util.Map<String, Map<String, String>> journalProperties = getJournals();


            if(authDocs != null){
                int maxDocs = authDocs.size();

                //import all the authors
                for (int i = 0; i < maxDocs; i++) {
                    SolrDocument solrDocument = authDocs.get(i);
                    if(solrDocument != null){
                        AuthorityValue authorityValue = new AuthorityValue(solrDocument);
                        if(authorityValue.getId() != null){
                            ArrayList<Concept> aConcepts = Concept.findByIdentifier(context,authorityValue.getId());
                            if(aConcepts==null||aConcepts.size()==0)  {
                                Concept aConcept = authScheme.createConcept(authorityValue.getId());
                                aConcept.setStatus(Concept.Status.ACCEPTED);

                                if(solrDocument.getFieldValue("source")!=null) {
                                    String source = String.valueOf(solrDocument.getFieldValue("source"));
                                    aConcept.setSource(source);
                                    if(source.equals("LOCAL-DryadJournal"))
                                    {
                                        Map<String,String> val = journalProperties.get(authorityValue.getValue());
                                        if(val!=null){
                                            journalProperties.remove(authorityValue.getValue());

                                            for(String key : properties)
                                            {
                                                if(val.get(key)!=null&&val.get(key).length()>0)
                                                {
                                                    aConcept.addMetadata("internal","journal",key,"",val.get(key),authorityValue.getId(),0);
                                                }
                                            }

                                        }
                                    }
                                }
                                aConcept.update();
                                Term term = aConcept.createTerm(authorityValue.getFullText(),1);
                                term.update();
                                context.commit();
                            }
                        }

                    }
                }

            }

            Set<String> keys = journalProperties.keySet();
            for(String key : keys)
            {
                Map<String,String> val = journalProperties.get(key);

                // TODO: THIS SHOULD BE NARROWED BY SCHEME
                Concept[] aConcepts = Concept.findByPreferredLabel(context,val.get(FULLNAME),authScheme.getID());
                if(aConcepts==null||aConcepts.length==0)  {
                    String id = AuthorityObject.createIdentifier();

                    Concept aConcept = authScheme.createConcept(id);
                    aConcept.setSource("LOCAL-DryadJournal");
                    aConcept.setStatus(Concept.Status.ACCEPTED);

                    for(String property:properties)
                    {
                        if(val.get(key)!=null)
                        {
                            aConcept.addMetadata("internal","journal",property,"",val.get(property),id,0);
                        }
                    }

                    aConcept.update();
                    Term term = aConcept.createTerm(val.get(FULLNAME),1);
                    term.update();
                    context.commit();
                }
            }

        }catch (Exception e)
        {
            System.out.print(e);
            System.out.print(e.getStackTrace());
        }

        context.complete();

        System.out.println("Authority imported");
    }


    private HashMap<String, Map<String, String>> getJournals(){

        HashMap<String, Map<String, String>> journalProperties = new HashMap<String, Map<String, String>>();

        String journalPropFile = ConfigurationManager.getProperty("submit.journal.config");
        Properties journalProfiles = new Properties();
        try {
            journalProfiles.load(new InputStreamReader(new FileInputStream(journalPropFile), "UTF-8"));
            String journalTypes = journalProfiles.getProperty("journal.order");

            for (int i = 0; i < journalTypes.split(",").length; i++) {
                String journalType = journalTypes.split(",")[i].trim();

                String str = "journal." + journalType + ".";

                Map<String, String> map = new HashMap<String, String>();
                for(String property: properties)
                {
                    map.put(property, journalProfiles.getProperty(str + property));
                }


                String key = journalProfiles.getProperty(str + FULLNAME);
                if(key!=null&&key.length()>0){
                    journalProperties.put(key, map);
                }
            }

        }catch (IOException e) {
            //log.error("Error while loading journal properties", e);
        }
        return journalProperties;

    }

    private AuthoritySearchService getSearchService(){
        DSpace dspace = new DSpace();

        org.dspace.kernel.ServiceManager manager = dspace.getServiceManager() ;

        return manager.getServiceByName(AuthoritySearchService.class.getName(),AuthoritySearchService.class);
    }
}
