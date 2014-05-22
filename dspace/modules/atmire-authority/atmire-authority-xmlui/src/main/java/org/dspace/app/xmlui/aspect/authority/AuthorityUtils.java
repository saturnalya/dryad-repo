package org.dspace.app.xmlui.aspect.authority;

import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Concept;
import org.dspace.content.Concept2Term;
import org.dspace.content.Scheme;
import org.dspace.content.Term;
import org.dspace.core.Context;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.Map;

import java.util.Date;
/**
 * User: lantian @ atmire . com
 * Date: 3/12/14
 * Time: 3:31 PM
 */
public class AuthorityUtils {

    /** log4j category */
    private static final Logger log = Logger.getLogger(AuthorityUtils.class);

    public static Concept createNewConcept(Map objectModel,Boolean topConcept,String status,String language,String identifier) throws
            SQLException, AuthorizeException
    {
        final HttpServletRequest request = (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
        Context context = ContextUtil.obtainContext(objectModel);

        // Need to create new concept

        Concept concept = Concept.create(context);
        Date date = new Date();
        concept.setLastModified(date);
        concept.setCreated(date);
        concept.setLang(language);
        concept.setTopConcept(topConcept);
        concept.setStatus(status);
        concept.setIdentifier(identifier);
        concept.update();
        context.commit();
        // Give site auth a chance to set/override appropriate fields
        //AuthenticationManager.initEPerson(context, request, eperson);

        return concept;
    }


    public static Term createNewTerm(Map objectModel,String literalForm,String status,String source,String language,String identifier) throws
            SQLException, AuthorizeException
    {
        final HttpServletRequest request = (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
        Context context = ContextUtil.obtainContext(objectModel);

        // Need to create new concept

        Term term = Term.create(context);
        Date date = new Date();
        term.setCreated(date);
        term.setLastModified(date);
        term.setLiteralForm(literalForm);
        term.setLang(language);
        term.setSource(source);
        term.setStatus(status);
        term.setIdentifier(identifier);
        term.update();
        context.commit();

        return term;
    }


    public static Concept2Term createNewConcept2Term(Map objectModel,Integer role_id,Integer concept_id,Integer term_id) throws
            SQLException, AuthorizeException
    {
        final HttpServletRequest request = (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
        Context context = ContextUtil.obtainContext(objectModel);

        // Need to create new concept2Term

        Concept2Term concept2Term = Concept2Term.create(context);
        Date date = new Date();
        concept2Term.setRoleId(role_id);
        concept2Term.setConceptId(concept_id);
        concept2Term.setTermId(term_id);
        context.commit();

        return concept2Term;
    }

    public static Scheme createNewScheme(Map objectModel,String status,String language,String identifier) throws
            SQLException, AuthorizeException
    {
        final HttpServletRequest request = (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
        Context context = ContextUtil.obtainContext(objectModel);

        // Need to create new concept

        Scheme concept = Scheme.create(context);
        Date date = new Date();
        concept.setLastModified(date);
        concept.setCreated(date);
        concept.setLang(language);
        //concept.setTopConcept(topConcept);
        concept.setStatus(status);
        concept.setIdentifier(identifier);
        concept.update();
        context.commit();
        // Give site auth a chance to set/override appropriate fields
        //AuthenticationManager.initEPerson(context, request, eperson);

        return concept;
    }

}
