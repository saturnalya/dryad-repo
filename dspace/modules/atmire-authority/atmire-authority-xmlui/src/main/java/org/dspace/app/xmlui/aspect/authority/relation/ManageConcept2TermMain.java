/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authority.relation;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.content.Concept2Term;

/**
 * The manage metadatarelation page is the starting point page for managing 
 * metadatarelation. From here the user is able to browse or search for metadatarelation, 
 * once identified the user can selected them for deletion by selecting 
 * the checkboxes and clicking delete or click their name to edit the 
 * metadatarelation.
 *
 * @author Alexey Maslov
 * @author Scott Phillips
 */
public class ManageConcept2TermMain extends AbstractDSpaceTransformer
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(ManageConcept2TermMain.class);

    /** Language Strings */
    private static final Message T_title =
            message("xmlui.aspect.authority.metadatarelation.ManageConcept2TermMain.title");

    private static final Message T_metadatarelation_trail =
            message("xmlui.aspect.authority.metadatarelation.general.metadatarelation_trail");

    private static final Message T_main_head =
            message("xmlui.aspect.authority.metadatarelation.ManageConcept2TermMain.main_head");

    private static final Message T_actions_head =
            message("xmlui.aspect.authority.metadatarelation.ManageConcept2TermMain.actions_head");

    private static final Message T_actions_create =
            message("xmlui.aspect.authority.metadatarelation.ManageConcept2TermMain.actions_create");

    private static final Message T_actions_create_link =
            message("xmlui.aspect.authority.metadatarelation.ManageConcept2TermMain.actions_create_link");

    private static final Message T_actions_browse =
            message("xmlui.aspect.authority.metadatarelation.ManageConcept2TermMain.actions_browse");

    private static final Message T_actions_browse_link =
            message("xmlui.aspect.authority.metadatarelation.ManageConcept2TermMain.actions_browse_link");

    private static final Message T_actions_search =
            message("xmlui.aspect.authority.metadatarelation.ManageConcept2TermMain.actions_search");

    private static final Message T_search_help =
            message("xmlui.aspect.authority.metadatarelation.ManageConcept2TermMain.search_help");

    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");

    private static final Message T_go =
            message("xmlui.general.go");

    private static final Message T_search_head =
            message("xmlui.aspect.authority.metadatarelation.ManageConcept2TermMain.search_head");

    private static final Message T_search_column1 =
            message("xmlui.aspect.authority.metadatarelation.ManageConcept2TermMain.search_column1");

    private static final Message T_search_column2 =
            message("xmlui.aspect.authority.metadatarelation.ManageConcept2TermMain.search_column2");

    private static final Message T_search_column3 =
            message("xmlui.aspect.authority.metadatarelation.ManageConcept2TermMain.search_column3");

    private static final Message T_search_column4 =
            message("xmlui.aspect.authority.metadatarelation.ManageConcept2TermMain.search_column4");

    private static final Message T_search_column5 =
            message("xmlui.aspect.authority.metadatarelation.ManageConcept2TermMain.search_column5");
    private static final Message T_submit_delete =
            message("xmlui.aspect.authority.metadatarelation.ManageConcept2TermMain.submit_delete");

    private static final Message T_no_results =
            message("xmlui.aspect.authority.metadatarelation.ManageConcept2TermMain.no_results");

    /**
     * The total number of entries to show on a page
     */
    private static final int PAGE_SIZE = 15;


    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(null,T_metadatarelation_trail);
    }


    public void addBody(Body body) throws WingException, SQLException
    {
        /* Get and setup our parameters */
        int page          = parameters.getParameterAsInteger("page",0);
        int highlightID   = parameters.getParameterAsInteger("highlightID",-1);
        String query      = decodeFromURL(parameters.getParameter("query",null));
        String baseURL    = contextPath+"/admin/metadatarelation?administrative-continue="+knot.getId();
        int resultCount   = Concept2Term.searchResultCount(context, query);
        Concept2Term[] metadatarelations = Concept2Term.search(context, query, page * PAGE_SIZE, PAGE_SIZE);


        // DIVISION: metadatarelation-main
        Division main = body.addInteractiveDivision("metadatarelation-main", contextPath
                + "/admin/metadatarelation", Division.METHOD_POST,
                "primary administrative metadatarelation");
        main.setHead(T_main_head);

        // DIVISION: metadatarelation-actions
        Division actions = main.addDivision("metadatarelation-actions");
        actions.setHead(T_actions_head);

        List actionsList = actions.addList("actions");
        actionsList.addLabel(T_actions_create);
        actionsList.addItemXref(baseURL+"&submit_add", T_actions_create_link);
        actionsList.addLabel(T_actions_browse);
        actionsList.addItemXref(baseURL+"&query&submit_search",
                T_actions_browse_link);

        actionsList.addLabel(T_actions_search);
        org.dspace.app.xmlui.wing.element.Item actionItem = actionsList.addItem();
        Text queryField = actionItem.addText("query");
        //queryField.setAutofocus("autofocus");
        if (query != null)
        {
            queryField.setValue(query);
        }
        queryField.setHelp(T_search_help);
        actionItem.addButton("submit_search").setValue(T_go);

        // DIVISION: metadatarelation-search
        Division search = main.addDivision("metadatarelation-search");
        search.setHead(T_search_head);

        // If there are more than 10 results the paginate the division.
        if (resultCount > PAGE_SIZE)
        {
            // If there are enough results then paginate the results
            int firstIndex = page*PAGE_SIZE+1;
            int lastIndex = page*PAGE_SIZE + metadatarelations.length;

            String nextURL = null, prevURL = null;
            if (page < (resultCount / PAGE_SIZE))
            {
                nextURL = baseURL + "&page=" + (page + 1);
            }
            if (page > 0)
            {
                prevURL = baseURL + "&page=" + (page - 1);
            }

            search.setSimplePagination(resultCount,firstIndex,lastIndex,prevURL, nextURL);
        }

        Table table = search.addTable("metadatarelation-search-table", metadatarelations.length + 1, 1);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_search_column1);
        header.addCell().addContent(T_search_column2);
        header.addCell().addContent(T_search_column3);
        header.addCell().addContent(T_search_column4);
        header.addCell().addContent(T_search_column5);
        CheckBox selectMTerm;
        for (Concept2Term metadatarelation : metadatarelations)
        {
            String metadatarelationID = String.valueOf(metadatarelation.getRelationID());
            String url = baseURL+"&submit_edit&metadatarelationID="+metadatarelationID;
            //java.util.List<String> deleteConstraints = metadatarelation.getDeleteConstraints();

            Row row;
            if (metadatarelation.getRelationID() == highlightID)
            {
                // This is a highlighted metadatarelation
                row = table.addRow(null, null, "highlight");
            }
            else
            {
                row = table.addRow();
            }

            selectMTerm = row.addCell().addCheckBox("select_metadatarelation");
            selectMTerm.setLabel(metadatarelationID);
            selectMTerm.addOption(metadatarelationID);
//            if (deleteConstraints != null && deleteConstraints.size() > 0)
//            {
//                selectEPerson.setDisabled();
//            }

            row.addCellContent(metadatarelationID);
            row.addCell().addXref(url, Integer.toString(metadatarelation.getRoleId()));
            row.addCell().addXref(url, Integer.toString(metadatarelation.getConceptId()));
            row.addCell().addXref(url, Integer.toString(metadatarelation.getTermId()));
        }

        if (metadatarelations.length <= 0)
        {
            Cell cell = table.addRow().addCell(1, 4);
            cell.addHighlight("italic").addContent(T_no_results);
        }
        else
        {
            search.addPara().addButton("submit_delete").setValue(T_submit_delete);
        }

        main.addHidden("administrative-continue").setValue(knot.getId());

    }
}
