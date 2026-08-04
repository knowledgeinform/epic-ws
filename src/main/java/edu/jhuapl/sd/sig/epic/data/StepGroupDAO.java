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
import edu.jhuapl.sd.sig.epic.model.util.CopyUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.WebApplicationException;
import javax.xml.ws.WebServiceException;
import java.util.*;

public class StepGroupDAO
{
    private static final Logger LOGGER = LogManager.getLogger();

    public static SortedSet<StepGroupDef> getStepGroups(EntityManager em, Integer proc_def_ver_id)
    {
        SortedSet<StepGroupDef> groups;
        ProcedureDetails pdv = JPAUtils.getRecordById(em, ProcedureDetails.class, proc_def_ver_id);
        groups = pdv.getStepGroupDefs();

        return groups;
    }

    public static List<StepGroupDef> insertNewStepGroupDefTransaction(EntityManager em, List<StepGroupDef> data, Integer procedureDetailsPk)
    {
        List<StepGroupDef> stepGroups = new ArrayList<>();
        try
        {
            em.getTransaction().begin();
            stepGroups = addStepGroupsOperations(em, data, procedureDetailsPk);
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String message = "Error inserting new step group into db";
            LOGGER.error(message, e);
            throw new WebApplicationException(message, e);
        }
        return stepGroups;
    }

