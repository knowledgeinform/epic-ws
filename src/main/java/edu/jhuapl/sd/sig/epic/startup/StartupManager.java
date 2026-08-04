/*
 * COPYRIGHT NOTICE
 * (C) 2010 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.startup;

import edu.jhuapl.sd.sig.epic.data.util.ConfigureAPI;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.util.*;
import edu.jhuapl.sd.sig.epic.startup.AppConfiguration.AppConfigKey;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.TimeZone;

@WebListener
public class StartupManager implements ServletContextListener
{

    private static final Logger LOGGER = LogManager.getLogger();

    public StartupManager()
    {}

    @Override
    public void contextInitialized(ServletContextEvent event)
    {
        try
        {
            Configurator.initialize(null, System.getenv("GSW_CONFIG") + File.separator + "loggingEPIC.xml");
            AppConfiguration.loadAppConfiguration();

            // Ensure export dir exists.
            Path exportDir = Paths.get(AppConfiguration.getConfigValue(AppConfigKey.EXPORT_ROOT_DIR) + File.separator);
            exportDir.toFile().mkdirs();
            if (Files.notExists(exportDir) || !Files.isWritable(exportDir))
            {
                throw new RuntimeException("Exports directory does not exist.");
            }

        }
        catch (Exception e)
        {
            System.err.println("!!!!!!!!!!Unable to start application");
            e.printStackTrace();
            System.exit(-1);
        }

        TimeZone.setDefault(TimeZone.getTimeZone("GMT+0"));
        ConfigureAPI.init();

        LOGGER.info("Perform database indexing if needed");
        long start = System.currentTimeMillis();
        try
        {
            IndexManager.indexDatabase();
        }
        catch (InterruptedException e)
        {
            String errMsg = "Unable to start application due to error during indexing database!";
            LOGGER.fatal(errMsg);
            System.err.println(errMsg);
            e.printStackTrace();
            System.exit(-1);
        }
        long end = System.currentTimeMillis();
        double elapsedMillis = end - start;
        double elapsedSec = elapsedMillis / 1000;
        LOGGER.info("Database indexed in " + elapsedSec + " seconds.");

        EmailEngine.getInstance().start();
        CleanupEngine.getInstance().start();

        //Initialize the Authorization JPA Realm from gswSecurity:
        //        String authzAppName = AppConfigurationDAO.getConfigForKey(ConfigKey.AUTHZ_APPLICATION_NAME);
        //        AuthzOnlyJpaConfigureAPI.init(authzAppName, gswLogger);

        LOGGER.info("Application Starting...");
    }

    @Override
    public void contextDestroyed(ServletContextEvent iArg0)
    {
        LOGGER.info("Application Terminating...");

        JPAUtils.closeEntityManagerFactory();
        EmailEngine.getInstance().stop();
        CleanupEngine.getInstance().stop();

        LOGGER.info("Termination Complete");
    }

}
