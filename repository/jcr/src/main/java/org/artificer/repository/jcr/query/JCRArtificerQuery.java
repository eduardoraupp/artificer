/*
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artificer.repository.jcr.query;

import org.artificer.common.ArtificerException;
import org.artificer.common.query.xpath.ast.Query;
import org.artificer.common.query.xpath.visitors.XPathSerializationVisitor;
import org.artificer.repository.error.QueryExecutionException;
import org.artificer.repository.jcr.ClassificationHelper;
import org.artificer.repository.jcr.JCRConstants;
import org.artificer.repository.jcr.JCRPersistence;
import org.artificer.repository.jcr.i18n.Messages;
import org.artificer.repository.query.AbstractArtificerQueryImpl;
import org.artificer.repository.query.ArtifactSet;
import org.artificer.repository.PersistenceFactory;
import org.artificer.repository.jcr.JCRRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.jcr.query.QueryResult;
import java.util.HashMap;
import java.util.Map;

/**
 * A JCR implementation of an s-ramp query ({@link org.artificer.repository.query.ArtificerQuery}).
 *
 * @author eric.wittmann@redhat.com
 */
public class JCRArtificerQuery extends AbstractArtificerQueryImpl {

	private static Logger log = LoggerFactory.getLogger(JCRPersistence.class);

	private static Map<String, String> sOrderByMappings = new HashMap<String, String>();
	static {
		sOrderByMappings.put("createdBy", JCRConstants.JCR_CREATED_BY);
		sOrderByMappings.put("version", "version");
		sOrderByMappings.put("uuid", JCRConstants.JCR_UUID); 
		sOrderByMappings.put("createdTimestamp", JCRConstants.JCR_CREATED);
		sOrderByMappings.put("lastModifiedTimestamp", JCRConstants.JCR_LAST_MODIFIED);
		sOrderByMappings.put("lastModifiedBy", JCRConstants.JCR_LAST_MODIFIED_BY); 
		sOrderByMappings.put("name", JCRConstants.SRAMP_NAME);
	}

    private Session session;

	/**
	 * Constructor.
	 * @param xpathTemplate
	 * @param orderByProperty
	 * @param orderAscending
	 */
	public JCRArtificerQuery(String xpathTemplate, String orderByProperty, boolean orderAscending) {
		super(xpathTemplate, orderByProperty, orderAscending);
	}

	@Override
	protected ArtifactSet executeQuery(Query queryModel) throws ArtificerException {
		Session session = null;
        boolean logoutOnClose = true;
		try {
		    if (this.session != null) {
		        session = this.session;
		        logoutOnClose = false;
		    } else {
		        session = JCRRepositoryFactory.getSession();
                logoutOnClose = true;
		    }
			javax.jcr.query.QueryManager jcrQueryManager = session.getWorkspace().getQueryManager();
            String jcrOrderBy = null;
            if (getOrderByProperty() != null) {
                String jcrPropName = sOrderByMappings.get(getOrderByProperty());
                if (jcrPropName != null) {
                    jcrOrderBy = jcrPropName;
                }
            }
            ArtificerToJcrSql2QueryVisitor visitor = new ArtificerToJcrSql2QueryVisitor(session, (ClassificationHelper) PersistenceFactory.newInstance());
            queryModel.accept(visitor);
            if (jcrOrderBy != null) {
                visitor.setOrder(jcrOrderBy);
                visitor.setOrderAscending(isOrderAscending());
            }

            javax.jcr.query.Query jcrQuery = visitor.buildQuery();

            String jcrQueryString = jcrQuery.getStatement();
            if (log.isDebugEnabled()) {
				XPathSerializationVisitor xpathVisitor = new XPathSerializationVisitor();
				queryModel.accept(xpathVisitor);
				String originalQuery = xpathVisitor.getXPath();
				System.out.println(Messages.i18n.format("JCR_QUERY_FROM", jcrQueryString, originalQuery));
			}
			long startTime = System.currentTimeMillis();
			QueryResult jcrQueryResult = jcrQuery.execute();
			long endTime = System.currentTimeMillis();

			log.debug(Messages.i18n.format("QUERY_EXECUTED", jcrQueryString));
			log.debug(Messages.i18n.format("QUERY_EXECUTED_IN", endTime - startTime));

			return new JCRArtifactSet(session, jcrQueryResult.getNodes(), logoutOnClose);
		} catch (ArtificerException e) {
            // Only logout of the session on a throw.  Otherwise, the JCRArtifactSet will be
            // responsible for closing the session.
		    if (logoutOnClose)
		        JCRRepositoryFactory.logoutQuietly(session);
		    throw e;
		} catch (Throwable t) {
			// Only logout of the session on a throw.  Otherwise, the JCRArtifactSet will be
			// responsible for closing the session.
            if (logoutOnClose)
                JCRRepositoryFactory.logoutQuietly(session);
			throw new QueryExecutionException(t);
		}
	}

    /**
     * @param session the session to set
     */
    public void setSession(Session session) {
        this.session = session;
    }

}
