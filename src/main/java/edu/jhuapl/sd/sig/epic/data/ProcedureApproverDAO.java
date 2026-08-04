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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.WebApplicationException;

import java.util.*;
import java.util.stream.Collectors;

public class ProcedureApproverDAO
{

    private static final Logger LOGGER = LogManager.getLogger();

    public static Set<ProcedureApproval> getProcedureApprovals(EntityManager em, Integer proc_def_ver_id)
    {
        return JPAUtils.getRecordById(em, ProcedureDetails.class, proc_def_ver_id).getProcedureApprovals();
    }

    public static ProcedureApproval insertNewProcedureApproval(EntityManager em, Integer user_id, ProcedureApprovalType type, Integer proc_def_ver_id)
    {
        ProcedureApproval pa;

        try
        {
            em.getTransaction().begin();

            pa = new ProcedureApproval();
            pa.setApprovalType(type);

            ProcedureDetails pdv = JPAUtils.getRecordById(em, ProcedureDetails.class, proc_def_ver_id);
            pa.setProcedureDetails(pdv);

            Users user = JPAUtils.getRecordById(em, Users.class, user_id);
            pa.setUsers(user);

            // check if exists
            Set<ProcedureApproval> pas = getProcedureApprovals(em, proc_def_ver_id);
            if (pas.contains(pa))
            {
                for (ProcedureApproval paInc : pas)
                {
                    if (paInc.equals(pa))
                    {
                        return paInc;
                    }
                }
            }

            em.persist(pa);
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            LOGGER.error("Could not save approver", e);
            throw new WebApplicationException("Could not save approver", e);
        }

        return pa;
    }

    public static boolean deleteProcedureApproval(EntityManager em, int pk, Users user)
    {
        try
        {
            em.getTransaction().begin();
            ProcedureApproval pa = JPAUtils.getRecordById(em, ProcedureApproval.class, pk);
            ProcedureDetails procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, pa.getProcedureDetails().getPk());
            em.remove(em.find(ProcedureApproval.class, pk));

            // is the procedure fully approved?
            List<ProcedureApproval> procedureApprovalsWaiting = getProcedureUnapprovedApprovals(em, procedureDetails);

            // all approvals are in; change the procedure status, set the approved date, and create a history item
            if (procedureDetails.getStatus() == ProcedureStatus.WAITING && procedureApprovalsWaiting.size() == 0)
            {
                ProcedureHeader header = procedureDetails.getProcedureHeader();

                procedureDetails.getHistories().add(new History(new Date(), ProcedureStatus.APPROVED.toString(), user, null, procedureDetails));
                procedureDetails.setStatus(ProcedureStatus.APPROVED);
                header.setApprovedDate(new Date());

                em.merge(header);
                em.merge(procedureDetails);
            }

            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            LOGGER.error("Could not delete approver", e);
            throw new WebApplicationException("Could not delete approver", e);
        }

