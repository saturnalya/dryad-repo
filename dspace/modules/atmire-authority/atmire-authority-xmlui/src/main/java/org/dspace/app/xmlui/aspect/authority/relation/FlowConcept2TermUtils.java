/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authority.relation;

import java.sql.SQLException;
import java.util.Map;

import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.administrative.FlowResult;
import org.dspace.app.xmlui.aspect.authority.AuthorityUtils;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Concept2Term;
import org.dspace.core.Context;


/**
 * Utility methods to processes actions on EPeople. These methods are used
 * exclusively from the administrative flow scripts.
 *
 * @author Scott Phillips
 */
public class FlowConcept2TermUtils {

    /** log4j category */
    private static final Logger log = Logger.getLogger(FlowConcept2TermUtils.class);

    /** Language Strings */
    private static final Message T_add_Concept2Term_success_notice =
            new Message("default","xmlui.administrative.FlowConcept2TermUtils.add_Concept2Term_success_notice");

    private static final Message T_edit_Concept2Term_success_notice =
            new Message("default","xmlui.administrative.FlowConcept2TermUtils.edit_Concept2Term_success_notice");

    private static final Message T_reset_password_success_notice =
            new Message("default","xmlui.administrative.FlowConcept2TermUtils.reset_password_success_notice");

    private static final Message t_delete_Concept2Term_success_notice =
            new Message("default","xmlui.administrative.FlowConcept2TermUtils.delete_Concept2Term_success_notice");

    private static final Message t_delete_Concept2Term_failed_notice =
            new Message("default","xmlui.administrative.FlowConcept2TermUtils.delete_Concept2Term_failed_notice");

    /**
     * Add a new Concept2Term. This method will check that the email address,
     * first name, and last name are non empty. Also a check is performed
     * to see if the requested email address is already in use by another
     * user.
     *
     * @param context The current DSpace context
     * @param request The HTTP request parameters
     * @param objectModel Cocoon's object model
     * @return A process result's object.
     */
    public static FlowResult processAddConcept2Term(Context context, Request request, Map objectModel) throws SQLException, AuthorizeException
    {
        FlowResult result = new FlowResult();
        result.setContinue(false); // default to no continue

        // Get all our request parameters
        String conceptID = request.getParameter("concept_id");
        String termID = request.getParameter("term_id");
        String roleID = request.getParameter("role_id");
        // If we have errors, the form needs to be resubmitted to fix those problems
        if (StringUtils.isEmpty(conceptID)||StringUtils.isEmpty(termID)||StringUtils.isEmpty(roleID)|| Concept2Term.findByConceptAndTerm(context, Integer.parseInt(conceptID), Integer.parseInt(termID))!=null)
        {
            result.addError("literalForm");
        }
        Integer concept_id = Integer.parseInt(conceptID);

        Integer term_id = Integer.parseInt(termID);
        Integer role_id = Integer.parseInt(roleID);
        // No errors, so we try to create the Concept2Term from the data provided
        if (result.getErrors() == null)
        {
            Concept2Term newConcept2Term = AuthorityUtils.createNewConcept2Term(objectModel,role_id,concept_id,term_id);
            context.commit();
            // success
            result.setContinue(true);
            result.setOutcome(true);
            result.setMessage(T_add_Concept2Term_success_notice);
            result.setParameter("Concept2TermID", newConcept2Term.getRelationID());
        }

        return result;
    }

}
