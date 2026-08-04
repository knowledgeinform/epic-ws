/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.startup;

import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import edu.jhuapl.sd.sig.epic.startup.AppConfiguration.AppConfigKey;

import javax.persistence.EntityManager;

/**
 * Class used for handling Hibernate search indexing at app initiation.
 * 
 * Indexed classes are: ProcesureDef, ProcedureDetails, Run
 */
public class IndexManager
{

    private static final Logger LOGGER = LogManager.getLogger();

    public static void indexDatabase() throws InterruptedException
    {
        if (indexingNeeded())
        {
            LOGGER.info("Indexing requested in the configuration file");
            EntityManager em = JPAUtils.getEntityManager();
            SearchSession searchSession = Search.session(em);
            MassIndexer indexer = searchSession.massIndexer();

            // Set the MassIndexer characteristics
            indexer.threadsToLoadObjects(Integer.parseInt(AppConfiguration.getConfigValue(AppConfigKey.THREADS_FOR_INDEXING).trim()));
            indexer.typesToIndexInParallel(Integer.parseInt(AppConfiguration.getConfigValue(AppConfigKey.SYNCHRONOUS_CLASSES_FOR_INDEXING).trim()));

            // Begin indexing and wait for completion
            indexer.startAndWait();

            // Perform post-indexing steps
            indexingComplete();
        }
        else
        {
            LOGGER.info("Indexing not requested... the configuration file " + AppConfigKey.PERFORM_INDEXING.toString() + " value was not set to 'TRUE'");
        }
    }

    // Determine if the indexing is desired
    private static boolean indexingNeeded()
    {
        // Check the configuration file to see if indexing was requested
        if (AppConfiguration.getConfigValue(AppConfigKey.PERFORM_INDEXING).trim().equalsIgnoreCase("TRUE"))
        {
            return true;
        }

        // Indexing not requested
        return false;
    }

    // Perform post-indexing steps
    private static void indexingComplete()
    {
        // Update the configuration file flag to indicate indexing is not needed
        AppConfiguration.UpdatePerormIndexingFlag();
    }

}
