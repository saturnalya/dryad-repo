package org.dspace.submit.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.workflow.DryadWorkflowUtils;

import java.sql.SQLException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 9/7/11
 * Time: 9:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class DryadJournalSubmissionUtils {
    private static Logger log = Logger.getLogger(DryadJournalSubmissionUtils.class);

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

    public static final String schema = "internal";
    public static final String element = "journal";
    public static final String scheme = "prism.publicationName";
    public static final String IDENTIFIER = "identifier";
    private static Map<String, DCValue> journalToMetadata = new HashMap<String, DCValue>();

    static {

    int counter = 1;
    String configLine = ConfigurationManager.getProperty("submit.journal.metadata." + counter);
    while(configLine != null){
        String journalField = configLine.split(":")[0];
        String metadataField = configLine.split(":")[1];
        DCValue dcVal = new DCValue();
        dcVal.schema = metadataField.split("\\.")[0];
        dcVal.element = metadataField.split("\\.")[1];
        if(metadataField.split("\\.").length == 3)
            dcVal.qualifier = metadataField.split("\\.")[2];

        //Add it our map
        journalToMetadata.put(journalField,dcVal);

        //Add one to our counter & read a new line
        counter++;
        configLine = ConfigurationManager.getProperty("submit.journal.metadata." + counter);
    }

    }

    public enum RecommendedBlackoutAction {
          BLACKOUT_TRUE
        , BLACKOUT_FALSE
        , JOURNAL_NOT_INTEGRATED
    }
    /*
    public static java.util.Map<String, Map<String, String>> getJournalProperties(Context c){
        java.util.Map<String, Map<String, String>> journalProperties = new HashMap<String, Map<String, String>>();
        try {
            Scheme journalScheme = Scheme.findByIdentifier(c,scheme);
            Concept[] concepts = journalScheme.getConcepts();
            for(Concept concept : concepts){
                String key = concept.getIdentifier();
                if(key!=null&&key.length()>0){
                    journalProperties.put(key, concept2JournalProperties(c,concept));
                }
            }

        }catch (Exception e) {
            log.error("Error while loading journal properties", e);
        }
        return journalProperties;
    }
    */


    public static Boolean shouldEnterBlackoutByDefault(Context context, Item item, Collection collection) throws SQLException {
        RecommendedBlackoutAction action = recommendedBlackoutAction(context, item, collection);
        return (action == RecommendedBlackoutAction.BLACKOUT_TRUE ||
                action == RecommendedBlackoutAction.JOURNAL_NOT_INTEGRATED);
    }

    public static RecommendedBlackoutAction recommendedBlackoutAction(Context context, Item item, Collection collection) throws SQLException {
        // get Journal
        Item dataPackage=item;
        if(!isDataPackage(collection)) 
            dataPackage = DryadWorkflowUtils.getDataPackage(context, item);
        DCValue[] journalFullNames = dataPackage.getMetadata(scheme);
        String journalFullName=null;
        String journalIdentifier=null;
        if(journalFullNames!=null && journalFullNames.length > 0){
            journalFullName=journalFullNames[0].value;
            journalIdentifier = journalFullNames[0].authority;
        }
        if(journalIdentifier==null || journalIdentifier.length()==0){
            Concept concept = findKeyByFullname(context,journalFullName);
            journalIdentifier = concept.getIdentifier();
        }
        // Ignore blackout setting if journal is not (yet) integrated
	// get journal's blackout setting
	// journal is blacked out if its blackout setting is true or if it has no setting
        boolean isIntegrated = true;
        boolean isBlackedOut = false;
        if(journalIdentifier!=null) {
            isIntegrated = isIntegrated(context,journalIdentifier);
            isBlackedOut = isBlackedOut(context,journalIdentifier);
        }

        if(!isIntegrated) {
            // journal is not integrated.  Enter blackout by default
            return RecommendedBlackoutAction.JOURNAL_NOT_INTEGRATED;
        } else if(isBlackedOut) {
            // journal has a blackout setting and it's set to true
            return RecommendedBlackoutAction.BLACKOUT_TRUE;
        } else {
            // journal is integrated but blackout setting is false or missing
           return RecommendedBlackoutAction.BLACKOUT_FALSE;
        }
    }


     private static boolean isDataPackage(Collection coll) throws SQLException {
        return coll.getHandle().equals(ConfigurationManager.getProperty("submit.publications.collection"));
    }


    /**
     * Replaces invalid filename characters by percent-escaping.  Based on
     * http://stackoverflow.com/questions/1184176/how-can-i-safely-encode-a-string-in-java-to-use-as-a-filename
     * 
     * @param filename A filename to escape
     * @return The filename, with special characters escaped with percent
     */
    public static String escapeFilename(String filename) {
        final char fileSep = System.getProperty("file.separator").charAt(0); // e.g. '/'
        final char escape = '%';
        int len = filename.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char ch = filename.charAt(i);
            if (ch < ' ' || ch >= 0x7F || ch == fileSep
                || (ch == '.' && i == 0) // we don't want to collide with "." or ".."!
                || ch == escape) {
                sb.append(escape);
                if (ch < 0x10) {
                    sb.append('0'); // Leading zero
                }
                sb.append(Integer.toHexString(ch));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
    
    /**
     * Replaces escaped characters with their original representations
     * @param escaped a filename that has been escaped by
     * {@link #escapeFilename(java.lang.String) }
     * @return The original string, after unescaping
     */
    public static String unescapeFilename(String escaped) {
        StringBuilder sb = new StringBuilder();
        int i;
        while ((i = escaped.indexOf("%")) >= 0) {
            sb.append(escaped.substring(0, i));
            sb.append((char) Integer.parseInt(escaped.substring(i + 1, i + 3), 16));
            escaped = escaped.substring(i + 3);
        }
        sb.append(escaped);
        return sb.toString();
    }

    public static ArrayList<String> getJournalVals(Context context) {
        ArrayList<String> vals =new ArrayList<String>();
        try{
            Scheme journalScheme = Scheme.findByIdentifier(context,scheme);
            Concept[] concepts = journalScheme.getConcepts();
            for(Concept concept : concepts)
            {
               vals.add(concept.getIdentifier());
            }
        }
        catch (Exception e)
        {
            return null;
        }
        return vals;
    }



    public static boolean exists(Context context,String journalID) {
        try{
            ArrayList<Concept> concept = Concept.findByIdentifier(context,journalID);
            if(concept!=null&&concept.size()>0)
            {
                return true;
            }

        }catch (Exception e)
        {

        }
        return false;
    }

    public  static String getConceptMetadata(Context context,String journalID,String qualifier){

           Concept concept = getConcept(context, journalID);
        if(concept!=null)
        {

            AuthorityMetadataValue[] authorityMetadataValue =  concept.getMetadata(schema,element,qualifier,Item.ANY);
            if(authorityMetadataValue!=null&&authorityMetadataValue.length>0)
            {
                return authorityMetadataValue[0].value;
            }
        }
        return null;

    }

    public static String getJournalName(Context context,String journalID) {
        try{
            return getConcept(context,journalID).getPreferredLabel();
        }
        catch (Exception e)
        {
            return null;
        }

    }

    public static String getJournalDirectory(Context context,String journalID) {
        return getConceptMetadata(context,journalID,METADATADIR);
    }

    public static DCValue getJournalMetadata(Context context,String journalID) {
        return journalToMetadata.get(journalID);
    }

    public  static String[] getConceptMetadataArray(Context context,String journalID,String qualifier){
        String emails = getConceptMetadata(context,journalID,qualifier);
        if(emails!=null&&emails.length()>0)
        {
             return emails.split(",");
        }
        return null;
    }

    public static String[] getJournalNotifyWeekly(Context context,String journalID) {
        return getConceptMetadataArray(context,journalID,NOTIFYWEEKLY);
    }

    public static String[] getJournalNotifyOnReview(Context context,String journalID) {
        return getConceptMetadataArray(context,journalID,NOTIFY_ON_REVIEW);
    }

    public static String[] getJournalNotifyOnArchive(Context context,String journalID) {
        return getConceptMetadataArray(context,journalID,NOTIFY_ON_ARCHIVE);
    }

    public static boolean getJournalEmbargoAllowed(Context context,String journalID) {
        Concept concept =  getConcept(context, journalID);
        return  checkConceptMetadata(concept, EMBARGOALLOWED,false);
    }

    public static boolean allowReviewWorkflowJournals(Context context,String journalID) {
        Concept concept =  getConcept(context, journalID);
        return  checkConceptMetadata(concept, ALLOWREVIEWWORKFLOW,false);
    }

    public static boolean isBlackedOut(Context context, String journalID) {
        Concept concept =  getConcept(context, journalID);
        return  checkConceptMetadata(concept, PUBLICATION_BLACKOUT,true);
    }

    public static boolean subscriptionPaid(Context context,String journalID) {
        Concept concept =  getConcept(context, journalID);
        return  checkConceptMetadata(concept, SUBSCRIPTION_PAID,false);
    }

    public static boolean isIntegrated(Context context,String journalID) {

        Concept concept =  getConcept(context, journalID);
        return  checkConceptMetadata(concept, INTEGRATED,false);
    }

    private static boolean checkConceptMetadata(Concept concept, String qualifier, boolean dflt){
        boolean result=dflt;

        if(concept!=null){
            AuthorityMetadataValue[] values = concept.getMetadata(schema, element ,  qualifier, "*");
            if(values!=null)
                for(AuthorityMetadataValue val : values) {
                    if(dflt)
                    {
                        if(val.value.equals("false"))
                        {
                            result = false;
                        }
                    }
                    else
                    {
                        if(val.value.equals("true"))
                        {
                            result = true;
                        }
                    }
                }
        }
        return result;
    }


    public static Concept findKeyByFullname(Context context, String fullName) {
        try{
        Scheme journalScheme = Scheme.findByIdentifier(context,scheme);

        Concept[] concept = Concept.findByPreferredLabel(context,fullName,journalScheme.getID());
        if(concept!=null&&concept.length>0)
        {
           return concept[0];
        }
        }catch (Exception e)
        {}
        return null;
    }

    public static Concept getConcept(Context context, String identifier){
        try{
            ArrayList<Concept> concept = Concept.findByIdentifier(context,identifier);
            if(concept!=null&&concept.size()>0)
            {
                return concept.get(0);
            }
            else
            {
                return null;
            }

        }catch (Exception e)
        {
            return null;
        }
    }


}
