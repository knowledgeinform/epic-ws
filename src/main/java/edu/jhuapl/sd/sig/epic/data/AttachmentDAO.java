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
import javax.ws.rs.WebApplicationException;

public class AttachmentDAO
{

    private static final Logger LOGGER = LogManager.getLogger();

    public static Attachment saveAttachment(EntityManager em, Attachment attachment, RedLineComment redLineComment, Users user)
    {
        try
        {
            em.getTransaction().begin();

            attachment = em.merge(attachment);

            if (redLineComment != null)
            {
                // all comment fields should have been set in the front end or endpoint method.
                // call method to handle updating the entities on the comment.
                em.persist(handleUpdatingRedLineCommentEntities(em, redLineComment));
            }

            if (attachment instanceof RunStepAttachment)
            {
                // if this is a run step attachment, a history needs to be created.
                StepDef step = generateHistoryForRunStepAttachment(em, null, attachment, (RunStepAttachment) attachment, user);
                step.getRunStepAttachments().add((RunStepAttachment) attachment);
                em.merge(step);
            }
            else if (attachment instanceof RunAttachment)
            {
                Run run = generateHistoryForRunAttachment(em, null, attachment, (RunAttachment) attachment, user);
                run.getAttachments().add((RunAttachment) attachment);
                em.merge(run);
            }

            em.getTransaction().commit();
            return attachment;
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String msg = "Could not save the attachment record";
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
    }

    private static StepDef generateHistoryForRunStepAttachment(EntityManager em, Attachment oldAttachment,
            Attachment newAttachment, RunStepAttachment rsa, Users user)
    {
        History history = new History(oldAttachment, newAttachment, user);
        StepDef step = rsa.getStepDef();
        history.setStepDef(step);
        em.persist(history);
        step.getHistories().add(history);
        return step;
    }

    private static Run generateHistoryForRunAttachment(EntityManager em, Attachment oldAttachment,
            Attachment newAttachment, RunAttachment ra, Users user)
    {
        History history = new History(oldAttachment, newAttachment, user);
        Run run = ra.getRun();
        history.setProcedureDetails(run.getProcedureDetails());
        em.persist(history);
        run.getProcedureDetails().getHistories().add(history);
        return run;
    }

    public static void deleteAttachment(EntityManager em, Attachment attachment, Users user)
    {
        try
        {
            em.getTransaction().begin();
            if (attachment instanceof StepDefAttachment)
            {
                StepDef sd = ((StepDefAttachment) attachment).getStepDef();
                sd.getStepDefAttachments().removeIf(stepDefAttachment -> attachment.getPk().equals(stepDefAttachment.getPk()));
                em.merge(sd);
            }
            else if (attachment instanceof RunStepAttachment)
            {
                // create a history
                StepDef sd = generateHistoryForRunStepAttachment(em, attachment, null, (RunStepAttachment) attachment, user);
                sd.getRunStepAttachments().removeIf(runStepAttachment -> attachment.getPk().equals(runStepAttachment.getPk()));
                em.merge(sd);
            }
            else if (attachment instanceof ProcedureAttachment)
            {
                ProcedureHeader ph = ((ProcedureAttachment) attachment).getProcedureHeader();
                ph.getAttachments().removeIf(procedureAttachment -> attachment.getPk().equals(procedureAttachment.getPk()));
                em.merge(ph);
            }
            else if (attachment instanceof RunAttachment)
            {
                Run r = generateHistoryForRunAttachment(em, attachment, null, (RunAttachment) attachment, user);
                r.getAttachments().removeIf(runAttachment -> attachment.getPk().equals(runAttachment.getPk()));
                em.merge(r);
            }
            else
            {
                throw new RuntimeException("Invalid Attachment Class: " + attachment.getClass());
            }
            em.remove(attachment);
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String msg = "Could not delete the attachment record";
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
    }

    /**
     * This method handles redline deletion of attachments. Note that the attachments do not actually
     * get deleted at this time; instead when a revision is created from a redlined run, the attachments
     * marked EditType.REDLINE_DELETE will not be copied over. Note that this method also saves a red line
     * comment for the action.
     * 
     * @param em
     * @param attachment
     * @param redLineComment
     */
    public static void redlineDeleteAttachment(EntityManager em, Attachment attachment, RedLineComment redLineComment)
    {
        try
        {
            em.getTransaction().begin();

            // just change the edit type of the attachment and save.
            attachment.setEditType(EditType.REDLINE_DELETE);
            em.merge(attachment);

            // handle updating the entities on the comment and save the comment if not null
            if (redLineComment != null)
            {
                em.persist(handleUpdatingRedLineCommentEntities(em, redLineComment));
            }

            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String msg = "Could not mark the attachment record as a red line deletion";
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
    }

    private static RedLineComment handleUpdatingRedLineCommentEntities(EntityManager em, RedLineComment redLineComment)
    {
        // because this may be the first redline to a run, we need to make sure that the procedureDetails
        // is set to EditType.REDLINE_EDIT.
        if (!redLineComment.getProcedureDetails().getEditType().equals(EditType.REDLINE_EDIT))
        {
            ProcedureDetails redLinedRun = JPAUtils.getRecordById(em, ProcedureDetails.class, redLineComment.getProcedureDetails().getPk());
            redLinedRun = RedlineDAO.markThisRunAsRedlined(em, redLinedRun);
            redLinedRun = em.merge(redLinedRun);
            redLineComment.setProcedureDetails(redLinedRun);
        }

        // if this is a step attachment, that step should be marked as EditType.REDLINE_EDIT unless it is a REDLINE_ADD already
        if (redLineComment.getStepDef() != null)
        {
            if (!redLineComment.getStepDef().getEditType().equals(EditType.REDLINE_ADD))
            {
                // if here, need to update the step to REDLINE_EDIT and save it.
                StepDef redlinedStep = JPAUtils.getRecordById(em, StepDef.class, redLineComment.getStepDef().getPk());
                redlinedStep.setEditType(EditType.REDLINE_EDIT);
                redlinedStep = em.merge(redlinedStep);
                redLineComment.setStepDef(redlinedStep);
            }
        }

        // get the redline comment's user; this is prevent a transient value error in Hibernate.
        Users user = UsersDAO.getUserByUsername(em, redLineComment.getUsers().getUsername());
        redLineComment.setUsers(user);

        return redLineComment;
    }
}
