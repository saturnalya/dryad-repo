package org.dspace.content.authority;

import org.apache.log4j.Logger;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.kernel.ServiceManager;
import org.dspace.utils.DSpace;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 5/15/12
 * Time: 12:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class AuthorityItemConsumer implements Consumer {
    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(AuthorityItemConsumer.class);

    // collect Items, Collections, Communities that need indexing
    // collect Concepts that need indexing
    private Set<Concept> conceptsToUpdate = null;
    private Item item=null;

    // handles to delete since IDs are not useful by now.
    private Set<String> handlesToDelete = null;

    public void initialize() throws Exception {
        // No-op

    }
    DSpace dspace = new DSpace();
    AuthorityIndexingService indexer = dspace.getServiceManager().getServiceByName(AuthorityIndexingService.class.getName(),AuthorityIndexingService.class);

    public void finish(Context ctx) throws Exception {
        // No-op
    }


    public void consume(Context ctx, Event event) throws Exception {
        int st = event.getSubjectType();
        int et = event.getEventType();
        if (conceptsToUpdate == null) {
            conceptsToUpdate = new HashSet<Concept>();
        }
        try {
            ctx = new Context();
            ctx.turnOffAuthorisationSystem();
            switch (st) {
                case Constants.ITEM: {
                    if (et == Event.MODIFY_METADATA||et == Event.INSTALL) {
                        item = (Item) event.getSubject(ctx);
                        if(item.isArchived()){
                            addAuthority(ctx, item);
                        }
                    }
                    break;
                }
            }
        }
        catch (Exception e) {
            ctx.abort();
        }
        finally {
            ctx.commit();
        }

    }

    private void addAuthority(Context context,Item item){
        DCValue[] vals = item.getMetadata("prism.publicationName");
        if (vals.length > 0) {
            String journal = vals[0].value;
            //Remove asterisks from journal name
            if(journal!=null&&journal.endsWith("*"))
            {
                journal = journal.replace("*","");
            }
            try{
                Scheme scheme = Scheme.findByIdentifier(context,"prism_publicationName");
                if(scheme!=null&&item.isArchived())
                {

                    for(DCValue dcValue : vals)
                    {
                        Concept newConcepts[] = Concept.findByPreferredLabel(context,dcValue.value,scheme.getID());
                        if(newConcepts==null||newConcepts.length==0){
                            Concept newConcept = scheme.createConcept();
                            newConcept.setStatus(Concept.Status.ACCEPTED);
                            newConcept.update();
                            Term term = newConcept.createTerm(dcValue.value,1);
                            term.update();
                            if(!conceptsToUpdate.contains(newConcept)){
                                conceptsToUpdate.add(newConcept);
                            }

                        }
                        else
                        {
                            boolean addConcept = true;
                            Concept newConcept = newConcepts[0];
                            for(Concept concept : conceptsToUpdate)
                            {
                                if(concept.getID()==newConcept.getID())
                                {
                                     addConcept=false;
                                }
                            }
                            if(addConcept)
                            {
                                if(!newConcept.getStatus().equals(Concept.Status.ACCEPTED))
                                {
                                    //set it to accepted so we can index it
                                    newConcept.setStatus(Concept.Status.ACCEPTED);
                                    newConcept.update();
                                }

                                conceptsToUpdate.add(newConcept);
                            }
                        }
                    }

                }

            }catch (Exception e)
            {
                log.error(e.getMessage());
            }

        }

    }

    private static ServiceManager getServiceManager(){
        //Retrieve our service
        DSpace dspace = new DSpace();
        ServiceManager serviceManager = dspace.getServiceManager();
        return serviceManager;
    }


    public void end(Context ctx) throws Exception {
        if(conceptsToUpdate!=null&&conceptsToUpdate.size()>0){
            try{
            item.clearMetadata("prism","publicationName",null,Item.ANY);
            for(Concept concept : conceptsToUpdate)
            {
                //indexer.indexContent(ctx,concept, true);
                item.addMetadata("prism","publicationName",null,Item.ANY,concept.getLabel(),concept.getIdentifier(),-1);
            }
            //indexer.commit();
            item.update();

            ctx.getDBConnection().commit();
            // "free" the resources
            conceptsToUpdate = null;
            item = null;
            }catch (Exception e)
            {
                log.error(e.getMessage());
            }
        }
    }

}