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
import edu.jhuapl.sd.sig.epic.resource.model.RedlineData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;

import javax.persistence.EntityManager;
import javax.ws.rs.WebApplicationException;
import java.util.*;

public class RedlineDAO
{
    private static final Logger LOGGER = LogManager.getLogger();

    public static ProcedureInstruction saveSingleProcedureInstructionRedLine(EntityManager em, ProcedureInstruction redlinedInstruction,
            RedLineComment redLineComment, ProcedureDetails redlinedRun)
    {
        ProcedureInstruction updatedInstruction = null;

        if (redlinedInstruction.getPk() != null)
        {
            updatedInstruction = JPAUtils.getRecordById(em, ProcedureInstruction.class, redlinedInstruction.getPk());
        }

        try
        {
            em.getTransaction().begin();

            // make sure the original procedure details redlinedVersion points to this run's ID
            redlinedRun = markThisRunAsRedlined(em, redlinedRun);

            if (updatedInstruction == null)
            {
                updatedInstruction = new ProcedureInstruction();
            }

            // update the instruction
            updatedInstruction.setEditType(redlinedInstruction.getEditType());
            updatedInstruction.setText(redlinedInstruction.getText());
            updatedInstruction.setSectionName(redlinedInstruction.getSectionName());
            updatedInstruction.setDisplayOrder(redlinedInstruction.getDisplayOrder());

            if (updatedInstruction.getProcedureDetails() == null)
            {
                updatedInstruction.setProcedureDetails(redlinedRun);
                em.persist(updatedInstruction);
            }
            else
            {
                em.merge(updatedInstruction);
            }

            // now save the redline comment
            redLineComment.setProcedureDetails(redlinedRun);
            redLineComment.setProcedureInstruction(updatedInstruction);
            em.persist(redLineComment);

            List<RedLineComment> comments = updatedInstruction.getRedLineComments();
            if (comments == null)
            {
                comments = new ArrayList<>();
            }
            comments.add(redLineComment);
            updatedInstruction.setRedLineComments(comments);

            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String msg = "Error saving red line for instruction with id of: " + redlinedInstruction.getPk();
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
        return updatedInstruction;
    }

    public static List<StepGroupDef> saveStepGroupRedLine(EntityManager em, List<RedlineData> redlineData, ProcedureDetails redlinedRun)
    {
        List<StepGroupDef> updatedStepGroups = new ArrayList<>();
        try
        {
            em.getTransaction().begin();

            for (RedlineData redline : redlineData)
            {
                StepGroupDef redlinedStepGroup = null;

                if (redline.getStepGroupDef().getPk() != null)
                {
                    redlinedStepGroup = JPAUtils.getRecordById(em, StepGroupDef.class, redline.getStepGroupDef().getPk());
                }
                // make sure the original procedure details redlinedVersion points to this run's ID
                redlinedRun = markThisRunAsRedlined(em, redlinedRun);

                if (redlinedStepGroup == null)
                {
                    redlinedStepGroup = new StepGroupDef();
                }

                // update the step group
                redlinedStepGroup.setEditType(redline.getStepGroupDef().getEditType());
                redlinedStepGroup.setStepGroupName(redline.getStepGroupDef().getStepGroupName());
                redlinedStepGroup.setDisplayOrder(redline.getStepGroupDef().getDisplayOrder());
                redlinedStepGroup.setDescription(redline.getStepGroupDef().getDescription());

                if (redline.getStepGroupDef().getStepGroupDefParent() != null)
                {
                    // if here, has a step group parent (not a top level group)
                    redlinedStepGroup.setStepGroupDefParent(JPAUtils.getRecordById(em, StepGroupDef.class,
                            redline.getStepGroupDef().getStepGroupDefParent().getPk()));
                    redlinedStepGroup.setProcedureDetails(null);
                }
                else
                {
                    // if here, is a top level group with no step group parent
                    redlinedStepGroup.setStepGroupDefParent(null);
                    redlinedStepGroup.setProcedureDetails(redlinedRun);
                }

                // next - has this group been redline deleted? If so, everything in the group should also have the
                // editType changed.
                if (redlinedStepGroup.getEditType().equals(EditType.REDLINE_DELETE))
                {
                    Set<StepDef> steps = redlinedStepGroup.getAllSteps();
                    Set<StepGroupDef> subgroups = redlinedStepGroup.getAllChildGroups();

                    steps.forEach(stepDef ->
                    {
                        stepDef = JPAUtils.getRecordById(em, StepDef.class, stepDef.getPk());
                        stepDef.setEditType(EditType.REDLINE_DELETE);
                        em.merge(stepDef);
                    });

                    subgroups.forEach(stepGroupDef ->
                    {
                        stepGroupDef = JPAUtils.getRecordById(em, StepGroupDef.class, stepGroupDef.getPk());
                        stepGroupDef.setEditType(EditType.REDLINE_DELETE);
                        em.merge(stepGroupDef);
                    });
                }

                if (redlinedStepGroup.getPk() == null)
                {
                    em.persist(redlinedStepGroup);
                }
                else
                {
                    em.merge(redlinedStepGroup);
                }

                // now save the redline comment
                RedLineComment redLineComment = redline.getRedLineComment();
                redLineComment.setProcedureDetails(redlinedRun);
                redLineComment.setStepGroupDef(redlinedStepGroup);
                em.persist(redLineComment);

                List<RedLineComment> comments = redlinedStepGroup.getRedLineComments();
                if (comments == null)
                {
                    comments = new ArrayList<>();
                }
                comments.add(redLineComment);
                redlinedStepGroup.setRedLineComments(comments);

                updatedStepGroups.add(redlinedStepGroup);

                // if there are non-redlined groups to update (such as groups that have been reordered), save them to the database
                if (redline.getStepGroupDefList() != null && !redline.getStepGroupDefList().isEmpty())
                {
                    for (StepGroupDef group : redline.getStepGroupDefList())
                    {
                        StepGroupDef groupDb = JPAUtils.getRecordById(em, StepGroupDef.class, group.getPk());
                        groupDb.setEditType(group.getEditType());
                        groupDb.setStepGroupName(group.getStepGroupName());
                        groupDb.setDisplayOrder(group.getDisplayOrder());
                        groupDb.setDescription(group.getDescription());
                        groupDb = em.merge(groupDb);
                        updatedStepGroups.add(groupDb);
                    }
                }
            }
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String msg = "Error saving red line for step group";
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
        return updatedStepGroups;
    }

    public static List<StepDef> saveRedLinesToSteps(EntityManager em, List<RedlineData> redlineData, ProcedureDetails redlinedRun)
    {
        List<StepDef> updatedSteps = new ArrayList<>();
        try
        {
            em.getTransaction().begin();

            for (RedlineData redline : redlineData)
            {
                StepDef redlinedStep = null;
                StepDef data = redline.getStepDef();

                if (data.getPk() != null)
                {
                    redlinedStep = JPAUtils.getRecordById(em, StepDef.class, data.getPk());
                    if (redlinedStep == null)
                    {
                        String msg = "Could not find a record for step with primary key: " + data.getPk();
                        LOGGER.error(msg);
                        throw new WebApplicationException(msg);
                    }
                }

                // make sure the original procedure details redlinedVersion points to this run's ID
                redlinedRun = markThisRunAsRedlined(em, redlinedRun);

                if (redlinedStep == null)
                {
                    // this is a new step
                    redlinedStep = redline.getStepDef();
                    StepGroupDef stepGroupDef = JPAUtils.getRecordById(em, StepGroupDef.class, redlinedStep.getStepGroupDef().getPk());
                    SortedSet<StepDef> stepDefs = stepGroupDef.getStepDefs();
                    if (stepDefs == null)
                    {
                        stepDefs = new TreeSet<>();
                    }
                    stepDefs.add(redlinedStep);
                    stepGroupDef.setStepDefs(stepDefs);
                    redlinedStep.setStepGroupDef(stepGroupDef);

                    SortedSet<StepTableRow> rows = null;
                    if (redlinedStep.getType().equals(StepType.TABLE))
                    {
                        rows = ((StepTable) redlinedStep).getStepTableRows();
                        ((StepTable) redlinedStep).setStepTableRows(null);
                        for (StepTableRow row : rows)
                        {
                            row.setStepTable((StepTable) redlinedStep);
                            for (StepTableCell cell : row.getStepTableCells())
                            {
                                cell.setStepTableRow(row);
                            }
                        }
                        ((StepTable) redlinedStep).setStepTableRows(rows);
                    }
                    em.persist(redlinedStep);
                    em.merge(stepGroupDef);

                    ProcedureDetails procedureDetails = StepDAO.checkIfProcedureDetailsShouldBeMarkedHazardousOrEsd0(em, redlinedStep);
                    if (procedureDetails != null)
                    {
                        em.merge(procedureDetails);
                    }
                }
                else
                {
                    redlinedStep = (StepDef) Hibernate.unproxy(redlinedStep);
                    // if this is a table step, also need to update rows/cells
                    if (redlinedStep.getType().equals(StepType.TABLE))
                    {
                        for (StepTableRow r : ((StepTable) data).getStepTableRows())
                        {
                            r.setStepTable((StepTable) redlinedStep);
                            for (StepTableCell c : r.getStepTableCells())
                            {
                                c.setStepTableRow(r);
                            }
                        }
                        ((StepTable) redlinedStep).getStepTableRows().clear();
                        em.flush();
                        ((StepTable) redlinedStep).getStepTableRows().addAll(((StepTable) data).getStepTableRows());
                    }

                    // if here, this is not a new step; need to update the existing step's field using the step data
                    redlinedStep = StepDAO.updateCommonStepDefFields(em, redlinedStep, data);
                }

                // now save the redline comment
                RedLineComment redLineComment = redline.getRedLineComment();
                redLineComment.setProcedureDetails(redlinedRun);
                redLineComment.setStepDef(redlinedStep);
                em.persist(redLineComment);

                List<RedLineComment> redLineComments = redlinedStep.getRedLineComments();
                if (redLineComments == null)
                {
                    redLineComments = new ArrayList<>();
                }
                redLineComments.add(redLineComment);
                redlinedStep.setRedLineComments(redLineComments);
                updatedSteps.add(redlinedStep);

                // if there are non-redlined steps to update (such as steps that have been reordered), save them to the database
                if (redline.getStepDefList() != null && !redline.getStepDefList().isEmpty())
                {
                    for (StepDef step : redline.getStepDefList())
                    {
                        StepDef stepFromDb = JPAUtils.getRecordById(em, StepDef.class, step.getPk());
                        stepFromDb = StepDAO.updateCommonStepDefFields(em, stepFromDb, step);
                        stepFromDb = em.merge(stepFromDb);
                        updatedSteps.add(stepFromDb);
                    }
                }
            }
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String msg = "Error saving red line for step";
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
        return updatedSteps;
    }

    public static ProcedureDetails markThisRunAsRedlined(EntityManager em, ProcedureDetails redlinedRun)
    {
        // get the original procedure details and check if it's pointing to a redlined version
        ProcedureDetails originalProcedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, redlinedRun.getOriginalProcedureDetails().getPk());
        if (originalProcedureDetails.getRedlinedVersion() == null)
        {
            // if here, this run will be the redlined version.
            redlinedRun.setEditType(EditType.REDLINE_EDIT);
            originalProcedureDetails.setRedlinedVersion(redlinedRun.getId());

            // we need to change all the other runs associated with originalProcedureDetails to EditType.LOCKED_RUN
            for (ProcedureDetails otherRun : originalProcedureDetails.getProcedureDetailRuns())
            {
                // don't change the edit type of the redlined run; skip it
                if (otherRun.getPk() != redlinedRun.getPk())
                {
                    otherRun.setEditType(EditType.LOCKED_RUN);
                    em.merge(otherRun);
                }
            }
            redlinedRun = em.merge(redlinedRun);
            em.merge(originalProcedureDetails);
        }
        else
        {
            if (!redlinedRun.getId().equalsIgnoreCase(originalProcedureDetails.getRedlinedVersion()))
            {
                // we should never be here.
                redlinedRun.setEditType(EditType.LOCKED_RUN);
                em.merge(redlinedRun);
                // if the original procedure details is pointing to a redlined run that is not THIS run, then there's a problem.
                throw new WebApplicationException("There is already a redlined run in progress for this procedure " + originalProcedureDetails.getId() +
                        ". That run id is: " + originalProcedureDetails.getRedlinedVersion() + ". This run " + redlinedRun.getId() + " is now marked as locked.");
            }
        }
        return redlinedRun;
    }

    /**
     * This is a wrapper - the assumption is that the parent group has already had the comment applied to it, and now the comment
     * needs to be applied to the parent group's steps and subgroups.
     * 
     * @param em
     * @param parentGroup
     * @param comment
     */
    public static void applyRedLineCommentToSubGroupsAndStepsWrapper(EntityManager em, StepGroupDef parentGroup, RedLineComment comment)
    {
        try
        {
            if (parentGroup.getStepDefs() != null && !parentGroup.getStepDefs().isEmpty())
            {
                applyRedLineCommentToSteps(em, parentGroup.getStepDefs(), comment);
            }
            if (parentGroup.getStepGroupDefsChildren() != null && !parentGroup.getStepGroupDefsChildren().isEmpty())
            {
                applyRedLineCommentToSubGroupsAndSteps(em, parentGroup.getStepGroupDefsChildren(), comment);
            }
        }
        catch (Exception e)
        {
            throw new WebApplicationException("Error while applying red line comment to steps and subgroups of group with pk " + parentGroup.getPk(), e);
        }
    }

    private static void applyRedLineCommentToSteps(EntityManager em, SortedSet<StepDef> steps, RedLineComment comment)
    {
        try
        {
            steps.stream().forEach(stepDef ->
            {
                RedLineComment newComment = new RedLineComment(comment.getCommentTimestamp(), comment.getCommentText(),
                        comment.getCommentType(), comment.getUsers(), comment.getProcedureChangeType());
                newComment.setStepDef(stepDef);
                em.persist(newComment);

                List<RedLineComment> redLineComments = stepDef.getRedLineComments();
                if (redLineComments == null)
                {
                    redLineComments = new ArrayList<>();
                }
                redLineComments.add(newComment);
                stepDef.setRedLineComments(redLineComments);
            });
        }
        catch (Exception e)
        {
            throw new WebApplicationException("Problem applying red line comment to step", e);
        }
    }

    /**
     * This is a helper method that applies a redline comment to subgroups and steps. Intended to be applied to subgroups
     * and steps being copied when their parent group is being copied during redlining.
     * 
     * @param em
     * @param groups
     * @param comment
     */
    private static void applyRedLineCommentToSubGroupsAndSteps(EntityManager em, SortedSet<StepGroupDef> groups, RedLineComment comment)
    {
        try
        {
            groups.stream().forEach(stepGroupDef ->
            {
                RedLineComment newComment = new RedLineComment(comment.getCommentTimestamp(), comment.getCommentText(),
                        comment.getCommentType(), comment.getUsers(), comment.getProcedureChangeType());
                newComment.setStepGroupDef(stepGroupDef);
                em.persist(newComment);

                List<RedLineComment> redLineComments = stepGroupDef.getRedLineComments();
                if (redLineComments == null)
                {
                    redLineComments = new ArrayList<>();
                }
                redLineComments.add(newComment);
                stepGroupDef.setRedLineComments(redLineComments);

                if (stepGroupDef.getStepDefs() != null && !stepGroupDef.getStepDefs().isEmpty())
                {
                    applyRedLineCommentToSteps(em, stepGroupDef.getStepDefs(), comment);
                }

                if (stepGroupDef.getStepGroupDefsChildren() != null && !stepGroupDef.getStepGroupDefsChildren().isEmpty())
                {
                    applyRedLineCommentToSubGroupsAndSteps(em, stepGroupDef.getStepGroupDefsChildren(), comment);
                }
            });
        }
        catch (Exception e)
        {
            throw new WebApplicationException("Problem while applying red line comment to subgroups and steps", e);
        }
    }

    public static ProcedureDetails updateProcedureDetailsHazardRedLine(EntityManager em, ProcedureDetails run, RedLineComment comment, Boolean isHazard, Boolean isEsd0, String hazardDescription)
    {
        try
        {
            em.getTransaction().begin();

            run = markThisRunAsRedlined(em, run);
            if (isHazard != null)
            {
                // fail disable if there are child steps that are hazardous
                if (!isHazard && ProcedureDetailsDAO.groupsHaveHazardousChild(run.getStepGroupDefs()))
                {
                    throw new WebApplicationException("Cannot disable procedure hazard flag while procedure contains steps marked as hazardous");
                }
                run.setHazardous(isHazard);
            }
            if (hazardDescription != null)
            {
                run.setHazardDescription(hazardDescription);
            }
            if (isEsd0 != null)
            {
                // Fail disable when children have flag set.
                if (!isEsd0 && ProcedureDetailsDAO.groupsHaveEsd0Child(run.getStepGroupDefs()))
                {
                    throw new WebApplicationException("Cannot disable procedure ESD Class 0 flag while children steps have the flag.");
                }
                run.setEsd0(isEsd0);
            }
            run = em.merge(run);

            // now save the redline comment
            comment.setProcedureDetails(run);
            em.persist(comment);

            List<RedLineComment> redLineComments = run.getRedLineComments();
            if (redLineComments == null)
            {
                redLineComments = new ArrayList<>();
            }
            redLineComments.add(comment);
            run.setRedLineComments(redLineComments);

            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
                throw new WebApplicationException("Could not update hazard information as red line for run with id: " + run.getId() + ", error: " + e.getMessage(), e);
            }
        }
        return run;
    }
}
