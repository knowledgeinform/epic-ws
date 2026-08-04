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
import edu.jhuapl.sd.sig.epic.data.RunDAO;
import edu.jhuapl.sd.sig.epic.data.TestProcedureDAO;
import edu.jhuapl.sd.sig.epic.data.TestProcedureDetailsDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.*;
import edu.jhuapl.sd.sig.epic.utils.DataGeneratorUtils;
import edu.jhuapl.sd.sig.epic.utils.TestUtils;
import org.junit.jupiter.api.*;

import javax.persistence.EntityManager;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
public class TestRevisionCreationOperations
{
    @Context
    private static SecurityContext sc;

    private ProcedureDetails pd = null;
    private static EntityManager em;
    private static Users user;

    @BeforeAll
    public static void beforeClass()
    {
        TestUtils.init();
        user = DataGeneratorUtils.getRandomUser();
        em = JPAUtils.getEntityManager();
    }

    @AfterAll
    public static void afterClass()
    {
        JPAUtils.closeEntityManager(em);
    }

    @BeforeEach
    public void before() throws Exception
    {
        // TODO: This is causing errors, because the data generator randomly makes PD sometimes a draft, sometimes a ready procedure.
        // If it's generated as READY, then it generates two runs, too, which causes testRunRevisionCreation to fail.
        // If it's generated as DRAFT, there are no runs, the rest of this method transitions it to READY, and
        // testRunRevisionCreation passes.
        pd = DataGeneratorUtils.generateProcedureDef(2, 2, 3, 3).getProcedureDetails().iterator().next();
        pd = JPAUtils.getRecordById(em, ProcedureDetails.class, pd.getPk()); // Pull again, since session ended.
        TestProcedureDAO.transitionToReadyWrapper(pd);
        pd.setStatus(ProcedureStatus.READY);
        user = JPAUtils.getRecordById(em, Users.class, user.getUserId());
    }

    @AfterEach
    public void after()
    {
        // TODO: Re-enable. There is currently a bug preventing this from working as intended.
        // TestProcedureDAO.deleteProcedureDef(pd.getProcedureDef().getPk());
    }

    @Test
    public void testCreateProcedureDetails()
    {
        assertNotNull(pd);
        assertNotNull(pd.getEditType());
        assertNotNull(pd.getId());
        assertNotNull(pd.getProcedureDef());
        assertNotNull(pd.getProcedureHeader());
        assertNotNull(pd.getProcedureInstructions());
        assertNotNull(pd.getStatus());
        assertNotNull(pd.getStepGroupDefs());
    }

    @Test
    public void testRevisionCreation()
    {

        ProcedureDetails rev = ProcedureDetailsDAO.createRevision(em, pd.getId(), user);
        assertNotNull(rev);
        checkRevProcedureDetailsEquality(pd, rev);

        // Try creating a second revision. Run same checks.
        ProcedureDetails rev2 = ProcedureDetailsDAO.createRevision(em, pd.getId(), user);
        assertNotNull(rev2);
        checkRevProcedureDetailsEquality(rev, rev2);

    }

    @Test
    public void testRevisionCreationWithRedlineEdits()
    {

        // Ensure our pd has at least 3 instructions and step groups, so that we can test adding, editing, and deleting.
        pd.setStepGroupDefs(DataGeneratorUtils.generateStepGroupDefs(3, 3, 3, 3, pd, null, pd, 0));
        pd.setProcedureInstructions(DataGeneratorUtils.generateProcedureInstructions(3, 3, pd));

        TestProcedureDetailsDAO.addRedLines(em, pd, pd);
        ProcedureDetails rev = ProcedureDetailsDAO.createRevision(em, pd.getId(), user); // Not finding a revision in ProcedureDetailsDAO:258. Does it have revisions? Yes, but only a REDLINE_EDIT, which gets filtered out.
        assertNotNull(rev);
        checkRevProcedureDetailsEquality(pd, rev);
    }

    @Test
    public void testRunRevisionCreation()
    {
        Run run = DataGeneratorUtils.generateRuns(1, 1, pd).get(0).getRun();

        String runId = run.getProcedureDetails().getId();
        ProcedureDetails rev1 = ProcedureDetailsDAO.createRevision(em, runId, user);
        assertNotNull(rev1);
        checkRevProcedureDetailsEquality(pd, rev1);

        // Try creating a second revision. Run same checks.
        ProcedureDetails rev2 = ProcedureDetailsDAO.createRevision(em, runId, user);
        assertNotNull(rev2);
        checkRevProcedureDetailsEquality(rev1, rev2);
    }

