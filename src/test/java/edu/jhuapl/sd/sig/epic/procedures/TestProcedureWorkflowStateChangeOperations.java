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

import edu.jhuapl.sd.sig.epic.data.ProcedureApproverDAO;
import edu.jhuapl.sd.sig.epic.data.ProcedureDetailsDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.*;
import edu.jhuapl.sd.sig.epic.utils.DataGeneratorUtils;
import edu.jhuapl.sd.sig.epic.utils.TestUtils;
import org.junit.jupiter.api.*;

import javax.persistence.EntityManager;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TestProcedureWorkflowStateChangeOperations
{
    private static EntityManager em = null;
    private static ProcedureDetails procedureDetails = null;

    @BeforeAll
    public static void beforeClass()
    {
        TestUtils.init();
        em = JPAUtils.getEntityManager();
    }

    @AfterAll
    public static void afterClass()
    {
        JPAUtils.closeEntityManager(em);
    }

    @BeforeEach
    public void before()
    {
        try
        {
            JPAUtils.closeEntityManager(em);
            em = JPAUtils.getEntityManager();
            procedureDetails = TestUtils.createDraftProcedureDetails(em);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Test
    public void testProcedureCreationGeneratesHistory()
    {
        assertNotNull(procedureDetails);
        assertNotNull(procedureDetails.getHistories());
        assertEquals(1, procedureDetails.getHistories().size());

        History history = procedureDetails.getHistories().first();
        assertEquals("Added " + procedureDetails.toString(), history.getDescription());
        assertEquals(procedureDetails.getProcedureHeader().getUser().getUserId(), history.getUser().getUserId());
        assertNotNull(history.getTimestamp());
    }

    @Test
    public void testProcedureTransitionToWaitingGeneratesHistory()
    {
        assertNotNull(procedureDetails);
        Users randomUser = DataGeneratorUtils.getRandomUser();
        procedureDetails = ProcedureDetailsDAO.transitionToWaiting(em, procedureDetails.getPk(), new Date().getTime(), randomUser);

        // assert is in waiting status
        assertEquals(ProcedureStatus.WAITING, procedureDetails.getStatus());

        // there ought to be two history objects
        assertNotNull(procedureDetails.getHistories());
        assertEquals(2, procedureDetails.getHistories().size());

        List<History> history = procedureDetails.getHistories().stream()
                .filter(hist -> hist.getDescription().equalsIgnoreCase(ProcedureStatus.WAITING.toString()))
                .collect(Collectors.toList());

        // found it?
        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals(randomUser.getUserId(), history.get(0).getUser().getUserId());
        assertNotNull(history.get(0).getTimestamp());

        // also assert data on procedure header is not null
        assertNotNull(procedureDetails.getProcedureHeader().getSubmittedForReviewDate());
    }

    @Test
    public void testProcedureReturnToDraftGeneratesHistory()
    {
        assertNotNull(procedureDetails);
        Users randomUser = DataGeneratorUtils.getRandomUser();
        procedureDetails = ProcedureDetailsDAO.transitionToWaiting(em, procedureDetails.getPk(), new Date().getTime(), randomUser);
        procedureDetails = ProcedureDetailsDAO.returnToDraft(em, procedureDetails.getPk(), randomUser);

        // assert is in DRAFT status
        assertEquals(ProcedureStatus.DRAFT, procedureDetails.getStatus());

        // there ought to be three history objects
        assertNotNull(procedureDetails.getHistories());
        assertEquals(3, procedureDetails.getHistories().size());

        List<History> history = procedureDetails.getHistories().stream()
                .filter(hist -> hist.getDescription().equalsIgnoreCase(ProcedureStatus.DRAFT.toString()))
                .collect(Collectors.toList());

        // found it? Should only be one in the list
        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals(randomUser.getUserId(), history.get(0).getUser().getUserId());
        assertNotNull(history.get(0).getTimestamp());

        // the dates on procedure header should be nulled
        assertNull(procedureDetails.getProcedureHeader().getSubmittedForReviewDate());
        assertNull(procedureDetails.getProcedureHeader().getApprovedDate());
        assertNull(procedureDetails.getProcedureHeader().getReleasedDate());
    }

    @Test
    public void testProcedureReleaseGeneratesHistory()
    {
        assertNotNull(procedureDetails);
        Users randomUser = DataGeneratorUtils.getRandomUser();
        procedureDetails = ProcedureDetailsDAO.transitionToWaiting(em, procedureDetails.getPk(), new Date().getTime(), randomUser);
        procedureDetails = ProcedureDetailsDAO.transitionToReadyForRelease(em, procedureDetails.getPk(), randomUser);

        // assert is in READY status
        assertEquals(ProcedureStatus.READY, procedureDetails.getStatus());

        // there ought to be three history objects
        assertNotNull(procedureDetails.getHistories());
        assertEquals(3, procedureDetails.getHistories().size());

        List<History> history = procedureDetails.getHistories().stream()
                .filter(hist -> hist.getDescription().equalsIgnoreCase(ProcedureStatus.READY.toString()))
                .collect(Collectors.toList());

        // found it? Should only be one in the list
        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals(randomUser.getUserId(), history.get(0).getUser().getUserId());
        assertNotNull(history.get(0).getTimestamp());

        assertNotNull(procedureDetails.getProcedureHeader().getSubmittedForReviewDate());
        assertNull(procedureDetails.getProcedureHeader().getApprovedDate());
        assertNotNull(procedureDetails.getProcedureHeader().getReleasedDate());
    }

    @Test
    public void testNewRevisionCreationGeneratesHistory()
    {
        assertNotNull(procedureDetails);
        Users randomUser = DataGeneratorUtils.getRandomUser();
        procedureDetails = ProcedureDetailsDAO.transitionToReadyForRelease(em, procedureDetails.getPk(), randomUser);

        // assert old revision is in READY status
        assertEquals(ProcedureStatus.READY, procedureDetails.getStatus());

        ProcedureDetails newRev = ProcedureDetailsDAO.createRevision(em, procedureDetails.getId(), JPAUtils.getRecordById(em, Users.class, randomUser.getUserId()));

        // assert new revision is in DRAFT status
        assertEquals(ProcedureStatus.DRAFT, newRev.getStatus());

        // there ought to be three history objects on the original revision
        assertNotNull(procedureDetails.getHistories());
        assertEquals(3, procedureDetails.getHistories().size());

        // the original revision should have a history object about the new revision creation
        List<History> history = procedureDetails.getHistories().stream()
                .filter(hist -> hist.getDescription().equalsIgnoreCase("Added " + newRev.toString()))
                .collect(Collectors.toList());

        // found it? Should only be one in the list
        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals(randomUser.getUserId(), history.get(0).getUser().getUserId());
        assertNotNull(history.get(0).getTimestamp());

        // there should be one history object in the new revision
        assertNotNull(newRev.getHistories());
        assertEquals(1, newRev.getHistories().size());

        // the new revision should have a history object about the new revision creation
        history = newRev.getHistories().stream()
                .filter(hist -> hist.getDescription().equalsIgnoreCase("Added " + newRev.toString()))
                .collect(Collectors.toList());

        // found it? Should only be one in the list
        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals(randomUser.getUserId(), history.get(0).getUser().getUserId());
        assertNotNull(history.get(0).getTimestamp());
    }

    @Test
    public void testProcedureApprovalGeneratesHistory()
    {
        assertNotNull(procedureDetails);
        Users randomUser = DataGeneratorUtils.getRandomUser();

        // add the user as an approver on the procedure
        ProcedureApproverDAO.insertNewProcedureApproval(em, randomUser.getUserId(), ProcedureApprovalType.APPROVER, procedureDetails.getPk());

        // transition procedure to waiting
        Users randomUser2 = DataGeneratorUtils.getRandomUser();
        procedureDetails = ProcedureDetailsDAO.transitionToWaiting(em, procedureDetails.getPk(), new Date().getTime(), randomUser2);
        em.refresh(procedureDetails);

        // assert procedure is waiting status and has one approver that has a null approval decision
        assertEquals(ProcedureStatus.WAITING, procedureDetails.getStatus());
        assertEquals(1, procedureDetails.getProcedureApprovals().size());
        ProcedureApproval pa = procedureDetails.getProcedureApprovals().iterator().next();
        assertNull(pa.getIsApproved());

        // set the approval equal to true
        pa = ProcedureApproverDAO.setApprovalFlag(em, pa.getPk(), true);

        // re-query the database for the procedure details
        procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDetails.getPk());

        // there ought to be four history objects on the procedure
        assertNotNull(procedureDetails.getHistories());
        assertEquals(4, procedureDetails.getHistories().size());

        // should have a history object about the approval
        ProcedureApproval finalPa = pa;
        List<History> history = procedureDetails.getHistories().stream()
                .filter(hist -> hist.getDescription().equalsIgnoreCase(finalPa.toString()))
                .collect(Collectors.toList());

        // found it? Should only be one in the list
        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals(randomUser.getUserId(), history.get(0).getUser().getUserId());
        assertNotNull(history.get(0).getTimestamp());

        // since this was the only approval, there should also be a history object about the procedure being fully approved
        history = procedureDetails.getHistories().stream()
                .filter(hist -> hist.getDescription().equalsIgnoreCase(ProcedureStatus.APPROVED.toString()))
                .collect(Collectors.toList());

        // found it? Should only be one in the list
        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals(randomUser.getUserId(), history.get(0).getUser().getUserId());
        assertNotNull(history.get(0).getTimestamp());

        // also the procedure should be in approved status
        assertEquals(ProcedureStatus.APPROVED, procedureDetails.getStatus());

        // all dates except release date should be not null
        assertNotNull(procedureDetails.getProcedureHeader().getSubmittedForReviewDate());
        assertNotNull(procedureDetails.getProcedureHeader().getApprovedDate());
        assertNull(procedureDetails.getProcedureHeader().getReleasedDate());
    }

    @Test
    public void testProcedureRejectionGeneratesHistory()
    {
        assertNotNull(procedureDetails);
        Users randomUser = DataGeneratorUtils.getRandomUser();

        // add the user as an approver on the procedure
        ProcedureApproverDAO.insertNewProcedureApproval(em, randomUser.getUserId(), ProcedureApprovalType.APPROVER, procedureDetails.getPk());

        // transition procedure to waiting
        Users randomUser2 = DataGeneratorUtils.getRandomUser();
        procedureDetails = ProcedureDetailsDAO.transitionToWaiting(em, procedureDetails.getPk(), new Date().getTime(), randomUser2);
        em.refresh(procedureDetails);

        // assert procedure is waiting status and has one approver that has a null approval decision
        assertEquals(ProcedureStatus.WAITING, procedureDetails.getStatus());
        assertEquals(1, procedureDetails.getProcedureApprovals().size());
        ProcedureApproval pa = procedureDetails.getProcedureApprovals().iterator().next();
        assertNull(pa.getIsApproved());

        // set the approval equal to false
        pa = ProcedureApproverDAO.setApprovalFlag(em, pa.getPk(), false);

        // re-query the database for the procedure details
        procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDetails.getPk());

        // there ought to be three history objects on the procedure
        assertNotNull(procedureDetails.getHistories());
        assertEquals(3, procedureDetails.getHistories().size());

        // should have a history object about the approval
        ProcedureApproval finalPa = pa;
        List<History> history = procedureDetails.getHistories().stream()
                .filter(hist -> hist.getDescription().equalsIgnoreCase(finalPa.toString()))
                .collect(Collectors.toList());

        // found it? Should only be one in the list
        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals(randomUser.getUserId(), history.get(0).getUser().getUserId());
        assertNotNull(history.get(0).getTimestamp());

        assertNotNull(procedureDetails.getProcedureHeader().getSubmittedForReviewDate());
        assertNull(procedureDetails.getProcedureHeader().getApprovedDate());
        assertNull(procedureDetails.getProcedureHeader().getReleasedDate());
    }

    @Test
    public void testProcedureDecisionRemovalGeneratesHistory()
    {
        assertNotNull(procedureDetails);
        Users randomUser = DataGeneratorUtils.getRandomUser();

        // add the user as an approver on the procedure
        ProcedureApproverDAO.insertNewProcedureApproval(em, randomUser.getUserId(), ProcedureApprovalType.APPROVER, procedureDetails.getPk());

        // transition procedure to waiting
        Users randomUser2 = DataGeneratorUtils.getRandomUser();
        procedureDetails = ProcedureDetailsDAO.transitionToWaiting(em, procedureDetails.getPk(), new Date().getTime(), randomUser2);
        em.refresh(procedureDetails);

        // assert procedure is waiting status and has one approver that has a null approval decision
        assertEquals(ProcedureStatus.WAITING, procedureDetails.getStatus());
        assertEquals(1, procedureDetails.getProcedureApprovals().size());
        ProcedureApproval pa = procedureDetails.getProcedureApprovals().iterator().next();
        assertNull(pa.getIsApproved());

        // set the approval equal to true
        pa = ProcedureApproverDAO.setApprovalFlag(em, pa.getPk(), true);

        // re-query the database for the procedure details
        procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDetails.getPk());

        // there ought to be four history objects on the procedure
        assertNotNull(procedureDetails.getHistories());
        assertEquals(4, procedureDetails.getHistories().size());

        // should have a history object about the approval
        ProcedureApproval finalPa = pa;
        List<History> history = procedureDetails.getHistories().stream()
                .filter(hist -> hist.getDescription().equalsIgnoreCase(finalPa.toString()))
                .collect(Collectors.toList());

        // found it? Should only be one in the list
        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals(randomUser.getUserId(), history.get(0).getUser().getUserId());
        assertNotNull(history.get(0).getTimestamp());

        // since this was the only approval, there should also be a history object about the procedure being fully approved
        history = procedureDetails.getHistories().stream()
                .filter(hist -> hist.getDescription().equalsIgnoreCase(ProcedureStatus.APPROVED.toString()))
                .collect(Collectors.toList());

        // found it? Should only be one in the list
        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals(randomUser.getUserId(), history.get(0).getUser().getUserId());
        assertNotNull(history.get(0).getTimestamp());

        // also the procedure should be in approved status
        assertEquals(ProcedureStatus.APPROVED, procedureDetails.getStatus());

        // all dates except release date should be not null
        assertNotNull(procedureDetails.getProcedureHeader().getSubmittedForReviewDate());
        assertNotNull(procedureDetails.getProcedureHeader().getApprovedDate());
        assertNull(procedureDetails.getProcedureHeader().getReleasedDate());

        // now undo that approval
        pa = ProcedureApproverDAO.setApprovalFlag(em, pa.getPk(), null);

        // re-query the database for the procedure details
        procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDetails.getPk());

        // there ought to be six history objects on the procedure
        assertNotNull(procedureDetails.getHistories());
        assertEquals(6, procedureDetails.getHistories().size());

        // should have a history object about the removed approval
        ProcedureApproval finalPa1 = pa;
        history = procedureDetails.getHistories().stream()
                .filter(hist -> hist.getDescription().equalsIgnoreCase(finalPa1.toString()))
                .collect(Collectors.toList());

        // found it? Should only be one in the list
        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals(randomUser.getUserId(), history.get(0).getUser().getUserId());
        assertNotNull(history.get(0).getTimestamp());

        // since this was the only approval, there should also be a history object about the procedure being transitioned back to waiting
        SortedSet<History> history1 = new TreeSet<>(procedureDetails.getHistories().stream()
                .filter(hist -> hist.getDescription().equalsIgnoreCase(ProcedureStatus.WAITING.toString()))
                .collect(Collectors.toSet()));

        // found it? Should only be one in the list
        assertNotNull(history1);
        assertEquals(2, history1.size());
        assertEquals(randomUser.getUserId(), history1.last().getUser().getUserId());
        assertNotNull(history1.last().getTimestamp());

        // also the procedure should be in waiting status
        assertEquals(ProcedureStatus.WAITING, procedureDetails.getStatus());

        // only submitted date should be not null
        assertNotNull(procedureDetails.getProcedureHeader().getSubmittedForReviewDate());
        assertNull(procedureDetails.getProcedureHeader().getApprovedDate());
        assertNull(procedureDetails.getProcedureHeader().getReleasedDate());
    }
}
