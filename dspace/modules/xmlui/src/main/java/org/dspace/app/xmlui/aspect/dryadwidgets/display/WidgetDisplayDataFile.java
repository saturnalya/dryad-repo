package org.dspace.app.xmlui.aspect.dryadwidgets.display;

import java.util.Map;
import java.sql.SQLException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.Action;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.xmlui.aspect.dryadwidgets.WidgetBannerLookup;


/**
 * Class for returning an HTML view of a data file object.
 *
 * @author Nathan Day
 */
public class WidgetDisplayDataFile extends WidgetBannerLookup implements Action {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WidgetDisplayDataFile.class);
    
    /**
     * Determine if the provided identifier is resolvable
     */
    public boolean select(String expression, Map objectModel,
            Parameters parameters) {
            String referrer = parameters.getParameter("referrer","");
            String pubId = expression;
            String packageDOI = null;
        try {
            packageDOI = lookup(pubId, referrer, objectModel);
        } catch (SQLException ex) {
            log.error("Error looking up article identifier:", ex);
        }
        return packageDOI != null;
    }

    @Override
    public Map act(Redirector rdrctr, SourceResolver sr, Map map, String string, Parameters prmtrs) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
}