        return true;
    }

    public static ApprovalCommentReply saveApprovalCommentReply(EntityManager em, ApprovalCommentReply newReply)
    {
        em.getTransaction().begin();
        newReply = em.merge(newReply);
        em.getTransaction().commit();

        return newReply;

    }

    public static ApprovalComment saveApprovalComment(EntityManager em, ApprovalComment newComment)
    {
        em.getTransaction().begin();
        newComment = em.merge(newComment);
        em.getTransaction().commit();
        return newComment;
    }

    public static ProcedureApproval setApprovalFlag(EntityManager em, Integer procedureApprovalId, Boolean approved)
    {
        em.getTransaction().begin();
        ProcedureApproval pa = JPAUtils.getRecordById(em, ProcedureApproval.class, procedureApprovalId);
        ProcedureDetails procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, pa.getProcedureDetails().getPk());

        pa.setIsApproved(approved);

        procedureDetails.getHistories().add(new History(new Date(), pa.toString(), pa.getUsers(), null, procedureDetails));

        pa = em.merge(pa);

        // is the procedure fully approved?
        ProcedureApproval finalPa = pa;
        // get the remaining approvals with active approver (all approvals that are null or false)
        List<ProcedureApproval> remainingApprovals = procedureDetails.getProcedureApprovals()
                .stream()
                .filter(approval ->
                {
                    if (approval.getApprovalType().equals(ProcedureApprovalType.REVIEWER))
                        return false;

                    if (approval.getPk().equals(finalPa.getPk()))
                    {
                        boolean activeApprover = finalPa.getApproverDisabled() == null || !finalPa.getApproverDisabled();
                        return activeApprover && (finalPa.getIsApproved() == null || !finalPa.getIsApproved());
                    }
                    else
                    {
                        boolean activeApprover = approval.getApproverDisabled() == null || !approval.getApproverDisabled();
                        return activeApprover && (approval.getIsApproved() == null || !approval.getIsApproved());
                    }
                })
                .collect(Collectors.toList());

        ProcedureHeader header = procedureDetails.getProcedureHeader();
        if (remainingApprovals == null || remainingApprovals.isEmpty())
        {
            // all approvals are in; change the procedure status, set the approved date, and create a history item
            procedureDetails.getHistories().add(new History(new Date(), ProcedureStatus.APPROVED.toString(), pa.getUsers(), null, procedureDetails));
            procedureDetails.setStatus(ProcedureStatus.APPROVED);

            header.setApprovedDate(new Date());
        }
        else if (procedureDetails.getStatus().equals(ProcedureStatus.APPROVED))
        {
            // if here, procedure WAS approved, but someone walked back their approval decision. Put status
            // back to WAITING, null out the approved date on the header, and create history object
            procedureDetails.getHistories().add(new History(new Date(), ProcedureStatus.WAITING.toString(), pa.getUsers(), null, procedureDetails));
            procedureDetails.setStatus(ProcedureStatus.WAITING);

            header.setApprovedDate(null);
        }
        em.merge(header);

        em.merge(procedureDetails);

        em.getTransaction().commit();

        return pa;
    }

    public static ProcedureApproval setDisabledFlag(EntityManager em, Integer procedureApprovalId, Boolean approverDisabled,
            Users user)
    {

        em.getTransaction().begin();
        ProcedureApproval pa = JPAUtils.getRecordById(em, ProcedureApproval.class, procedureApprovalId);
        pa.setApproverDisabled(approverDisabled);
        pa = em.merge(pa);

        // is the procedure fully approved?
        List<ProcedureApproval> procedureApprovalsWaiting = getProcedureUnapprovedApprovals(em, pa.getProcedureDetails());

        // all approvals are in; change the procedure status, set the approved date, and create a history item
        if (pa.getProcedureDetails().getStatus() == ProcedureStatus.WAITING && procedureApprovalsWaiting.size() == 0)
        {
            ProcedureDetails procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, pa.getProcedureDetails().getPk());
            ProcedureHeader header = procedureDetails.getProcedureHeader();

            //Use person who set the approver to disabled
            procedureDetails.getHistories().add(new History(new Date(), ProcedureStatus.APPROVED.toString(), user, null, procedureDetails));
            procedureDetails.setStatus(ProcedureStatus.APPROVED);
            header.setApprovedDate(new Date());

            em.merge(header);
            em.merge(procedureDetails);
        }

        em.getTransaction().commit();

        return pa;
    }

    public static Set<ProcedureApproval> getUnapprovedApprovals(EntityManager em)
    {
        String q = "SELECT pa FROM ProcedureApproval pa WHERE " +
                "(pa.approverDisabled IS NULL OR pa.approverDisabled = false ) AND " + // make sure approver is active
                "pa.isApproved = null AND pa.procedureDetails.status = :waiting";
        TypedQuery<ProcedureApproval> tq = em.createQuery(q, ProcedureApproval.class);
        tq.setParameter("waiting", ProcedureStatus.WAITING);
        return new HashSet<>(tq.getResultList());
    }

    public static List<ProcedureApproval> getProcedureUnapprovedApprovals(EntityManager em, ProcedureDetails procedureDetails)
    {
        String q = "SELECT pa FROM ProcedureApproval pa WHERE " +
                "(pa.approverDisabled IS NULL OR pa.approverDisabled = false ) AND " + // make sure approver is active
                "pa.isApproved = null AND pa.procedureDetails.status = :waiting" +
                " AND pa.procedureDetails.pk = :procedureDetailsPK AND pa.approvalType = :approverType";
        TypedQuery<ProcedureApproval> tq = em.createQuery(q, ProcedureApproval.class);
        tq.setParameter("waiting", ProcedureStatus.WAITING);
        tq.setParameter("procedureDetailsPK", procedureDetails.getPk());
        tq.setParameter("approverType", ProcedureApprovalType.APPROVER);
        return tq.getResultList();
    }
}
