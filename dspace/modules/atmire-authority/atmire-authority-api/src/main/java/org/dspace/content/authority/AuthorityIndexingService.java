package org.dspace.content.authority;

import org.dspace.core.Context;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Indexing API for Solr based Authority Control Service.
 *
 * @author Lantian Gai, Mark Diggory, Kevin Van de Velde
 */
public interface AuthorityIndexingService {


    public void indexContent(AuthorityValue value, boolean force);

    public void unIndexContent(Context context, String handle, boolean commit) throws SQLException, IOException;

    public void cleanIndex() throws Exception;

    public void commit();

}
