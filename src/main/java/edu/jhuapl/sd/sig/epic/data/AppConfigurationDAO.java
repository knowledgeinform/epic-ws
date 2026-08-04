/*
 * COPYRIGHT NOTICE
 * (C) 2010 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.data;

import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.AppConfiguration;
import edu.jhuapl.sd.sig.epic.model.ConfigKey;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppConfigurationDAO
{

    private static final Long REFRESH_MILLIS = 10000L;

    private static Long LAST_REFRESH = 0L;

    private static Map<ConfigKey, String> configMap = new HashMap<ConfigKey, String>();

    public static synchronized String getConfigForKey(ConfigKey key)
    {

        long now = System.currentTimeMillis();
        if ((now - LAST_REFRESH) > REFRESH_MILLIS)
        {
            List<AppConfiguration> configs = null;
            EntityManager em = null;
            String query = "SELECT a FROM AppConfiguration a";

            try
            {
                em = JPAUtils.getEntityManager();
                TypedQuery<AppConfiguration> q = em.createQuery(query, AppConfiguration.class);
                configs = q.getResultList();
                for (AppConfiguration config : configs)
                {
                    configMap.put(config.getConfigKey(), config.getConfigValue());
                }
            }
            finally
            {
                JPAUtils.closeEntityManager(em);
                LAST_REFRESH = System.currentTimeMillis();
            }
        }

        return configMap.get(key);
    }
}