    @Test
    public void testGetLatestRedlineForRun()
    {

        ProcedureDetails rev = null;
        ProcedureDetails lv = null;

        // Ensure our pd has at least 3 instructions and step groups, so that we can test adding, editing, and deleting.
        pd.setStepGroupDefs(DataGeneratorUtils.generateStepGroupDefs(3, 3, 3, 3, pd, null, pd, 0));
        pd.setProcedureInstructions(DataGeneratorUtils.generateProcedureInstructions(3, 3, pd));

        // Create a run; this is a prerequisite to redlining.
        Run run = TestProcedureDetailsDAO.generateTestRun(em, pd);
        // Find next unique run number:
        int nextRunNumber = pd.getProcedureDef().getProcedureDetails().stream()
                .filter(pd -> pd.getEditType().equals(EditType.RUN))
                .collect(Collectors.toList()).size() + 1;
        RunDAO.createNewRun(em, pd, nextRunNumber, run, user);

        // No redlines.
        lv = ProcedureDetailsDAO.getLatestVersionForProcedure(em, run.getProcedureDetails().getId());
        assertEquals(lv, pd);

        // One redline revision. Check the latest version is our new revision, and that its ID is what we'd expect.
        rev = ProcedureDetailsDAO.createRevision(em, pd.getId(), user);
        TestProcedureDetailsDAO.addRedLines(em, rev, pd);
        lv = ProcedureDetailsDAO.getLatestVersionForProcedure(em, pd.getId());
        assertEquals(lv, rev);

        // You ought to ultimately end up with a procedure details that has EditType.ORIGINAL and an id of programCode-subsystemCode-procedureDefPK-revisionNumber
        assertEquals(pd.getRedlinedVersion(), pd.getId() + "-000-1");  // TODO: Verify this is correct expectation.

        // Two redline revisions.
        TestProcedureDetailsDAO.addRedLines(em, rev, pd);
        lv = ProcedureDetailsDAO.getLatestVersionForProcedure(em, pd.getId());
        assertEquals(lv, rev);
        assertEquals(lv.getRedlinedVersion(), pd.getId() + "-000-2");

    }

    @Test
    public void testShouldNotCreateIfNotReady()
    {
        for (ProcedureStatus status : ProcedureStatus.values())
        {
            if (pd.getStatus().equals(ProcedureStatus.READY))
                continue;

            Assertions.assertThrows(WebApplicationException.class, () ->
            {
                pd.setStatus(status);
            });

            ProcedureDetailsDAO.createRevision(em, pd.getId(), user);
        }
    }

    /**
     * Ensures all instruction attributes are identical, except EditType which must be set to Original.
     */
    public void checkClonedInstructionEquality(ProcedureInstruction original, ProcedureInstruction clone)
    {
        assertNotEquals(original.getPk(), clone.getPk());
        assertNotEquals(original.getProcedureDetails(), clone.getProcedureDetails());
        assertEquals(original.getSectionName(), clone.getSectionName());
        assertEquals(original.getText(), clone.getText());
        assertEquals(clone.getEditType(), EditType.ORIGINAL);
    }

    /**
     * Ensures all instruction attributes are identical, except EditType which must be set to Original.
     */
    public void checkClonedInstructionsEquality(List<ProcedureInstruction> original, List<ProcedureInstruction> rev)
    {

        int origIter = 0;
        int revIter = 0;
        while (revIter < rev.size())
        {
            ProcedureInstruction curRev = rev.get(revIter);
            ProcedureInstruction curOrig = original.get(origIter);
            if (curOrig.getEditType() != EditType.REDLINE_DELETE)
            {
                checkClonedInstructionEquality(curOrig, curRev);
                revIter++;
            }
            // If it is redline_delete, we just skip the item in orig and assume it's skipped in rev. If it's really there, the equality check will fail.
            origIter++;
        }

        // Ensure we got to the end of both lists. If not, there was a coppying issue.
        assertEquals(origIter, original.size());
        assertEquals(revIter, rev.size());

    }

    public void checkRevProcedureStepEquality(StepDef original, StepDef rev)
    {
        assertNotEquals(original.getPk(), rev.getPk());
        assertNotEquals(original.getStepGroupDef(), rev.getStepGroupDef());
        assertEquals(rev.getInstructions(), original.getInstructions());
        assertEquals(rev.getDisplayOrder(), original.getDisplayOrder());
        assertEquals(rev.getStepName(), original.getStepName());
        assertEquals(rev.getRequireWitness(), original.getRequireWitness());
        assertEquals(rev.getHazardous(), original.getHazardous());
        assertEquals(rev.getEsd0(), original.getEsd0());
        assertEquals(rev.getMandatoryInspection(), original.getMandatoryInspection());
        assertEquals(rev.getType(), original.getType());
        assertEquals(rev.getEditType(), EditType.ORIGINAL);
        assertEquals(rev.getIsManualValidation(), false);
        assertEquals(rev.getAllowEquipmentEntry(), original.getAllowEquipmentEntry());
    }

