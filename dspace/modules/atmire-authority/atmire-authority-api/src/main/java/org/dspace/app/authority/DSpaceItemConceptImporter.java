package org.dspace.app.authority;

import org.dspace.content.Item;
import org.dspace.core.Context;

import java.util.List;

/**
 * Utility to transfer Item object updates into Authority Concept Updates.
 *
 * @author Lantian Gai, Mark Diggory
 */
public class DSpaceItemConceptImporter {

    private List<ConceptMapper> mappers = null;

    public void importItem(Context context, Item item){
        if(mappers != null)
        {
            for (ConceptMapper mapper : mappers) {
                mapper.mapConcepts(context, item);
            }
        }
    }
}
