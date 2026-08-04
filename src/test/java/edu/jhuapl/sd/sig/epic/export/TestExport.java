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

import java.io.IOException;

import javax.persistence.EntityManager;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.hibernate.Hibernate;

import edu.jhuapl.sd.sig.epic.utils.DataGeneratorUtils;
import edu.jhuapl.sd.sig.epic.utils.TestUtils;
import freemarker.template.TemplateException;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.*;
import edu.jhuapl.sd.sig.epic.resource.Export;
import edu.jhuapl.sd.sig.epic.startup.AppConfiguration;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.fail;

public class TestExport
{

    private static Export exportResource;
    private static EntityManager em;

    @BeforeAll
    public static void beforeClass()
    {
        Configurator.setRootLevel(Level.ERROR);
        AppConfiguration.loadAppConfiguration();
        TestUtils.init();
    }

    @BeforeEach
    public void beforeEach()
    {
        exportResource = new Export();
        em = JPAUtils.getEntityManager();
    }

    @AfterEach
    public void afterEach()
    {
        JPAUtils.closeEntityManager(em);
    }

    @Test
    public void testRunExport() throws Exception
    {

        try
        {
            ProcedureDetails run = JPAUtils.getRecordById(em, ProcedureDetails.class, TestUtils.createCleanRun().getPk());

            // Export the run.
            exportResource.exportProcedureResource(run.getId());

        }
        catch (TemplateException e)
        {
            e.printStackTrace();
            fail("Error processing run export template.");
        }
        catch (IOException e)
        {
            e.printStackTrace();
            fail("Error writing run to file.");
        }

    }

    @Test
    public void testAttachmentExport() throws Exception
    {
        ProcedureDetails run = JPAUtils.getRecordById(em, ProcedureDetails.class, TestUtils.createCleanRun().getPk());
        ProcedureDetails parentProcedure = run.getOriginalProcedureDetails();
        Hibernate.initialize(parentProcedure.getProcedureDetailRuns());
        try
        {
            exportResource.exportProcedure(em, parentProcedure);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("IO Exception: " + e);
        }
    }

    @Test
    public void testProgramExport()
    {
        Program program = DataGeneratorUtils.getRandomProgram();
        try
        {
            exportResource.exportProgram(em, JPAUtils.getRecordById(em, Program.class, program.getPk()), System.currentTimeMillis());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("IO Exception: " + e);
        }
    }
}
