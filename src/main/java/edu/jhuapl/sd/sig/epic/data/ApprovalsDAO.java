/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.data;

import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.*;
import edu.jhuapl.sd.sig.epic.model.display.dto.ProcApprovalDashboardDTO;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.WebApplicationException;

import edu.jhuapl.sd.sig.epic.model.display.dto.RunApprovalDashboardDTO;
import edu.jhuapl.sd.sig.epic.startup.EmailEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class ApprovalsDAO
{

    private static final Logger LOGGER = LogManager.getLogger();

    public static List<ProcApprovalDashboardDTO> getProcedureApprovalList(EntityManager em, Integer userid)
    {
        List<ProcApprovalDashboardDTO> approvals = null;

        String qString = "SELECT new edu.jhuapl.sd.sig.epic.model.display.dto.ProcApprovalDashboardDTO(pd.id, pa.pk, pa.approvalType, pd.procedureDef.name,  pd.status ) FROM ProcedureApproval pa JOIN pa.procedureDetails pd WHERE pa.users.userId = :user_id "
                +
                "AND pa.procedureDetails.status = 'WAITING' " +
                "AND pa.isApproved IS NULL ";

        TypedQuery<ProcApprovalDashboardDTO> q = em.createQuery(qString, ProcApprovalDashboardDTO.class);
        q.setParameter("user_id", userid);
        approvals = q.getResultList();

        return approvals;
    }

    public static List<RunApprovalDashboardDTO> getRunApprovalList(EntityManager em, Integer userId)
    {
        List<RunApprovalDashboardDTO> approvals;

        String qString = "SELECT new edu.jhuapl.sd.sig.epic.model.display.dto.RunApprovalDashboardDTO(pd.id, " +
                "ra.pk, ra.approvalType, ra.run.name,  ra.run.status ) FROM RunApproval ra " +
                "JOIN ra.run.procedureDetails pd WHERE ra.users.userId = :userId " +
                "AND ra.run.status = :status " +
                "AND (ra.isApproved IS NULL OR ra.isApproved = false)";

        TypedQuery<RunApprovalDashboardDTO> q = em.createQuery(qString, RunApprovalDashboardDTO.class);
        q.setParameter("userId", userId);
        q.setParameter("status", RunStatus.REVIEWING);
        approvals = q.getResultList();

        return approvals;
    }

    public static void updateApproval(EntityManager em, ProcedureApproval approval)
    {
        try
        {
            em.getTransaction().begin();
            em.merge(approval);
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String msg = "Error updating Approval with PK of: " + approval.getPk();
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
    }

    public static RunApproval saveNewRunApproval(EntityManager em, Integer userId, ProcedureApprovalType type, Integer runPk,
            Integer approverOrder)
    {
        RunApproval runApproval = null;
        try
        {
            em.getTransaction().begin();
            runApproval = new RunApproval();
            runApproval.setApprovalType(type);
            runApproval.setApproverOrder(approverOrder);

            Run run = JPAUtils.getRecordById(em, Run.class, runPk);
            runApproval.setRun(run);

            Users user = JPAUtils.getRecordById(em, Users.class, userId);
            runApproval.setUsers(user);

            Set<RunApproval> runApprovals = run.getRunApprovals();
            if (runApprovals != null && runApprovals.contains(runApproval))
            {
                for (RunApproval ra : runApprovals)
                {
                    if (ra.equals(runApproval))
                    {
                        return ra;
                    }
                }
            }

            em.persist(runApproval);
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String message = "Error while saving new run approval";
            LOGGER.error(message, e);
            throw new WebApplicationException(message, e);
        }
        return runApproval;
    }

    public static SortedSet<RunApproval> deleteRunApproval(EntityManager em, RunApproval runApproval)
    {
        SortedSet<RunApproval> runApprovals = null;
        try
        {
            em.getTransaction().begin();
            em.remove(runApproval);

            // need to update the approver ordering
            runApprovals = runApproval.getRun().getRunApprovals();
            runApprovals.remove(runApproval);
            Iterator<RunApproval> iterator = runApprovals.iterator();
            int orderNumber = 1;
            while (iterator.hasNext())
            {
                RunApproval remainingApproval = iterator.next();
                remainingApproval.setApproverOrder(orderNumber++);
                remainingApproval = em.merge(remainingApproval);
            }

            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String message = "Error while deleting run approval";
            LOGGER.error(message, e);
            throw new WebApplicationException(message, e);
        }
        return runApprovals;
    }

    public static Set<RunApproval> updateRunApprovals(EntityManager em, Set<RunApproval> runApprovals)
    {
        try
        {
            em.getTransaction().begin();
            runApprovals.forEach(runApproval ->
            {
                RunApproval ra = JPAUtils.getRecordById(em, RunApproval.class, runApproval.getPk());
                ra.setDueDate(runApproval.getDueDate());
                ra.setApproverOrder(runApproval.getApproverOrder());
                ra.setLastReminderDate(runApproval.getLastReminderDate());
                runApproval = em.merge(ra);
            });
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String message = "Error while updating run approval";
            LOGGER.error(message, e);
            throw new WebApplicationException(message, e);
        }
        return runApprovals;
    }

    public static Run handleRunApprovalDecision(EntityManager em, RunApproval runApprovalData)
    {
        Run run = null;
        try
        {
            em.getTransaction().begin();
            RunApproval runApproval = JPAUtils.getRecordById(em, RunApproval.class, runApprovalData.getPk());
            run = JPAUtils.getRecordById(em, Run.class, runApproval.getRun().getPk());
            ProcedureDetails runProcedureDetails = run.getProcedureDetails();

            runApproval.setIsApproved(runApprovalData.getIsApproved());
            runProcedureDetails.getHistories().add(new History(new Date(), runApproval.toString(), runApproval.getUsers(), null, runProcedureDetails));

            runApproval = em.merge(runApproval);

            if (runApproval.getIsApproved() != null && runApproval.getIsApproved())
            {
                // find out who has left to approve
                SortedSet<RunApproval> remainingApprovers = run.getRunApprovals()
                        .stream()
                        .filter(approval ->
                        {
                            boolean activeApprover = approval.getApproverDisabled() == null || !approval.getApproverDisabled();
                            return activeApprover && (approval.getIsApproved() == null || !approval.getIsApproved());
                        }).collect(Collectors.toCollection(TreeSet::new));

                if (remainingApprovers.isEmpty())
                {
                    // if here, all approvers have approved. Mark the run as approved and sent an email.
                    runProcedureDetails.getHistories().add(new History(new Date(), RunStatus.APPROVED.toString(), runApproval.getUsers(), null, runProcedureDetails));
                    run.setStatus(RunStatus.APPROVED);
                    Users runCloseoutUser = run.getCloseoutSubmissionUser();
                    if (runCloseoutUser != null)
                    {
                        EmailEngine.getInstance().sendMessage(MessageType.NOTIFICATION_RUN_CLOSEOUT_FULLY_APPROVED, runCloseoutUser, runApproval);
                    }
                    else
                    {
                        LOGGER.warn("All approvers have approved run. Unable to send email - No closeout user found for run: " + run.getName());
                    }

                }
                else
                {
                    // if here, remainingApprovers is not empty and should be in sorted order. Email
                    // the first person in the list.

                    // only send if the approver hasn't previously been emailed.
                    if (remainingApprovers.first().getLastReminderDate() == null)
                    {
                        EmailEngine.getInstance().sendMessage(MessageType.NOTIFICATION_RUN_CLOSEOUT_WAITING, remainingApprovers.first());

                    }
                }
            }
            else if (runApproval.getIsApproved() != null && !runApproval.getIsApproved())
            {
                // if here, approver did not approve; transition run to CORRECTING status and email run closeout submitter
                runProcedureDetails.getHistories().add(new History(new Date(), RunStatus.CORRECTING.toString(), runApproval.getUsers(), null, runProcedureDetails));
                run.setStatus(RunStatus.CORRECTING);
                Users runCloseoutUser = run.getCloseoutSubmissionUser();
                if (runCloseoutUser != null)
                {
                    EmailEngine.getInstance().sendMessage(MessageType.NOTIFICATION_RUN_CLOSEOUT_CORRECTIONS_NEEDED, runCloseoutUser, runApproval);
                }
                else
                {
                    LOGGER.warn("Run set to CORRECTING. Unable to send email - No closeout user found for run: " + run.getName());
                }

            }
            else
            {
                // if here, decision was walked back
                // if the procedure was approved, set back to in REVIEWING status
                if (run.getStatus().equals(RunStatus.APPROVED))
                {
                    run.setStatus(RunStatus.REVIEWING);
                    runProcedureDetails.getHistories().add(new History(new Date(), RunStatus.REVIEWING.toString(), runApproval.getUsers(), null, runProcedureDetails));
                }
            }

            // save the run and add the saved approval to it.
            runProcedureDetails = em.merge(runProcedureDetails);
            run.setProcedureDetails(runProcedureDetails);
            run = em.merge(run);

            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String message = "Could not record approver's decision for approval with pk = " + runApprovalData.getPk();
            LOGGER.error(message, e);
            throw new WebApplicationException(message, e);
        }
        return run;
    }
}
