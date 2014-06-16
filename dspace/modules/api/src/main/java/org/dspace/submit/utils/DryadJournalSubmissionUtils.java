package org.dspace.submit.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.authority.AuthorityValue;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.workflow.DryadWorkflowUtils;
import org.dspace.workflow.WorkflowItem;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
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

    public enum RecommendedBlackoutAction {
          BLACKOUT_TRUE
        , BLACKOUT_FALSE
        , JOURNAL_NOT_INTEGRATED
    }

    public static java.util.Map<String, Map<String, String>> getJournalProperties(){
        java.util.Map<String, Map<String, String>> journalProperties = new HashMap<String, Map<String, String>>();
        Context context = null;
        try {
            context = new Context();
            Concept[] concepts = Concept.findAll(context, AuthorityObject.ID);
            for(Concept concept : concepts){

                String journalType = concept.getIdentifier();
                String key = concept.getIdentifier();
                String fullName = concept.getLabel();

                log.debug("reading config for journal " + journalType);
                log.debug("fullname " + concept.getMetadata("internal","journal",FULLNAME,Item.ANY));
                log.debug("subscription? " + concept.getMetadata("internal", "journal", SUBSCRIPTION_PAID, Item.ANY));

                Map<String, String> map = new HashMap<String, String>();
                if(fullName!=null&&fullName.length()>0)
                {
                    key = fullName;
                    map.put(FULLNAME, key);
                }


                if(concept.getMetadata("internal","journal",METADATADIR,Item.ANY)!=null&&concept.getMetadata("internal","journal",METADATADIR,Item.ANY).length>0)
                map.put(METADATADIR, concept.getMetadata("internal","journal",METADATADIR,Item.ANY)[0].value);

                if(concept.getMetadata("internal","journal",INTEGRATED,Item.ANY)!=null&&concept.getMetadata("internal","journal",INTEGRATED,Item.ANY).length>0)
                map.put(INTEGRATED, concept.getMetadata("internal","journal",INTEGRATED,Item.ANY)[0].value);

                if(concept.getMetadata("internal","journal",PUBLICATION_BLACKOUT,Item.ANY)!=null&&concept.getMetadata("internal","journal",PUBLICATION_BLACKOUT,Item.ANY).length>0)
                map.put(PUBLICATION_BLACKOUT, concept.getMetadata("internal","journal",PUBLICATION_BLACKOUT,Item.ANY)[0].value);

                AuthorityMetadataValue[] emailOnReview = concept.getMetadata("internal","journal",NOTIFY_ON_REVIEW,Item.ANY);
                if(emailOnReview!=null&&emailOnReview.length>0)
                {
                    int i = 0;
                    String[] emails = new String[emailOnReview.length];
                    for (AuthorityMetadataValue authorityMetadataValue : emailOnReview)
                    {
                        emails[i]=(authorityMetadataValue.value);
                        i++;
                    }
                    map.put(NOTIFY_ON_REVIEW, StringUtils.join(emails,','));
                }

                AuthorityMetadataValue[] emailOnArchive = concept.getMetadata("internal","journal",NOTIFY_ON_ARCHIVE,Item.ANY);
                if(emailOnArchive!=null&&emailOnArchive.length>0)
                {
                    int i = 0;
                    String[] emails = new String[emailOnArchive.length];
                    for (AuthorityMetadataValue authorityMetadataValue : emailOnArchive)
                    {
                        emails[i]=(authorityMetadataValue.value);
                        i++;
                    }
                    map.put(NOTIFY_ON_ARCHIVE, StringUtils.join(emails,','));
                }

                map.put(JOURNAL_ID, journalType);

                if(concept.getMetadata("internal","journal",SUBSCRIPTION_PAID,Item.ANY)!=null&&concept.getMetadata("internal","journal",SUBSCRIPTION_PAID,Item.ANY).length>0)
                map.put(SUBSCRIPTION_PAID, concept.getMetadata("internal","journal",SUBSCRIPTION_PAID,Item.ANY)[0].value);

                if(key!=null&&key.length()>0){
                journalProperties.put(key, map);
                }

            }

        }catch (Exception e) {
            log.error("Error while loading journal properties", e);
        }
        finally {
            if(context!=null)
            {
                try{
                    context.complete();
                }catch (Exception e)
                {
                    context.abort();
                }
            }

        }
        return journalProperties;
    }

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
        DCValue[] journalFullNames = dataPackage.getMetadata("prism.publicationName");
        String journalFullName=null;
        if(journalFullNames!=null && journalFullNames.length > 0){
            journalFullName=journalFullNames[0].value;
        }

        Map<String, String> values = getJournalProperties().get(journalFullName);
        // Ignore blackout setting if journal is not (yet) integrated
	// get journal's blackout setting
	// journal is blacked out if its blackout setting is true or if it has no setting
        String isIntegrated = null;
        String isBlackedOut = null;
        if(values!=null && values.size()>0) {
            isIntegrated = values.get(INTEGRATED);
            isBlackedOut = values.get(PUBLICATION_BLACKOUT);
        }

        if(isIntegrated == null || isIntegrated.equals("false")) {
            // journal is not integrated.  Enter blackout by default
            return RecommendedBlackoutAction.JOURNAL_NOT_INTEGRATED;
        } else if(isBlackedOut==null || isBlackedOut.equals("true")) {
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


    public static String findKeyByFullname(String fullname){
        Map<String, String> props = getJournalProperties().get(fullname);
        if(props!=null)
            return props.get(DryadJournalSubmissionUtils.JOURNAL_ID);

        return null;
    }


    public static Map<String, String> getPropertiesByJournal(String key){
        return getJournalProperties().get(key);
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
    
}
