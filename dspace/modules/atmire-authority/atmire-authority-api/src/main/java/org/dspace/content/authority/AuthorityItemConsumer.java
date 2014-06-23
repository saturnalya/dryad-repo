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
import java.util.*;

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
    private Set<Item> itemsToUpdate=null;

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

        if(itemsToUpdate==null)
        {
            itemsToUpdate = new HashSet<Item>();
        }
        try {

            switch (st) {
                case Constants.ITEM: {
                    if (et == Event.MODIFY_METADATA||et == Event.INSTALL) {
                        Item item = (Item) event.getSubject(ctx);
                        if(item.isArchived()){
                            itemsToUpdate.add(item);
                        }
                    }
                    break;
                }
            }
        }
        catch (Exception e) {
            ctx.abort();
        }

    }

    private void addAuthority(Item item){

        try{


        DCValue[] vals = item.getMetadata("prism.publicationName");

        if (vals.length > 0) {
            String journal = vals[0].value;
            //Remove asterisks from journal name
            if(journal!=null&&journal.endsWith("*"))
            {
                journal = journal.replace("*","");
            }


                Context context = new Context();
                context.turnOffAuthorisationSystem();

                Scheme scheme = Scheme.findByIdentifier(context,"prism.publicationName");
                if(scheme!=null&&item.isArchived())
                {
                    item.clearMetadata("prism","publicationName",null,Item.ANY);

                    for(DCValue dcValue : vals)
                    {
                        Concept newConcept = null;

                        if(dcValue.authority != null)
                        {
                            List<Concept> newConcepts = Concept.findByIdentifier(context, dcValue.authority);
                            if(newConcepts != null && newConcepts.size() > 0)
                                newConcept = newConcepts.get(0);

                        }

                        if(newConcept == null)
                        {
                            Concept newConcepts[] = Concept.findByPreferredLabel(context,dcValue.value,scheme.getID());
                            if(newConcepts!=null && newConcepts.length>0){
                                newConcept = newConcepts[0];
                            }
                        }

                        if(newConcept==null){

                            newConcept = scheme.createConcept();
                            newConcept.setStatus(Concept.Status.ACCEPTED);
                            newConcept.update();
                            Term term = newConcept.createTerm(dcValue.value,1);
                            term.update();
                            context.commit();

                        }


                        //indexer.indexContent(ctx,concept, true);
                        item.addMetadata("prism","publicationName",null,Item.ANY,newConcept.getLabel(),newConcept.getIdentifier(),Choices.CF_ACCEPTED);

                        //indexer.commit();
                        //item.update();
                    }
                    context.complete();

                }
            }

        }catch (Exception e)
        {
            log.error(e.getMessage());
        }

    }



    public void end(Context ctx) throws Exception {
        try{
            if(itemsToUpdate!=null&&itemsToUpdate.size()>0)
            {
                ctx.turnOffAuthorisationSystem();
                for(Item item : itemsToUpdate)
                {
                    addAuthority(item);
                    item.update();
                }

                ctx.getDBConnection().commit();
                ctx.restoreAuthSystemState();
            }


        }catch (Exception e)
        {
            log.error(e.getMessage());
        }
        itemsToUpdate = null;
    }

}