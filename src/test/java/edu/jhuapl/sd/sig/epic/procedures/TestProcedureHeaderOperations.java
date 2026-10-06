/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.procedures;

import edu.jhuapl.sd.sig.epic.data.ProcedureDetailsDAO;
import edu.jhuapl.sd.sig.epic.data.StepDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.*;
import edu.jhuapl.sd.sig.epic.utils.DataGeneratorUtils;
import edu.jhuapl.sd.sig.epic.utils.DbTestContainer;
import org.junit.jupiter.api.*;

import javax.persistence.EntityManager;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.util.SortedSet;

import static org.junit.jupiter.api.Assertions.*;

public class TestProcedureHeaderOperations
{
    private static EntityManager em = null;
    private ProcedureDetails procedureDetails = null;

    @Context
    SecurityContext sc;

    private static DbTestContainer container;

    @BeforeAll
    static void beforeAll() throws Exception
    {
        container = new DbTestContainer();
        container.start();
        em = JPAUtils.getEntityManager();
    }

    @AfterAll
    static void afterAll()
    {
        container.stop();
    }

    @BeforeEach
    public void before() throws Exception
    {
        // set up base case
        procedureDetails = setCleanEsd0AndHazard(em, false, false);
    }

    @Test
    public void testEsd0FlagSettingWithNoEsd0Steps()
    {
        // use the groupsHaveEsd0Child method to confirm no steps with Esd0
        assertFalse(ProcedureDetailsDAO.groupsHaveEsd0Child(procedureDetails.getStepGroupDefs()));
        assertFalse(procedureDetails.getEsd0());

        // confirm that can set procedure esd using ProcedureDetailsDAO
        try
        {
            // set to true
            procedureDetails = ProcedureDetailsDAO.updateProcedureDetailsEsd0(em, procedureDetails.getPk(), true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }

        procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDetails.getPk());
        assertTrue(procedureDetails.getEsd0());

        // confirm that can set procedure esd using ProcedureDetailsDAO
        try
        {
            // set to false
            procedureDetails = ProcedureDetailsDAO.updateProcedureDetailsEsd0(em, procedureDetails.getPk(), false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }

        procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDetails.getPk());
        assertFalse(procedureDetails.getEsd0());
    }

    @Test
    public void testEsd0FlagSettingWithEsd0Steps()
    {
        // use the groupsHaveEsd0Child method to confirm no steps with Esd0
        assertFalse(ProcedureDetailsDAO.groupsHaveEsd0Child(procedureDetails.getStepGroupDefs()));
        assertFalse(procedureDetails.getEsd0());

        // now update a step with esd0 flag
        StepDef step = procedureDetails.getStepGroupDefs().first().getStepDefs().first();
        step.setEsd0(true);
        try
        {
            if (step.getType().equals(StepType.TABLE))
            {
                StepDAO.updateTableStep(em, (StepTable) step, false);
            }
            else
            {
                StepDAO.updateSingleValueOrCheckboxStep(em, step);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }

        // the procedure esd0 flag should have been set to true during the step update.
        procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDetails.getPk());
        assertTrue(procedureDetails.getEsd0());

        // attempt to change the esd0 flag to false; should throw an exception
        ProcedureDetails finalProcedureDetails = procedureDetails;
        assertThrows(WebApplicationException.class, () -> ProcedureDetailsDAO.updateProcedureDetailsEsd0(em, finalProcedureDetails.getPk(), false));
    }

    @Test
    public void testHazardFlagSettingWithNoHazardousSteps()
    {
        // use the groupsHaveHazardousChild method to confirm no steps with Esd0
        assertFalse(ProcedureDetailsDAO.groupsHaveHazardousChild(procedureDetails.getStepGroupDefs()));
        assertFalse(procedureDetails.getHazardous());

        // confirm that can set procedure hazard using ProcedureDetailsDAO
        try
        {
            // set to true
            procedureDetails = ProcedureDetailsDAO.updateProcedureDetailsHazard(em, procedureDetails.getPk(), true, "");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }

        procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDetails.getPk());
        assertTrue(procedureDetails.getHazardous());

        // confirm that can set procedure hazard using ProcedureDetailsDAO
        try
        {
            // set to false
            procedureDetails = ProcedureDetailsDAO.updateProcedureDetailsHazard(em, procedureDetails.getPk(), false, "");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }

        procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDetails.getPk());
        assertFalse(procedureDetails.getHazardous());

        // confirm can set hazard description
        String hazardDescription = "This is a hazard.";

        try
        {
            // set to true
            procedureDetails = ProcedureDetailsDAO.updateProcedureDetailsHazard(em, procedureDetails.getPk(), true, hazardDescription);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }

        procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDetails.getPk());
        assertTrue(procedureDetails.getHazardous());
        assertEquals(hazardDescription, procedureDetails.getHazardDescription());
    }

    @Test
    public void testHazardFlagSettingWithHazardSteps()
    {
        // use the groupsHaveHazardousChild method to confirm no steps with hazard
        assertFalse(ProcedureDetailsDAO.groupsHaveHazardousChild(procedureDetails.getStepGroupDefs()));
        assertFalse(procedureDetails.getHazardous());

        // now update a step with hazardous flag
        StepDef step = procedureDetails.getStepGroupDefs().first().getStepDefs().first();
        step.setHazardous(true);
        try
        {
            if (step.getType().equals(StepType.TABLE))
            {
                StepDAO.updateTableStep(em, (StepTable) step, false);
            }
            else
            {
                StepDAO.updateSingleValueOrCheckboxStep(em, step);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }

        // the procedure hazard flag should have been set to true during the step update.
        procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDetails.getPk());
        assertTrue(procedureDetails.getHazardous());

        // attempt to change the hazard flag to false; should throw an exception
        ProcedureDetails finalProcedureDetails = procedureDetails;
        assertThrows(WebApplicationException.class, () -> ProcedureDetailsDAO.updateProcedureDetailsHazard(em, finalProcedureDetails.getPk(), false, ""));
    }

    private ProcedureDetails setCleanEsd0AndHazard(EntityManager em, Boolean isEsd0, Boolean isHazardous)
            throws Exception
    {
        // first generate a procedure with one top-level group and some steps
        ProcedureDef procedureDef = DataGeneratorUtils.generateProcedureDef(1, 0, 1, 3);
        ProcedureDetails procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDef.getProcedureDetails().iterator().next().getPk());

        // go through the steps and mark everything non esd0 and non hazardous as the base case.
        setAllStepsToFlags(em, procedureDetails.getStepGroupDefs(), isEsd0, isHazardous);
        procedureDetails.setEsd0(isEsd0);
        procedureDetails.setHazardous(isHazardous);
        procedureDetails.setHazardDescription("");
        procedureDetails = em.merge(procedureDetails);

        return procedureDetails;
    }

    private SortedSet<StepGroupDef> setAllStepsToFlags(EntityManager em, SortedSet<StepGroupDef> groups, Boolean isEsd, Boolean isHazardous)
    {
        groups.stream().forEach(group ->
        {
            group.getStepDefs().stream().forEach(step ->
            {
                StepDef stepFromDb = JPAUtils.getRecordById(em, StepDef.class, step.getPk());
                stepFromDb.setEsd0(isEsd);
                stepFromDb.setHazardous(isHazardous);
                em.merge(stepFromDb);
                step = stepFromDb;
            });
            if (group.getStepGroupDefsChildren() != null && !group.getStepGroupDefsChildren().isEmpty())
            {
                setAllStepsToFlags(em, group.getStepGroupDefsChildren(), isEsd, isHazardous);
            }
        });

        return groups;
    }
}