    private static List<StepGroupDef> addStepGroupsOperations(EntityManager em, List<StepGroupDef> groupsToAdd, Integer procedureDetailsPk)
    {
        List<StepGroupDef> stepGroups = new ArrayList<>();
        for (StepGroupDef sgData : groupsToAdd)
        {
            StepGroupDef sgd = null;
            if (sgData.getPk() != null)
            {
                // if we are here, this is a step group with an updated display order due to the addition of a new step group.
                // That's the only thing changing for this group.
                sgd = JPAUtils.getRecordById(em, StepGroupDef.class, sgData.getPk());
                sgd.setDisplayOrder(sgData.getDisplayOrder());
                em.merge(sgd);
            }
            else
            {
                // if here, this is the new group
                sgd = new StepGroupDef();
                sgd.setStepGroupName(sgData.getStepGroupName());
                sgd.setDisplayOrder(sgData.getDisplayOrder());
                sgd.setDescription(sgData.getDescription());
                sgd.setEditType(EditType.ORIGINAL);

                StepGroupDef parentStepGroup = null;
                if (sgData.getStepGroupDefParent() != null)
                {
                    // has a step group parent
                    parentStepGroup = JPAUtils.getRecordById(em, StepGroupDef.class, sgData.getStepGroupDefParent().getPk());
                    sgd.setStepGroupDefParent(parentStepGroup);

                    SortedSet<StepGroupDef> children = parentStepGroup.getStepGroupDefsChildren();
                    if (children == null)
                    {
                        children = new TreeSet<>();
                    }
                    children.add(sgd);
                    parentStepGroup.setStepGroupDefsChildren(children);
                }
                else
                {
                    // no step group parent - is top level
                    ProcedureDetails pdv = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDetailsPk);
                    sgd.setProcedureDetails(pdv);
                }

                em.persist(sgd);
                if (parentStepGroup != null)
                {
                    em.merge(parentStepGroup);
                }
            }
            stepGroups.add(sgd);
        }
        return stepGroups;
    }

    public static List<StepGroupDef> updateStepGroupsTransaction(EntityManager em, List<StepGroupDef> stepGroupData)
    {
        List<StepGroupDef> updatedStepGroups = new ArrayList<>();

        try
        {
            em.getTransaction().begin();
            updatedStepGroups = updateStepGroupsOperations(em, stepGroupData);
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String message = "Error updating step group into database";
            LOGGER.error(message, e);
            throw new WebApplicationException(message, e);
        }
        return updatedStepGroups;
    }

    private static List<StepGroupDef> updateStepGroupsOperations(EntityManager em, List<StepGroupDef> stepGroupData)
    {
        List<StepGroupDef> updatedStepGroups = new ArrayList<>();
        for (StepGroupDef sgData : stepGroupData)
        {
            StepGroupDef stepGroupDef;

            stepGroupDef = JPAUtils.getRecordById(em, StepGroupDef.class, sgData.getPk());
            stepGroupDef.setStepGroupName(sgData.getStepGroupName());
            stepGroupDef.setDisplayOrder(sgData.getDisplayOrder());
            stepGroupDef.setDescription(sgData.getDescription());

            StepGroupDef newStepGroupParent = null;
            if (sgData.getStepGroupDefParent() != null)
            {
                newStepGroupParent = JPAUtils.getRecordById(em, StepGroupDef.class, sgData.getStepGroupDefParent().getPk());
            }
            StepGroupDef currentStepGroupParent = stepGroupDef.getStepGroupDefParent();

            if (currentStepGroupParent != null && newStepGroupParent == null)
            {
                // step group has moved from subgroup to top group
                stepGroupDef.setStepGroupDefParent(newStepGroupParent);

                SortedSet<StepGroupDef> children = currentStepGroupParent.getStepGroupDefsChildren();
                children.remove(stepGroupDef);
                currentStepGroupParent.setStepGroupDefsChildren(children);
                em.merge(currentStepGroupParent);

                ProcedureDetails pdv = JPAUtils.getRecordById(em, ProcedureDetails.class, sgData.getProcedureDetails().getPk());
                stepGroupDef.setProcedureDetails(pdv);
            }
            else if (currentStepGroupParent == null && newStepGroupParent != null)
            {
                // group has moved from top group to being a subgroup
                stepGroupDef.setStepGroupDefParent(newStepGroupParent);

                SortedSet<StepGroupDef> children = newStepGroupParent.getStepGroupDefsChildren();
                if (children == null)
                {
                    children = new TreeSet<>();
                }
                children.add(stepGroupDef);
                newStepGroupParent.setStepGroupDefsChildren(children);
                em.merge(newStepGroupParent);

                // WHY THIS?!?!??!
                // KVF 10/29/2019 response: Only top level groups get linked to the procedure details. Subgroups link
                // to their parent group.
                stepGroupDef.setProcedureDetails(null);
            }
            else if (currentStepGroupParent != null && newStepGroupParent != null &&
                    (currentStepGroupParent.getPk() != newStepGroupParent.getPk()))
            {
                // group has moved to a new parent group
                // need to set the new parent, remove the group from the old parent's children, and add to the new parent's children
                stepGroupDef.setStepGroupDefParent(newStepGroupParent);

                SortedSet<StepGroupDef> children = currentStepGroupParent.getStepGroupDefsChildren();
                children.remove(stepGroupDef);
                currentStepGroupParent.setStepGroupDefsChildren(children);

                children = newStepGroupParent.getStepGroupDefsChildren();
                if (children == null)
                {
                    children = new TreeSet<>();
                }
                children.add(stepGroupDef);
                newStepGroupParent.setStepGroupDefsChildren(children);

                em.merge(currentStepGroupParent);
                em.merge(newStepGroupParent);
            }
            em.merge(stepGroupDef);
            updatedStepGroups.add(stepGroupDef);
        }
        return updatedStepGroups;
    }

    /**
     * This function deletes a step group. It calls two private utility functions to first delete the step group's
     * stepDefs and its child step groups before the step group itself is deleted.
     * 
     * @param em
     * @param pk
     * @return boolean of deletion success.
     */
    public static boolean deleteStepGroupTransaction(EntityManager em, int pk)
    {
        boolean deletedStepGroup = false;
        try
        {
            em.getTransaction().begin();
            // first get the step group from the database
            StepGroupDef stepGroupDef = em.find(StepGroupDef.class, pk);
            deleteStepGroupOperations(em, stepGroupDef);

            em.getTransaction().commit();
            deletedStepGroup = true;
        }
        catch (Exception e)
        {
            deletedStepGroup = false;
            throw new WebServiceException("Could not complete step group deletion for group with primary key " + pk, e);
        }
        return deletedStepGroup;
    }

    private static void deleteStepGroupOperations(EntityManager em, StepGroupDef stepGroupDef)
    {
        try
        {
            // next delete all the steps in this step group
            if (stepGroupDef.getStepDefs() != null && stepGroupDef.getStepDefs().size() > 0)
            {
                deleteAllStepsInStepGroup(em, stepGroupDef.getStepDefs());
            }

            // if this step group has children subgroups, all of those subgroup also need to be deleted.
            // we're going to call a recursive function to walk through the tree and do that.
            SortedSet<StepGroupDef> childrenStepGroups = stepGroupDef.getStepGroupDefsChildren();

            if (childrenStepGroups != null && childrenStepGroups.size() > 0)
            {
                deleteAllStepGroupsInThisArray(em, childrenStepGroups);
            }

            // remove this step group from its parent
            StepGroupDef parentStepGroup = stepGroupDef.getStepGroupDefParent();

            if (parentStepGroup != null)
            {
                SortedSet<StepGroupDef> childrenGroupsOfParent = parentStepGroup.getStepGroupDefsChildren();
                childrenGroupsOfParent.remove(stepGroupDef);
                parentStepGroup.setStepGroupDefsChildren(childrenGroupsOfParent);
                em.merge(parentStepGroup);
            }

            // and now finally delete the step group itself
            stepGroupDef.setStepGroupDefParent(null);
            em.remove(stepGroupDef);
        }
        catch (Exception e)
        {
            throw e;
        }

    }

    /**
     * This utility function is a recursive function for deleting all step groups within a given array. It walks through
     * each step group to first delete that group's steps, then its children step groups, then finally the step group
     * itself.
     * 
     * @param em
     * @param stepGroupDefs
     */
    private static void deleteAllStepGroupsInThisArray(EntityManager em, SortedSet<StepGroupDef> stepGroupDefs)
    {
        try
        {
            for (StepGroupDef sgd : stepGroupDefs)
            {
                // for each group, first delete its steps
                if (sgd.getStepDefs() != null && sgd.getStepDefs().size() > 0)
                {
                    deleteAllStepsInStepGroup(em, sgd.getStepDefs());
                }

                // now delete the child step groups of this step group
                if (sgd.getStepGroupDefsChildren() != null && sgd.getStepGroupDefsChildren().size() > 0)
                {
                    deleteAllStepGroupsInThisArray(em, sgd.getStepGroupDefsChildren());
                }

                // finally now delete this step group
                em.remove(sgd);
            }
        }
        catch (Exception e)
        {
            throw e;
        }
    }

    /**
     * This is a utility function for deleting all the steps within a step group.
     * 
     * @param em
     * @param stepDefs
     */
    private static void deleteAllStepsInStepGroup(EntityManager em, SortedSet<StepDef> stepDefs)
    {
        try
        {
            for (StepDef step : stepDefs)
            {
                em.remove(step);
            }
        }
        catch (Exception e)
        {
            throw e;
        }
    }

    public static void persistStepGroupArray(EntityManager em, SortedSet<StepGroupDef> stepGroupDefs)
    {
        if (stepGroupDefs == null || stepGroupDefs.size() == 0)
        {
            return;
        }

        try
        {
            for (StepGroupDef group : stepGroupDefs)
            {
                // persist all steps in the group
                //                em.persist(group);
                SortedSet<StepDef> stepsFromGroup = group.getStepDefs();
                group.setStepDefs(null);

                SortedSet<StepGroupDef> childrenGroups = group.getStepGroupDefsChildren();
                group.setStepGroupDefsChildren(null);

                em.persist(group);

                for (StepGroupDef child : childrenGroups)
                {
                    child.setStepGroupDefParent(group);
                }

                for (StepDef stepDef : stepsFromGroup)
                {
                    stepDef.setStepGroupDef(group);
                }

                if (stepsFromGroup != null && stepsFromGroup.size() > 0)
                {
                    for (StepDef step : stepsFromGroup)
                    {
                        SortedSet<StepTableRow> rows = null;
                        if (step.getType() == StepType.TABLE)
                        {
                            rows = ((StepTable) step).getStepTableRows();
                            ((StepTable) step).setStepTableRows(null);
                        }
                        em.persist(step);

                        if (rows != null && rows.size() > 0)
                        {
                            // is a table step, need to save the rows and cells
                            for (StepTableRow row : rows)
                            {
                                SortedSet<StepTableCell> cells = row.getStepTableCells();
                                row.setStepTable((StepTable) step);
                                row.setStepTableCells(null);
                                em.persist(row);
                                for (StepTableCell cell : cells)
                                {
                                    cell.setStepTableRow(row);
                                    em.persist(cell);
                                }
                                row.setStepTableCells(new TreeSet<>(cells));
                            }
                            ((StepTable) step).setStepTableRows(new TreeSet<>(rows));
                        }
                    } // end for loop stepsFromGroup
                }
                group.setStepDefs(new TreeSet<StepDef>(stepsFromGroup));
                //                em.persist(group);
                persistStepGroupArray(em, childrenGroups);
                group.setStepGroupDefsChildren(new TreeSet<>(childrenGroups));
                //                em.merge(group);
                //                group.setStepGroupDefsChildren(childrenGroups);
                //                em.merge(group);
            }
        }
        catch (Exception e)
        {
            throw new WebApplicationException("Error persisting step groups", e);
        }
    }

    public static List<StepGroupDef> copyStepsHierarchyToGroup(EntityManager em, List<StepDef> stepsToCopy, List<StepGroupDef> groupsToCopy, Integer parentGroupPk,
            Integer procedurePk, EditType editType, RedLineComment comment)
    {
        //read steps from database..and collect root groups and list of step PK's to copy
        Set<StepGroupDef> groupsToCopySet = new HashSet<>();
        List<Integer> stepPksToCopy = new ArrayList<>();
        List<Integer> stepGroupPksToCopy = new ArrayList<>();
        for (StepDef sd : stepsToCopy)
        {
            stepPksToCopy.add(sd.getPk());
            StepDef s = JPAUtils.getRecordById(em, StepDef.class, sd.getPk());
            groupsToCopySet.add(CopyUtils.getRootGroup(s.getStepGroupDef()));
        }
        for (StepGroupDef sgd : groupsToCopy)
        {
            stepGroupPksToCopy.add(sgd.getPk());
            groupsToCopySet.add(CopyUtils.getRootGroup(JPAUtils.getRecordById(em, StepGroupDef.class, sgd.getPk())));
        }

        List<StepGroupDef> groupsToCopyList = new ArrayList<>();
        groupsToCopyList.addAll(groupsToCopySet);

        return copyGroupsToGroup(em, groupsToCopyList, parentGroupPk, procedurePk, stepPksToCopy, stepGroupPksToCopy, editType, comment);
    }

    public static StepGroupDef copyGroupToGroup(EntityManager em, Integer groupPk, Integer parentGroupPk, Integer procedurePk,
            EditType editType, RedLineComment comment)
    {
        StepGroupDef sourceGroup = em.find(StepGroupDef.class, groupPk);
        return copyGroupsToGroup(em, Arrays.asList(sourceGroup), parentGroupPk, procedurePk, null, null, editType, comment).get(0);
    }

    public static List<StepGroupDef> copyGroupsToGroup(EntityManager em, List<StepGroupDef> sourceGroups, Integer parentGroupPk,
            Integer procedurePk, List<Integer> stepPksToCopy, List<Integer> stepGroupPksToCopy, EditType editType,
            RedLineComment comment)
    {
        try
        {
            em.getTransaction().begin();
            List<StepGroupDef> newGroups = new ArrayList<>();
            for (StepGroupDef sourceGroup : sourceGroups)
            {
                ProcedureDetails pd = null;
                StepGroupDef parentGroup = null;
                //				EditType et;
                if (parentGroupPk != null && parentGroupPk > 0)
                {
                    parentGroup = em.find(StepGroupDef.class, parentGroupPk);
                    //					et = parentGroup.getEditType();
                }
                else
                {
                    pd = em.find(ProcedureDetails.class, procedurePk);
                    //					et = pd.getEditType();
                }

                StepGroupDef newGroup = CopyUtils.copyGroup(sourceGroup, editType, parentGroup, pd, stepPksToCopy, stepGroupPksToCopy, false);
                if (newGroup != null)
                {
                    //set the display order and child of parent
                    if (newGroup.getProcedureDetails() != null)
                    {
                        newGroup.setDisplayOrder(newGroup.getProcedureDetails().getStepGroupDefs().size() + 1);
                        newGroup.getProcedureDetails().getStepGroupDefs().add(newGroup);
                    }
                    else
                    {
                        newGroup.setDisplayOrder(newGroup.getStepGroupDefParent().getStepGroupDefsChildren().size() + 1);
                        newGroup.getStepGroupDefParent().getStepGroupDefsChildren().add(newGroup);
                    }

                    em.persist(newGroup);
                    em.merge(pd == null ? parentGroup : pd);

                    if (comment != null)
                    {
                        if (pd == null)
                        {
                            pd = em.find(ProcedureDetails.class, procedurePk);
                        }

                        if (pd == null)
                        {
                            String msg = "Could not find a run with primary key of " + procedurePk;
                            LOGGER.error(msg);
                            throw new WebApplicationException(msg);
                        }

                        // the red line comment might be applied multiple times (to each new step group)
                        RedLineComment newComment = new RedLineComment(comment.getCommentTimestamp(), comment.getCommentText(),
                                comment.getCommentType(), comment.getUsers(), comment.getProcedureChangeType());
                        newComment.setProcedureDetails(pd);
                        newComment.setStepGroupDef(newGroup);
                        em.persist(newComment);

                        List<RedLineComment> redLineComments = newGroup.getRedLineComments();
                        if (redLineComments == null)
                        {
                            redLineComments = new ArrayList<>();
                        }
                        redLineComments.add(newComment);
                        newGroup.setRedLineComments(redLineComments);

                        // the comment needs to also be applied to all subgroups and steps.
                        // note that the clone/copy function for groups should be the only place where a redline comment is
                        // applied to a step/subgroup without also being applied to the procedureDetails. This is so we don't
                        // end up with multiple copies of this comment at the procedureDetails level for every step/subgroup copied as
                        // part of this function.
                        RedlineDAO.applyRedLineCommentToSubGroupsAndStepsWrapper(em, newGroup, comment);
                    }
                    newGroups.add(newGroup);
                }
            }
            em.getTransaction().commit();
            return newGroups;
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String msg = "Could not copy group.";
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
    }

    public static List<StepGroupDef> bulkAddUpdateDeleteTransaction(EntityManager em, List<StepGroupDef> groupsToAdd,
            List<StepGroupDef> groupsToUpdate, List<StepGroupDef> groupsToDelete,
            Integer procedureDetailsPk)
    {
        List<StepGroupDef> stepGroups = new ArrayList<>();
        try
        {
            em.getTransaction().begin();
            stepGroups.addAll(addStepGroupsOperations(em, groupsToAdd, procedureDetailsPk));
            stepGroups.addAll(updateStepGroupsOperations(em, groupsToUpdate));
            for (StepGroupDef group : groupsToDelete)
            {
                deleteStepGroupOperations(em, group);
            }
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String message = "Error doing bulk add/update/delete of step groups to db";
            LOGGER.error(message, e);
            throw new WebApplicationException(message, e);
        }
        return stepGroups;
    }
}
