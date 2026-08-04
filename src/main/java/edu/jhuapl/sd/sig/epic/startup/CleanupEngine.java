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

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.jhuapl.sd.sig.epic.data.AppConfigurationDAO;
import edu.jhuapl.sd.sig.epic.model.ConfigKey;
import edu.jhuapl.sd.sig.epic.startup.AppConfiguration.AppConfigKey;

public class CleanupEngine
{

    private final Logger LOGGER = LogManager.getLogger();
    private static CleanupEngine cleanupEngineInstance = null;
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);

    private Integer checkFrequencyMinutes = Integer
            .parseInt(AppConfigurationDAO.getConfigForKey(ConfigKey.CLEANUP_CHECK_FREQUENCY_MINUTES));

    private Integer checkClientLogFrequencyHours = Integer
            .parseInt(AppConfigurationDAO.getConfigForKey(ConfigKey.CLEANUP_CLIENT_LOGS_FREQUENCY_HOURS));

    public static CleanupEngine getInstance()
    {
        if (CleanupEngine.cleanupEngineInstance == null)
            CleanupEngine.cleanupEngineInstance = new CleanupEngine();
        return CleanupEngine.cleanupEngineInstance;
    }

    public void start()
    {
        ;
        this.executorService.scheduleAtFixedRate(() ->
        {
            try
            {
                this.cleanupOldExports();
            }
            catch (Exception e)
            {
                LOGGER.error("Error cleaning up old export file: " + e, e);
            }
        }, 0, this.checkFrequencyMinutes, TimeUnit.MINUTES);

        // once a day we'll check for old client log files that are more than 30 days old and should be deleted.
        this.executorService.scheduleAtFixedRate(() ->
        {
            try
            {
                this.cleanupOldClientLogFiles();
            }
            catch (Exception e)
            {
                LOGGER.error("Error cleaning up client log files", e);
            }
        }, 0, checkClientLogFrequencyHours, TimeUnit.HOURS);
    }

    public void stop()
    {
        List<Runnable> terminatedTasks = this.executorService.shutdownNow();
        LOGGER.info("Cleanup engine stopped, terminating " + terminatedTasks.size() + " tasks.");
    }

    /**
     * Scan export directory and delete anything over a day old.
     */
    public void cleanupOldExports() throws Exception
    {
        File[] files = new File(AppConfiguration.getConfigValue(AppConfigKey.EXPORT_ROOT_DIR)).listFiles();
        for (File file : files)
        {
            if (Duration.between(new Date(file.lastModified()).toInstant(), new Date().toInstant()).toHours() > 24)
            {
                if (file.isDirectory())
                    FileUtils.deleteDirectory(file);
                else if (file.isFile())
                    Files.delete(file.toPath());
            }
        }
    }

    public void cleanupOldClientLogFiles() throws Exception
    {
        File[] files = new File(AppConfiguration.getConfigValue(AppConfigKey.CLIENT_LOGS_DIR)).listFiles();
        if (files == null)
            return;
        for (File file : files)
        {
            // we don't want to delete directories
            if (file.isDirectory())
                continue;

            // check if file is more than 30 days old
            if (Duration.between(new Date(file.lastModified()).toInstant(), new Date().toInstant()).toDays() > 30)
            {
                Files.delete(file.toPath());
            }
        }
    }
}
