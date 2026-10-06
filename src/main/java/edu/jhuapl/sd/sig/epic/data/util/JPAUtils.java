/*
 * COPYRIGHT NOTICE
 * (C) 2010 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.data.util;

import com.mchange.v2.c3p0.C3P0Registry;
import com.mchange.v2.c3p0.PooledDataSource;

import edu.jhuapl.sd.sig.epic.model.VersionInfo;
import edu.jhuapl.sd.sig.epic.startup.AppConfiguration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.search.util.common.SearchException;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.repository.sql.DialectName;
import org.javers.repository.sql.JaversSqlRepository;
import org.javers.repository.sql.SqlRepositoryBuilder;
import org.javers.repository.sql.ConnectionProvider;
import java.sql.Connection;
import java.sql.SQLException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.ws.rs.WebApplicationException;

import java.io.*;
import java.util.*;

public class JPAUtils
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static EntityManagerFactory emf = null;
    private static final Integer DB_EXPECTED_MAJOR_VER = 81;
    private static String dbVersionErrorMsg = null;
    private static JaversSqlRepository javersSqlRepository = null;
    private static Javers javers = null;
    private static volatile boolean initialized = false;
    private static final Object INIT_LOCK = new Object();

    static
    {
        if (!ConfigureAPI.isInitialized())
        {
            String msg = "API is not configured, you must call ConfigureAPI.init() prior to executing any other methods";
            LOGGER.fatal(msg);
            throw new RuntimeException(msg);
        }
        // Initialization is now lazy so tests can supply their own EntityManagerFactory
        // before any JPA work is performed.
    }

    /**
     * Initialize JPAUtils with the standard production configuration.
     */
    public static void init()
    {
        initialize(null);
    }

    /**
     * Initialize JPAUtils with a specific EntityManagerFactory. This is intended
     * for integration tests that run against a throwaway database container.
     *
     * @param providedEmf the EntityManagerFactory to use; if null, the production
     *     configuration is used.
     */
    public static void initialize(EntityManagerFactory providedEmf)
    {
        synchronized (INIT_LOCK)
        {
            if (initialized)
            {
                return;
            }

            if (providedEmf != null)
            {
                emf = providedEmf;
            }
            else
            {
                createProductionEntityManagerFactory();
            }

            try
            {
                checkDbVersion();
            }
            catch (IllegalStateException e)
            {
                System.exit(-1);
            }

            initializeJavers();
            initialized = true;
        }
    }

    private static void createProductionEntityManagerFactory()
    {
        try
        {
            File f = new File(System.getenv("GSW_CONFIG") + "/epicdb_override.cfg");
            Map<String, String> props = new HashMap<>();

            if (f.exists())
            {
                LOGGER.info("Reading database configuration override file: {}", f.getAbsolutePath());
                props = readPropsFromFile(f);
            }
            else
            {
                LOGGER.info("No database configuration override file found at : {}", f.getAbsolutePath());
            }

            try
            {
                emf = Persistence.createEntityManagerFactory(ConfigureAPI.getPersistenceUnit(), props);
            }
            catch (SearchException se)
            {
                int count = 0;
                while (emf == null && count < 10)
                {
                    // if here, it's likely that Elasticsearch isn't ready yet; try again 10 times.
                    try
                    {
                        Thread.sleep(6000);
                    }
                    catch (InterruptedException ie)
                    {
                        LOGGER.error("Thread sleep was interrupted", ie);
                    }
                    emf = Persistence.createEntityManagerFactory(ConfigureAPI.getPersistenceUnit(), props);
                    count++;
                }

                if (emf == null)
                {
                    LOGGER.error("Failed to initialize entity manager factory due to search exception; possibly " +
                            "Elasticsearch isn't running?", se);
                }
            }
        }
        catch (PersistenceException e)
        {
            // Note: If we fail to create the EMF, we will likely throw an exception in the call to the logger.  Print the
            // stack trace so it will be helpful for troubleshooting.
            e.printStackTrace();
            LOGGER.error("Error creating entity manager factory for persistence unit {}", ConfigureAPI.getPersistenceUnit(), e);
        }
    }

    private static void initializeJavers()
    {
        try
        {
            ConnectionProvider connectionProvider = new ConnectionProvider()
            {
                private Connection conn;

                @Override
                public Connection getConnection() throws SQLException
                {
                    if (conn == null)
                    {
                        Set<PooledDataSource> pdsSet = C3P0Registry.getPooledDataSources();
                        Iterator<PooledDataSource> iterator = pdsSet.iterator();
                        PooledDataSource pooledDataSource = iterator.next();
                        conn = pooledDataSource.getConnection();
                    }

                    return conn;
                }
            };

            String databaseName = AppConfiguration.getConfigValue(AppConfiguration.AppConfigKey.DATABASE_NAME);
            javersSqlRepository = SqlRepositoryBuilder
                    .sqlRepository()
                    .withSchema(databaseName)
                    .withConnectionProvider(connectionProvider)
                    .withDialect(DialectName.MYSQL)
                    .build();
            javers = JaversBuilder
                    .javers()
                    .registerJaversRepository(javersSqlRepository)
                    .withObjectAccessHook(new HibernateEntityAccessHook())
                    .build();
        }
        catch (Exception e)
        {
            LOGGER.error("Error initializing Javers repository and Javers instance", e);
        }
    }

    /**
     * Close the current EntityManagerFactory and reset JPAUtils so it can be
     * re-initialized (for example, with a different database in a later test run).
     */
    public static void closeEntityManagerFactory()
    {
        synchronized (INIT_LOCK)
        {
            if (emf != null && emf.isOpen())
            {
                emf.close();
            }
            emf = null;
            javers = null;
            javersSqlRepository = null;
            initialized = false;
        }
    }

    private static void checkDbVersion() throws IllegalStateException
    {
        EntityManager em = null;

        try
        {
            em = emf.createEntityManager();
            VersionInfo version = em.find(VersionInfo.class, VersionInfo.Type.DATABASE);

            if (!Objects.equals(version.getMajor(), DB_EXPECTED_MAJOR_VER))
            {
                dbVersionErrorMsg = "Version mismatch.  Software: " +
                        DB_EXPECTED_MAJOR_VER + ", Database: " + version.getMajor() + "." + version.getMinor();

                System.err.println(dbVersionErrorMsg);
                LOGGER.fatal(dbVersionErrorMsg);
                throw new IllegalStateException(dbVersionErrorMsg);
            }
            else
            {
                LOGGER.info("Database version check passed. Version is {}.{}", version.getMajor(), version.getMinor());
            }
        }
        catch (RuntimeException e)
        {
            LOGGER.error("Error checking database version: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        finally
        {
            if (em != null)
            {
                em.close();
            }
        }
    }

    public JPAUtils()
    {}

    public static Javers getJavers()
    {
        if (!initialized)
        {
            initialize(null);
        }
        return javers;
    }

    public static EntityManagerFactory getEntityManagerFactory()
    {
        if (!initialized)
        {
            initialize(null);
        }
        return emf;
    }

    public static EntityManager getEntityManager()
    {
        return getEntityManagerFactory().createEntityManager();
    }

    public static void closeEntityManager(EntityManager em)
    {
        if (em != null)
        {
            try
            {
                if (em.getTransaction().isActive())
                {
                    //likely an bug/error occurred, safeguard rolling back the transaction
                    em.getTransaction().rollback();
                }
            }
            finally
            {
                em.close();
            }
        }
    }

    private static Map<String, String> readPropsFromFile(File f)
    {
        FileReader fileReader;
        Map<String, String> props = null;

        try
        {
            fileReader = new FileReader(f);
        }
        catch (FileNotFoundException e1)
        {
            LOGGER.error("Error could not find database configuration override file: {}", f.getAbsolutePath(), e1);
            return props;
        }

        try (BufferedReader bufferedReader = new BufferedReader(fileReader))
        {
            props = new HashMap<>();
            String line;
            while ((line = bufferedReader.readLine()) != null)
            {

                String[] tokens = line.split(",");

                if (tokens.length != 2)
                {
                    throw new RuntimeException("Invalid configuration found in file [" + f.getAbsolutePath() + "] for line [" + line + "]");
                }
                if (!tokens[0].startsWith("#"))
                {
                    props.put(tokens[0], tokens[1]);
                }

            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            LOGGER.error("Error reading database configuration override file: {}", f.getAbsolutePath(), e);
        }

        return props;
    }

    public static <T> List<T> getAllRecordsForTable(EntityManager em, Class<T> clazz)
    {
        CriteriaQuery<T> criteriaQuery = em.getCriteriaBuilder().createQuery(clazz);
        Root<T> root = criteriaQuery.from(clazz);
        criteriaQuery.select(root);
        List<T> all = em.createQuery(criteriaQuery).getResultList();
        return all;
    }

    public static <T> T getRecordById(EntityManager em, Class<T> clazz, Integer pk)
    {
        return em.find(clazz, pk);
    }

    public static void doInTransaction(EntityManager em, Runnable runnable)
    {
        em.getTransaction().begin();
        runnable.run();
        em.getTransaction().commit();
    }

    /**
     * Tries an operation, and logs and throws specified error message on error.
     */
    public static void basicTryCatch(EntityManagerOperator emop, String errMsg)
    {
        EntityManager em = JPAUtils.getEntityManager();
        try
        {
            emop.op(em);
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            LOGGER.error(errMsg, e);
            throw new WebApplicationException(errMsg, e);
        }
        finally
        {
            closeEntityManager(em);
        }
    }

    /**
     * Same as `basicTryCatch`, but wraps the op in a transaction begin and commit.
     */
    public static void basicTransaction(EntityManagerOperator emop, String errMsg)
    {
        JPAUtils.basicTryCatch((em) ->
        {
            em.getTransaction().begin();
            emop.op(em);
            em.getTransaction().commit();
        }, errMsg);
    }

    /**
     * Represents an operation performed using an entity manager.
     */
    public interface EntityManagerOperator
    {
        public void op(EntityManager em);
    }

    public static void logWebAppException(String msg)
    {
        LOGGER.error(msg);
        throw new WebApplicationException(msg);
    }

}
