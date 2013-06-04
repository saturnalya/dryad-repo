/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.dspace.handle.HandleManager;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Bundle;
import org.dspace.content.Bitstream;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.identifier.IdentifierService;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.utils.DSpace;

import org.apache.log4j.Logger;

/**
 * DataFileStats retrieves detailed statistics about a data file.
 *
 * The task succeeds if it was able to locate all required stats, otherwise it fails.
 * Originally based on the RequiredMetadata task by Richard Rodgers.
 *
 * Input: a single data file OR a collection that contains data files
 * Output: CSV file with appropriate stats 
 * @author Ryan Scherle
 */
@Suspendable
public class DataFileStats extends AbstractCurationTask {

    private static Logger log = Logger.getLogger(DataFileStats.class);
    private IdentifierService identifierService = null;
    DocumentBuilderFactory dbf = null;
    DocumentBuilder docb = null;
    static long total = 0;
    private Context context;
    private static List<String> journalsThatAllowReview = new ArrayList<String>();
    private static List<String> integratedJournals = new ArrayList<String>();
    private static List<String> integratedJournalsThatAllowEmbargo = new ArrayList<String>();
    
    @Override 
    public void init(Curator curator, String taskId) throws IOException {
        super.init(curator, taskId);
	
        identifierService = new DSpace().getSingletonService(IdentifierService.class);            
	
	// init xml processing
	try {
	    dbf = DocumentBuilderFactory.newInstance();
	    docb = dbf.newDocumentBuilder();
	} catch (ParserConfigurationException e) {
	    throw new IOException("unable to initiate xml processor", e);
	}

	// init list of journals that support embargo and review
        String journalPropFile = ConfigurationManager.getProperty("submit.journal.config");
	log.info("initializing journal settings from property file " + journalPropFile);
        Properties properties = new Properties();
	try {
	    properties.load(new FileInputStream(journalPropFile));
	    String journalTypes = properties.getProperty("journal.order");
	    for (int i = 0; i < journalTypes.split(",").length; i++) {
		String journalType = journalTypes.split(",")[i].trim();
		String journalDisplay = properties.getProperty("journal." + journalType + ".fullname");
		String integrated = properties.getProperty("journal." + journalType + ".integrated");
		String embargo = properties.getProperty("journal." + journalType + ".embargoAllowed", "true");
		String allowReviewWorkflow = properties.getProperty("journal." + journalType + ".allowReviewWorkflow", "false");

		if(integrated != null && Boolean.valueOf(integrated)) {
		    integratedJournals.add(journalDisplay);
		}
		if(allowReviewWorkflow != null && Boolean.valueOf(allowReviewWorkflow)) {
		    journalsThatAllowReview.add(journalDisplay);
		}
		if(embargo != null && Boolean.valueOf(embargo)) {
		    integratedJournalsThatAllowEmbargo.add(journalDisplay);
		}
	    }
	} catch(Exception e) {
	    log.error("Unable to initialize the journal settings");
	}
    }
    
