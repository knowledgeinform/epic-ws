/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.runs;

import edu.jhuapl.sd.sig.epic.data.ApprovalsDAO;
import edu.jhuapl.sd.sig.epic.data.RunDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.*;
import edu.jhuapl.sd.sig.epic.utils.DataGeneratorUtils;
import edu.jhuapl.sd.sig.epic.utils.DbTestContainer;
import edu.jhuapl.sd.sig.epic.utils.TestUtils;
import org.junit.jupiter.api.*;

import javax.persistence.EntityManager;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestRunStateChangeOperations
{
    private static EntityManager em = null;
    private static ProcedureDetails run = null;

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
        run = TestUtils.createCleanRun();
    }

    @Test
    public void testRunCreationGeneratesHistory()
    {
        assertNotNull(run);
        assertNotNull(run.getHistories());
        assertEquals(1, run.getHistories().size());

        History history = run.getHistories().first();
        assertEquals("Added " + run.toString(), history.getDescription());
        assertEquals(run.getRun().getUser().getUserId(), history.getUser().getUserId());
        assertNotNull(history.getTimestamp());
    }

    @Test
    public void testRunCloseoutSubmissionGenerateHistory()
    {
        assertNotNull(run);
        Users randomUser = DataGeneratorUtils.getRandomUser();
        run = RunDAO.transitionRunToReviewing(em, run.getRun().getPk(), randomUser).getProcedureDetails();

        // assert is in approved status, since no approvers were added.
        assertEquals(RunStatus.APPROVED, run.getRun().getStatus());

        // there ought to be two history objects
        assertNotNull(run.getHistories());
        assertEquals(2, run.getHistories().size());

        assertExpectedHistoryFound(run, RunStatus.APPROVED.toString(), randomUser, 1);

        // also assert closeout data on run is not null
        assertNotNull(run.getRun().getCloseoutSubmissionUser());
        assertEquals(randomUser.getUserId(), run.getRun().getCloseoutSubmissionUser().getUserId());
        assertNotNull(run.getRun().getCloseoutSubmittedDate());
    }

    @Test
    public void testRunTransitionToCorrectingGeneratesHistory()
    {
        assertNotNull(run);
        Users randomUser = DataGeneratorUtils.getRandomUser();
        run = RunDAO.transitionRunToReviewing(em, run.getRun().getPk(), randomUser).getProcedureDetails();

        // assert is in approved status
        assertEquals(RunStatus.APPROVED, run.getRun().getStatus());

        // there ought to be two history objects
        assertNotNull(run.getHistories());
        assertEquals(2, run.getHistories().size());

        // now transition run to correcting status
        run = RunDAO.transitionRunToCorrectingStatus(em, run.getRun(), randomUser).getProcedureDetails();

        // assert run is in correcting status
        assertEquals(RunStatus.CORRECTING, run.getRun().getStatus());

        // there ought to be three history objects
        assertNotNull(run.getHistories());
        assertEquals(3, run.getHistories().size());

        assertExpectedHistoryFound(run, RunStatus.CORRECTING.toString(), randomUser, 1);
    }

    @Test
    public void testRunTransitionToCompletedStatus()
    {
        assertNotNull(run);
        Users randomUser = DataGeneratorUtils.getRandomUser();
        run = RunDAO.transitionRunToReviewing(em, run.getRun().getPk(), randomUser).getProcedureDetails();

        // assert is in approved status
        assertEquals(RunStatus.APPROVED, run.getRun().getStatus());

        // there ought to be two history objects
        assertNotNull(run.getHistories());
        assertEquals(2, run.getHistories().size());

        // now transition the run to completed status
        run = RunDAO.transitionRunToCompletedStatus(em, run.getRun().getPk(), randomUser).getProcedureDetails();

        // assert run is in completed status
        assertEquals(RunStatus.COMPLETED, run.getRun().getStatus());

        // there ought to be three history objects
        assertNotNull(run.getHistories());
        assertEquals(3, run.getHistories().size());

        assertExpectedHistoryFound(run, RunStatus.COMPLETED.toString(), randomUser, 1);
    }

    @Test
    public void testRunApprovalGeneratesHistory()
    {
        assertNotNull(run);
        Users randomUser = DataGeneratorUtils.getRandomUser();
        RunApproval runApproval = assertRunHasApproverAndIsInExpectedStatus(run, randomUser);

        // set the approval equal to true
        runApproval.setIsApproved(true);
        ApprovalsDAO.handleRunApprovalDecision(em, runApproval);

        // re-query the database for the procedure details
        run = JPAUtils.getRecordById(em, ProcedureDetails.class, run.getPk());

        // there ought to be four history objects on the procedure
        assertNotNull(run.getHistories());
        assertEquals(4, run.getHistories().size());

        // assert that the approval was recorded
        assertExpectedHistoryFound(run, runApproval.toString(), randomUser, 1);

        // since this was the only approval, there should also be a history object about the procedure being fully approved
        assertExpectedHistoryFound(run, RunStatus.APPROVED.toString(), randomUser, 1);

        // also the procedure should be in approved status
        assertEquals(RunStatus.APPROVED, run.getRun().getStatus());

        // all dates except completed date should be not null
        assertNotNull(run.getRun().getCloseoutSubmittedDate());
        assertNull(run.getRun().getCloseoutCompletedDate());
    }

    @Test
    public void testRunRejectionGeneratesHistory()
    {
        assertNotNull(run);
        Users randomUser = DataGeneratorUtils.getRandomUser();
        RunApproval runApproval = assertRunHasApproverAndIsInExpectedStatus(run, randomUser);

        // set the approval equal to false
        runApproval.setIsApproved(false);
        ApprovalsDAO.handleRunApprovalDecision(em, runApproval);

        // re-query the database for the procedure details
        run = JPAUtils.getRecordById(em, ProcedureDetails.class, run.getPk());

        // there ought to be four history objects on the procedure
        assertNotNull(run.getHistories());
        assertEquals(4, run.getHistories().size());

        // should have a history object about the approval
        // assert that the approval decision was recorded
        assertExpectedHistoryFound(run, runApproval.toString(), randomUser, 1);

        // run status should have been set to CORRECTING
        assertEquals(RunStatus.CORRECTING, run.getRun().getStatus());

        // since this was the only approval, there should also be a history object about the run being in correcting status
        assertExpectedHistoryFound(run, RunStatus.CORRECTING.toString(), randomUser, 1);
    }

    @Test
    public void testRunApprovalDecisionRemovalGeneratesHistory()
    {
        assertNotNull(run);
        Users randomUser = DataGeneratorUtils.getRandomUser();
        RunApproval runApproval = assertRunHasApproverAndIsInExpectedStatus(run, randomUser);

        // set the approval equal to true
        runApproval.setIsApproved(true);
        ApprovalsDAO.handleRunApprovalDecision(em, runApproval);

        // re-query the database for the procedure details
        run = JPAUtils.getRecordById(em, ProcedureDetails.class, run.getPk());

        // there ought to be four history objects on the run
        assertNotNull(run.getHistories());
        assertEquals(4, run.getHistories().size());

        // also the run should be in approved status
        assertEquals(RunStatus.APPROVED, run.getRun().getStatus());

        // now undo that approval
        runApproval.setIsApproved(null);
        ApprovalsDAO.handleRunApprovalDecision(em, runApproval);

        // re-query the database for the run
        run = JPAUtils.getRecordById(em, ProcedureDetails.class, run.getPk());

        // there ought to be six history objects on the procedure
        assertNotNull(run.getHistories());
        assertEquals(6, run.getHistories().size());

        // should have a history object about the removed approval
        assertExpectedHistoryFound(run, runApproval.toString(), randomUser, 1);

        // also the run should be in REVIEWING status
        assertEquals(RunStatus.REVIEWING, run.getRun().getStatus());

        // since this was the only approval, there should also be a history object about the run being transitioned back to REVIEWING
        assertExpectedHistoryFound(run, RunStatus.REVIEWING.toString(), randomUser, 2);
    }

    private void assertExpectedHistoryFound(ProcedureDetails run, String expectedDescription, Users expectedUser, int expectedHistoryItemsCount)
    {
        SortedSet<History> history = run.getHistories().stream()
                .filter(hist -> hist.getDescription().equalsIgnoreCase(expectedDescription)).collect(Collectors.toCollection(TreeSet::new));

        // found it?
        assertNotNull(history);
        assertEquals(expectedHistoryItemsCount, history.size());
        assertEquals(expectedUser.getUserId(), history.last().getUser().getUserId());
        assertNotNull(history.last().getTimestamp());
    }

    private RunApproval assertRunHasApproverAndIsInExpectedStatus(ProcedureDetails run, Users user)
    {
        // add the user as an approver on the procedure
        RunApproval ra = ApprovalsDAO.saveNewRunApproval(em, user.getUserId(), ProcedureApprovalType.APPROVER, run.getRun().getPk(), 1);
        ra.setDueDate(new Date());
        run.getRun().setRunApprovals(new TreeSet<>(ApprovalsDAO.updateRunApprovals(em, new HashSet<>(Collections.singleton(ra)))));
        JPAUtils.closeEntityManager(em);
        em = JPAUtils.getEntityManager();

        // transition procedure to reviewing
        Users randomUser2 = DataGeneratorUtils.getRandomUser();
        Run aRun = RunDAO.transitionRunToReviewing(em, run.getRun().getPk(), randomUser2);
        em.refresh(aRun);

        // assert run is in REVIEWING status and has one approver that has a null approval decision
        assertEquals(RunStatus.REVIEWING, aRun.getStatus());
        assertEquals(1, aRun.getRunApprovals().size());
        RunApproval runApproval = aRun.getRunApprovals().iterator().next();
        assertNull(runApproval.getIsApproved());

        return runApproval;
    }
}
