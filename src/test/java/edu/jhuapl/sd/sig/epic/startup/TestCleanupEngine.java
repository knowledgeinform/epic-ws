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

import edu.jhuapl.sd.sig.epic.data.AppConfigurationDAO;
import edu.jhuapl.sd.sig.epic.model.ConfigKey;
import edu.jhuapl.sd.sig.epic.utils.FileUtils;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestCleanupEngine
{
    private static String exportDirPath;

    @BeforeAll
    public static void setup() throws IOException
    {
        // Use a unique temp directory so tests never touch the source tree
        // and never collide with real export output.
        Path exportDir = Files.createTempDirectory("epic-export-test");
        exportDirPath = exportDir.toFile().getAbsolutePath();
    }

    /**
     * Deletes the temp export directory itself after all tests have run.
     * Runs after all tests are complete.
     */
    @AfterAll
    public static void deleteExportDir()
    {
        FileUtils.deleteAllRecursively(Paths.get(exportDirPath));
    }

    @AfterEach
    public void cleanup()
    {
        File dir = new File(exportDirPath);
        File[] files = dir.listFiles();
        if (files != null)
        {
            for (File file : files)
            {
                file.delete();
            }
        }
    }

    @Test
    public void testOldExportCleanup() throws Exception
    {
        try (MockedStatic<AppConfiguration> mockConfig = Mockito.mockStatic(AppConfiguration.class);
                MockedStatic<AppConfigurationDAO> mockDao = Mockito.mockStatic(AppConfigurationDAO.class))
        {
            AppConfiguration.AppConfigKey exportRootDir = AppConfiguration.AppConfigKey.EXPORT_ROOT_DIR;
            mockConfig.when(() -> AppConfiguration.getConfigValue(exportRootDir))
                    .thenReturn(exportDirPath);
            mockDao.when(() -> AppConfigurationDAO.getConfigForKey(ConfigKey.CLEANUP_CHECK_FREQUENCY_MINUTES))
                    .thenReturn("1");
            mockDao.when(() -> AppConfigurationDAO.getConfigForKey(ConfigKey.CLEANUP_CLIENT_LOGS_FREQUENCY_HOURS))
                    .thenReturn("1");

            CleanupEngine cleanupEngine = CleanupEngine.getInstance();

            // Write a fresh file that should NOT be deleted
            File freshFile = new File(exportDirPath, "fresh-export.txt");
            try (FileWriter writer = new FileWriter(freshFile))
            {
                writer.write("fresh test data");
            }

            // Write an old file that SHOULD be deleted (older than 24 hours)
            File oldFile = new File(exportDirPath, "old-export.txt");
            try (FileWriter writer = new FileWriter(oldFile))
            {
                writer.write("old test data");
            }
            // Set last modified time to 48 hours ago
            Instant fortyEightHoursAgo = Instant.now().minus(Duration.ofHours(48));
            Files.setLastModifiedTime(oldFile.toPath(), FileTime.fromMillis(fortyEightHoursAgo.toEpochMilli()));

            // Run cleanup
            cleanupEngine.cleanupOldExports();

            // Verify: old file should be deleted, fresh file should remain
            assertFalse(oldFile.exists(), "Old file should have been deleted");
            assertTrue(freshFile.exists(), "Fresh file should still exist");
        }
    }
}