    /**
     * Perform the curation task upon passed DSO
     *
     * @param dso the DSpace object
     * @throws IOException
     */
    @Override
    public int perform(DSpaceObject dso) throws IOException {
	log.info("performing DataPackageStats task " + total++ );
	
	String handle = "\"[no handle found]\"";
	String fileDOI = "\"[no file DOI found]\"";
	String journal = "[no journal found]"; // don't add quotes here, because journal is always quoted when output below
	boolean journalAllowsEmbargo = false;
	boolean journalAllowsReview = false;
	long fileSize = 0;
	String embargoType = "none";
	String embargoDate = "";
	int downloads = 0;
	int views = 0;
	String dateAccessioned = "\"[unknown]\"";

	
	try {
	    context = new Context();
        } catch (SQLException e) {
	    log.fatal("Unable to open database connection", e);
	    return Curator.CURATE_FAIL;
	}
	
	if (dso.getType() == Constants.COLLECTION) {
	    // output headers for the CSV file that will be created by processing all items in this collection
	    report("handle, fileDOI, journal, journalAllowsEmbargo, journalAllowsReview, fileSize, " +
		   "embargoType, embargoDate, downloads, views, dateAccessioned");
	} else if (dso.getType() == Constants.ITEM) {
            Item item = (Item)dso;

	    try {
		handle = item.getHandle();
		log.info("handle = " + handle);
		
		if (handle == null) {
		    // this item is still in workflow - no handle assigned
		    handle = "in workflow";
		}
		
		// file DOI
		DCValue[] vals = item.getMetadata("dc.identifier");
		if (vals.length == 0) {
		    setResult("Object has no dc.identifier available " + handle);
		    log.error("Skipping -- no dc.identifier available for " + handle);
		    context.abort(); 
		    return Curator.CURATE_SKIP;
		} else {
		    for(int i = 0; i < vals.length; i++) {
			if (vals[i].value.startsWith("doi:")) {
			    fileDOI = vals[i].value;
			}
		    }
		}
		log.debug("fileDOI = " + fileDOI);

		// get the associated data package so we can pull in relevant package-level information
		Item dataPackage = getContainingDataPackage(item);
		
		// journal
	 	vals = dataPackage.getMetadata("prism.publicationName");
		if (vals.length == 0) {
		    setResult("Object has no prism.publicationName available " + dataPackage.getHandle());
		    log.error("Skipping -- Object has no prism.publicationName available " + dataPackage.getHandle());
		    context.abort();
		    return Curator.CURATE_SKIP;
		} else {
		    journal = vals[0].value;
		}
		log.debug("journal = " + journal);

		// journalAllowsEmbargo
		// embargoes are allowed for all non-integrated journals
		// embargoes are also allowed for integrated journals that have set the embargoesAllowed option
		if(!integratedJournals.contains(journal) || integratedJournalsThatAllowEmbargo.contains(journal)) {
		    journalAllowsEmbargo = true;
		} 

		// journalAllowsReview
		if(journalsThatAllowReview.contains(journal)) {
		    journalAllowsReview = true;
		}
				
		// accession date
		vals = item.getMetadata("dc.date.accessioned");
		if (vals.length == 0) {
		    setResult("Object has no dc.date.accessioned available " + handle);
		    log.error("Skipping -- Object has no dc.date.accessioned available " + handle);
		    context.abort();
		    return Curator.CURATE_SKIP;
		} else {
		    dateAccessioned = vals[0].value;
		}
		log.debug("dateAccessioned = " + dateAccessioned);

		
		// total file size
		// add total size of the bitstreams in this data file 
		// (includes metadata, readme, and textual conversions for indexing)
		for (Bundle bn : item.getBundles()) {
		    for (Bitstream bs : bn.getBitstreams()) {
			fileSize = fileSize + bs.getSize();
		    }
		}
		log.debug("total file size = " + fileSize);

		// embargo setting 
		vals = item.getMetadata("dc.type.embargo");
		if (vals.length > 0) {
		    embargoType = vals[0].value;
		    log.debug("EMBARGO vals " + vals.length + " type " + embargoType);
		}
		vals = item.getMetadata("dc.date.embargoedUntil");
		if (vals.length > 0) {
		    embargoDate = vals[0].value;
		}
		if((embargoType == null || embargoType.equals("") || embargoType.equals("none")) &&
		   (embargoDate != null && !embargoDate.equals(""))) {
		    // correctly encode embago type to "oneyear" if there is a date set, but the type is blank or none
		    embargoType = "oneyear";
		}
		log.debug("embargoType = " + embargoType);
		log.debug("embargoDate = " + embargoDate);
				       			    			
		// number of downlaods 
		// must use the DSpace item ID, since the solr stats system is based on this ID
		// The SOLR address is hardcoded to the production system here, because even when we run on test servers,
		// it's easiest to use the real stats --the test servers typically don't have useful stats available
		URL downloadStatURL = new URL("http://datadryad.org/solr/statistics/select/?indent=on&q=owningItem:" + item.getID());
		log.debug("fetching " + downloadStatURL);
		Document statsdoc = docb.parse(downloadStatURL.openStream());
		NodeList nl = statsdoc.getElementsByTagName("result");
		String downloadsAtt = nl.item(0).getAttributes().getNamedItem("numFound").getTextContent();
		downloads = Integer.parseInt(downloadsAtt);
		log.debug("downloads = " + downloads);

		// number of views
		// must use the DSpace item ID, since the solr stats system is based on this ID
		// The SOLR address is hardcoded to the production system here, because even when we run on test servers,
		// it's easiest to use the real stats --the test servers typically don't have useful stats available
		URL viewStatURL = new URL("http://datadryad.org/solr/statistics/select/?fl=time&q=isBot:false+owningItem:" + item.getID());
		log.debug("fetching " + viewStatURL);
		statsdoc = docb.parse(viewStatURL.openStream());
		nl = statsdoc.getElementsByTagName("result");
		String viewsAtt = nl.item(0).getAttributes().getNamedItem("numFound").getTextContent();
		views = Integer.parseInt(viewsAtt);
		log.debug("views = " + views);

		
		log.info(handle + " done.");
	    } catch (Exception e) {
		log.fatal("Skipping -- Exception in processing " + handle, e);
		setResult("Object has a fatal error: " + handle + "\n" + e.getMessage());
		report("Object has a fatal error: " + handle + "\n" + e.getMessage());
		
		context.abort();
		return Curator.CURATE_SKIP;
	    }
	} else {
	    log.info("Skipping -- non-item DSpace object");
	    setResult("Object skipped (not an item)");
	    context.abort();
	    return Curator.CURATE_SKIP;
        }

	setResult("Last processed item = " + handle + " -- " + fileDOI);
	report(handle + ", " + fileDOI + ", " + journal + "\", " +
	       journalAllowsEmbargo + ", " + journalAllowsReview + ", " + fileSize + ", " +
	       embargoType + ", " + embargoDate + ", " + downloads + ", " + views + ", " +
	       dateAccessioned);

	// slow this down a bit so we don't overwhelm the production SOLR server with requests
	try {
	    Thread.sleep(20);
	} catch(InterruptedException e) {
	    // ignore it
	}

	log.debug("DataFileStats complete");

	try { 
	    context.complete();
        } catch (SQLException e) {
	    log.fatal("Unable to close database connection", e);
	}
	return Curator.CURATE_SUCCESS;
    }

    /**
       An XML utility method that returns the text content of a node.
    **/
    private String getNodeText(Node aNode) {
	return aNode.getChildNodes().item(0).getNodeValue();
    }

    private Item getDSpaceItem(String itemID) {
	Item dspaceItem = null;
	try {
	    dspaceItem = (Item)identifierService.resolve(context, itemID);  
        } catch (IdentifierNotFoundException e) {
	    log.fatal("Unable to get DSpace Item for " + itemID, e);
	} catch (IdentifierNotResolvableException e) {
	    log.fatal("Unable to get DSpace Item for " + itemID, e);
	}

	return dspaceItem;
    }

    private Item getContainingDataPackage(Item dataFile) {
	Item dataPackage = null;
	
	DCValue[] dataPackages = dataFile.getMetadata("dc.relation.ispartof");
	if (dataPackages.length == 0) {
	    setResult("Object has no dc.relation.ispartof available " + dataFile.getHandle());
	    log.error("Skipping -- Object has no dc.relation.ispartof available " + dataFile.getHandle());
	    context.abort();
	} else {
	    String dataPackageID = dataPackages[0].value;
	    dataPackage = getDSpaceItem(dataPackageID);
	}

	return dataPackage;
    }
    
}
