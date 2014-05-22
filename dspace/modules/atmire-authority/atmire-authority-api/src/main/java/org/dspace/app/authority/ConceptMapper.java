package org.dspace.app.authority;

import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.Concept;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Required;

import java.sql.SQLException;
import java.util.List;

/**
 * Extensible base class capable of transferring Item object updated into Authority Concept Updates.
 *
 * @author Lantian Gai, Mark Diggory
 */
public class ConceptMapper {

    private ConfigurationService cs = null;

    private List<ConceptMapper> mappers = null;

    @Required
    public void setCs(ConfigurationService cs) {
        this.cs = cs;
    }

    private String[] getAuthorityControlledFields() {
        return cs.getPropertyAsType("solr.authority.indexes", new String[0]);
    }

    /**
     * Map Concepts available in DSpace Item.
     * @param context
     * @param item
     */
    public void mapConcepts(Context context, Item item) {
        if (item != null && item.isArchived()) {
            for (String fieldName : getAuthorityControlledFields()) {
                for (DCValue value : item.getMetadata(fieldName)) {
                    try {
                        // TODO: How does this narrow by Scheme?
                        if(value.authority == null || Concept.findByIdentifier(context,value.authority) == null)
                        {

                            //1. try to find Concept in appropriate Scheme by metadata value.
                            //2.a. if candidate found, add its authority ID to the item.
                            //2.b. if candidate not found, create new concept.
                            //     Record the Authority ID in The Item
                            //     Commit all changes to reindex both Item and Concept.
                            //     Be sure to use a separate Context for these changes.
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }
        }
    }
}
