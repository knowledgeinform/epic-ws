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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class AppConfiguration
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static Properties properties = null;
    private static String configPath = System.getenv("GSW_CONFIG") + File.separator + "epic.properties";

    public static void loadAppConfiguration()
    {
        properties = new Properties();
        try (FileInputStream propInput = new FileInputStream(configPath))
        {
            properties.load(propInput);

            // Log each of the entries found
            for (AppConfigKey key : AppConfigKey.values())
            {
                LOGGER.info("Configuration item " + key.toString() + " = " + getConfigValue(key));
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Unable to read epic.properties", e);
        }
    }

    public static String getConfigValue(AppConfigKey key)
    {
        if (properties != null)
        {
            if (key == AppConfigKey.PERFORM_INDEXING)
            {
                return properties.getProperty(key.toString(), "TRUE");
            }
            else
            {
                return properties.getProperty(key.toString());
            }
        }
        throw new RuntimeException("App configuration Has not been loaded");
    }

    public enum AppConfigKey
    {
        ALLOWED_ORIGIN,
        DISABLE_EMAIL,
        EXPORT_ROOT_DIR,
        TEMPLATES_ROOT_DIR,
        CLIENT_LOGS_DIR,
        DATABASE_NAME,
        ATTACHMENT_MAX_ALLOWED_FILE_SIZE_BYTES,
        THREADS_FOR_INDEXING,
        SYNCHRONOUS_CLASSES_FOR_INDEXING,
        PERFORM_INDEXING
    }

    // Update the "LAST_SHUTDOWN_TIME" entry in the config file when the current time
    public static void UpdatePerormIndexingFlag()
    {
        AppConfiguration.properties.setProperty(AppConfigKey.PERFORM_INDEXING.toString(), "FALSE");

        try (FileOutputStream propOut = new FileOutputStream(configPath))
        {
            AppConfiguration.properties.store(propOut, "updated by the EPIC application");
        }
        catch (Exception exc)
        {
            LOGGER.warn("Unable to update properties file... " + exc.getMessage());
        }
    }
}
