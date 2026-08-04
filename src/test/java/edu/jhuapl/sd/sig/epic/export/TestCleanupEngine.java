/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.export;

import edu.jhuapl.sd.sig.epic.utils.TestUtils;

import edu.jhuapl.sd.sig.epic.startup.CleanupEngine;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.fail;

public class TestCleanupEngine
{

    private static CleanupEngine cleanupEngine;

    @BeforeAll
    public static void beforeClass()
    {
        TestUtils.init();
    }

    @BeforeEach
    public void beforeEach()
    {
        this.cleanupEngine = CleanupEngine.getInstance();
    }

    @AfterAll
    public static void afterClass()
    {
        // the start method of the cleanupEngine never gets called in this class, so no need to call stop.
        // leaving this here in the event that more tests are added in the future that do require this.
        //        cleanupEngine.stop();
    }

    @Test
    public void testOldExportCleanup()
    {
        try
        {
            this.cleanupEngine.cleanupOldExports();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception: " + e);
        }
    }
}