    public void checkRevProcedureStepsEquality(List<StepDef> original, List<StepDef> rev)
    {

        // This compare assumes the ordering of the two groups is stable, but that the sizes of the lists may have changed because of redline items being removed.
        int origIter = 0;
        int revIter = 0;
        while (revIter < rev.size())
        {
            StepDef curRev = rev.get(revIter);
            StepDef curOrig = original.get(origIter);

            assert (curRev.getDisplayOrder().equals(revIter + 1));

            if (curOrig.getEditType() != EditType.REDLINE_DELETE)
            {
                checkRevProcedureStepEquality(curOrig, curRev);
                revIter++;
            }
            // If it is redline_delete, we just skip the item in orig and assume it's skipped in rev. If it's really there, the equality check will fail.
            origIter++;
        }

        // Ensure we got to the end of both lists. If not, there was a coppying issue.
        assertEquals(origIter, original.size());
        assertEquals(revIter, rev.size());
    }

    public void checkRevProcedureStepGroupDefEquality(StepGroupDef original, StepGroupDef rev)
    {
        assertNotEquals(original.getPk(), rev.getPk());
        assertEquals(original.getStepGroupName(), rev.getStepGroupName());
        assertNotEquals(original.getProcedureDetails(), rev.getProcedureDetails());
        if (original.getStepGroupDefParent() == null)
        {
            assertNull(rev.getStepGroupDefParent());
        }
        else
        {
            assertNotEquals(original.getStepGroupDefParent(), rev.getStepGroupDefParent());
        }
        assertEquals(original.getEditType(), EditType.ORIGINAL);
        assertEquals(original.getDescription(), rev.getDescription());

        checkRevProcedureStepsEquality(new ArrayList<>(original.getStepDefs()), new ArrayList<>(rev.getStepDefs()));

        checkRevProcedureStepGroupDefsEquality(new ArrayList<>(original.getStepGroupDefsChildren()), new ArrayList<>(rev.getStepGroupDefsChildren()));

    }

    public void checkRevProcedureStepGroupDefsEquality(List<StepGroupDef> original, List<StepGroupDef> rev)
    {
        // This compare assumes the ordering of the two groups is stable, but that the sizes of the lists may have changed because of redline items being removed.
        int origIter = 0;
        int revIter = 0;
        while (revIter < rev.size())
        {
            StepGroupDef curRev = rev.get(revIter);
            StepGroupDef curOrig = original.get(origIter);

            assert (curRev.getDisplayOrder().equals(revIter + 1));

            if (curOrig.getEditType() != EditType.REDLINE_DELETE)
            {
                checkRevProcedureStepGroupDefEquality(curOrig, curRev);
                revIter++;
            }
            // If it is redline_delete, we just skip the item in orig and assume it's skipped in rev. If it's really there, the equality check will fail.
            origIter++;
        }

        // Ensure we got to the end of both lists. If not, there was a coppying issue.
        assertEquals(origIter, original.size());
        assertEquals(revIter, rev.size());
    }

    /**
     * Compares a revision against the original ProcedureDetails it spawned from by comparing properties. The new ID
     * string is validated. Groups and steps are validated, recursively. Red-Lines are checked to ensure they are not copied over.
     */
    public void checkRevProcedureDetailsEquality(ProcedureDetails original, ProcedureDetails rev)
    {

        assertNotEquals(original.getPk(), rev.getPk());
        assertEquals(original.getProcedureDefVersion() + 1, rev.getProcedureDefVersion());
        assertEquals(original.getProcedureDef(), rev.getProcedureDef());
        assertEquals(rev.getStatus(), ProcedureStatus.DRAFT);
        assertNotEquals(original.getId(), rev.getId());
        assertEquals(rev.getProcedureDef().getProgram().getCode() + "-" + rev.getProcedureDef().getSubsystem().getCode() + "-" + rev.getProcedureDef().getPk() + "-" + rev.getProcedureDefVersion(),
                rev.getId());
        assertEquals(original.getOriginalProcedureDetails(), rev.getOriginalProcedureDetails());
        assertEquals(rev.getEditType(), EditType.ORIGINAL);
        assertNull(original.getRunNumber());
        assertEquals(original.getHazardous(), rev.getHazardous());
        assertEquals(original.getEsd0(), rev.getEsd0());
        assertEquals(original.getHazardDescription(), rev.getHazardDescription());
        assertNull(original.getRedlinedVersion());
        assertNull(rev.getProcedureApprovalDueDate());

        checkClonedInstructionsEquality(new ArrayList<>(original.getProcedureInstructions()), new ArrayList<>(rev.getProcedureInstructions()));
        checkRevProcedureStepGroupDefsEquality(new ArrayList<>(original.getStepGroupDefs()), new ArrayList<>(rev.getStepGroupDefs()));
    }
}
