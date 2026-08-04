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
import edu.jhuapl.sd.sig.epic.model.CommentType;
import edu.jhuapl.sd.sig.epic.model.EditType;
import edu.jhuapl.sd.sig.epic.model.History;
import edu.jhuapl.sd.sig.epic.model.MandatoryInspectionSecondSignature;
import edu.jhuapl.sd.sig.epic.model.ProcedureDetails;
import edu.jhuapl.sd.sig.epic.model.RedLineComment;
import edu.jhuapl.sd.sig.epic.model.RunStepComment;
import edu.jhuapl.sd.sig.epic.model.StepCheckbox;
import edu.jhuapl.sd.sig.epic.model.StepDef;
import edu.jhuapl.sd.sig.epic.model.StepDefAttachment;
import edu.jhuapl.sd.sig.epic.model.StepGroupDef;
import edu.jhuapl.sd.sig.epic.model.StepSingleValue;
import edu.jhuapl.sd.sig.epic.model.StepTable;
import edu.jhuapl.sd.sig.epic.model.StepTableCell;
import edu.jhuapl.sd.sig.epic.model.StepTableRow;
import edu.jhuapl.sd.sig.epic.model.StepType;
import edu.jhuapl.sd.sig.epic.model.Users;
import edu.jhuapl.sd.sig.epic.model.WitnessSecondSignature;
import edu.jhuapl.sd.sig.epic.model.util.AttachmentHandler;
import edu.jhuapl.sd.sig.epic.model.util.CopyUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;

import javax.persistence.EntityManager;
import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

public class StepDAO
{
    private static final Logger LOGGER = LogManager.getLogger();

    public static StepDef createNewSingleValueOrCheckboxStep(EntityManager em, StepDef data)
    {
        try
        {
            em.getTransaction().begin();

            StepDef stepDef = data;
            stepDef.setEditType(EditType.ORIGINAL);

            StepGroupDef stepGroupDef = JPAUtils.getRecordById(em, StepGroupDef.class, data.getStepGroupDef().getPk());
            SortedSet<StepDef> stepDefs = stepGroupDef.getStepDefs();
            if (stepDefs == null)
            {
                stepDefs = new TreeSet<>();
            }
            stepDefs.add(stepDef);
            stepGroupDef.setStepDefs(stepDefs);

            stepDef.setStepGroupDef(stepGroupDef);
            em.persist(stepDef);

            em.merge(stepGroupDef);

            ProcedureDetails procedureDetails = checkIfProcedureDetailsShouldBeMarkedHazardousOrEsd0(em, stepDef);
            if (procedureDetails != null)
            {
                em.merge(procedureDetails);
            }

            em.getTransaction().commit();
            return stepDef;
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String msg = "Problem while saving new step of type: " + data.getType();
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
    }

    public static StepTable createNewStepTable(EntityManager em, StepTable data)
    {

        StepTable stepDef;

        try
        {
            em.getTransaction().begin();

            stepDef = data;
            stepDef.setEditType(EditType.ORIGINAL);

            StepGroupDef stepGroupDef = JPAUtils.getRecordById(em, StepGroupDef.class, data.getStepGroupDef().getPk());
            SortedSet<StepDef> stepDefs = stepGroupDef.getStepDefs();
            if (stepDefs == null)
            {
                stepDefs = new TreeSet<>();
            }

            SortedSet<StepTableRow> rows = stepDef.getStepTableRows();
            for (StepTableRow row : rows)
            {
                row.setStepTable(stepDef);
                for (StepTableCell cell : row.getStepTableCells())
                {
                    cell.setStepTableRow(row);
                }
            }

            stepDefs.add(stepDef);
            stepGroupDef.setStepDefs(stepDefs);

            stepDef.setStepGroupDef(stepGroupDef);
            em.persist(stepDef);

            em.merge(stepGroupDef);

            ProcedureDetails procedureDetails = checkIfProcedureDetailsShouldBeMarkedHazardousOrEsd0(em, stepDef);
            if (procedureDetails != null)
            {
                em.merge(procedureDetails);
            }

            em.getTransaction().commit();
            return stepDef;
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
                LOGGER.error("Problem while saving new Table Entry Step", e);
                throw new WebApplicationException("Problem while saving new Table Entry Step", e);
            }
        }
        return null;
    }

    public static StepDef updateSingleValueOrCheckboxStep(EntityManager em, StepDef data)
    {
        try
        {
            em.getTransaction().begin();

            StepDef stepDef = JPAUtils.getRecordById(em, StepDef.class, data.getPk());

            stepDef = updateCommonStepDefFields(em, stepDef, data);

            em.getTransaction().commit();
            return stepDef;
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String msg = "Problem while updating step with pk: " + data.getPk();
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
    }

    public static StepDef updateTableStep(EntityManager em, StepTable data, boolean updateTables)
    {
        try
        {
            em.getTransaction().begin();

            // get the existing table
            StepTable stepDef = JPAUtils.getRecordById(em, StepTable.class, data.getPk());

            if (updateTables)
            {
                for (StepTableRow r : data.getStepTableRows())
                {
                    r.setStepTable(stepDef);
                    for (StepTableCell c : r.getStepTableCells())
                    {
                        c.setStepTableRow(r);
                    }
                }
                stepDef.getStepTableRows().clear();
                stepDef.getStepTableRows().addAll(data.getStepTableRows());
                //em.flush();

                /*
                 * OLD Implementation, this fails with a constraint violation exception because of unique constraint.
                 * Keeping for reference if we need to revert back to a more surgical approach to changes.
                 */
                //				SortedSet<StepTableRow> newRows = data.getStepTableRows();
                //				SortedSet<StepTableRow> rowsInExistingStepDef = stepDef.getStepTableRows();
                //
                //				// now need to look for rows that were deleted
                //				List<StepTableRow> deletedRows = new ArrayList<>();
                //				for (StepTableRow existingRow : stepDef.getStepTableRows())
                //				{
                //					boolean found = newRows.stream().anyMatch(r -> Objects.equals(r.getPk(), existingRow.getPk()));
                //					if (!found)
                //					{
                //						// existing row is not in the array of updated rows - must be deleted.
                //						deletedRows.add(existingRow);
                //					}
                //				}
                //
                //				for (StepTableRow deleted : deletedRows)
                //				{
                //					// delete this row and all of its cells.
                //					SortedSet<StepTableCell> deletedCells = deleted.getStepTableCells();
                ////					deleted.setStepTableCells(null);
                //					for (StepTableCell deletedCell : deletedCells)
                //					{
                ////                    deletedCell.setStepTableRow(null);
                //						em.remove(JPAUtils.getRecordById(em, StepTableCell.class, deletedCell.getPk()));
                //						em.flush();
                //					}
                ////					deleted.setStepTable(null);
                //					em.remove(JPAUtils.getRecordById(em, StepTableRow.class, deleted.getPk()));
                //					em.flush();
                ////					rowsInExistingStepDef.remove(deleted);
                //				}
                //
                //
                //				// deal with updating all the rows and cells
                //				for (StepTableRow newRow : newRows)
                //				{
                //					// if row does not have a PK, is a new row
                //					if (newRow.getPk() == null)
                //					{
                //						newRow.setStepTable(stepDef);
                //						for (StepTableCell cell : newRow.getStepTableCells())
                //						{
                //							cell.setStepTableRow(newRow);
                //						}
                //						rowsInExistingStepDef.add(newRow);
                //					}
                //					else
                //					{
                //						// is an existing row, the existing row needs to be updated with the new row info/cells
                //						StepTableRow existingRow = JPAUtils.getRecordById(em, StepTableRow.class, newRow.getPk());
                //						existingRow.setRowNumber(newRow.getRowNumber());
                //						SortedSet<StepTableCell> existingCellsForRow = existingRow.getStepTableCells();
                //						List<StepTableCell> deletedCells = new ArrayList<>();
                //						// find what cells have been deleted
                //						for (StepTableCell existingCell : existingCellsForRow)
                //						{
                //							boolean found = newRow.getStepTableCells().stream().anyMatch(r -> Objects.equals(r.getPk(), existingCell.getPk()));
                //							if (!found)
                //							{
                //								// cell that was in existing row is not in new row - must be deleted.
                //								deletedCells.add(existingCell);
                //							}
                //						}
                //
                //						// remove the deleted cells from existing row and database
                //						for (StepTableCell deletedCell : deletedCells)
                //						{
                ////							existingCellsForRow.remove(deletedCell);
                //							em.remove(JPAUtils.getRecordById(em, StepTableCell.class, deletedCell.getPk()));
                //							em.flush();
                //						}
                //
                //						// now update remaining cells in this row
                //						for (StepTableCell newCell : newRow.getStepTableCells())
                //						{
                //							if (newCell.getPk() == null)
                //							{
                //								// this is a new cell
                //								newCell.setStepTableRow(existingRow);
                //								existingCellsForRow.add(newCell);
                //								em.persist(newCell);
                //							}
                //							else
                //							{
                //								// this is an existing cell
                //								StepTableCell existingCell = JPAUtils.getRecordById(em, StepTableCell.class, newCell.getPk());
                //								existingCell.setCellIndex(newCell.getCellIndex());
                //								existingCell.setEditable(newCell.getEditable());
                //								existingCell.setNonEditableValue(newCell.getNonEditableValue());
                //								em.merge(existingCell);
                //							}
                //						}
                //						em.merge(existingRow);
                //					}
                //				}

                // add the updated rows to the stepDef
                //				stepDef.setStepTableRows(rowsInExistingStepDef);

            }

            stepDef = (StepTable) updateCommonStepDefFields(em, stepDef, data);

            em.getTransaction().commit();
            return stepDef;
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String msg = "Problem while updating step with pk: " + data.getPk();
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
    }

    /**
     * This method updates the fields that all step def types have in common.
     * 
     * @param em
     * @param updatedStep
     * @param data
     * @return
     */
    public static StepDef updateCommonStepDefFields(EntityManager em, StepDef updatedStep, StepDef data)
    {
        // update the step's fields
        updatedStep = CopyUtils.copyStepDefCommonFields(data, updatedStep);
        updatedStep.setEditType(data.getEditType());

        // if the updated step has currently existing witness/inspection signatures but the witness/inspection
        // flag has been turned off, need to delete those signatures
        if (!updatedStep.getRequireWitness() && updatedStep.getWitnessSecondSignature() != null)
        {
            WitnessSecondSignature witnessSecondSignature = JPAUtils.getRecordById(em, WitnessSecondSignature.class, updatedStep.getWitnessSecondSignature().getPk());
            updatedStep.setWitnessSecondSignature(null);
            em.remove(witnessSecondSignature);
        }

        if (!updatedStep.getMandatoryInspection() && updatedStep.getMandatoryInspectionSecondSignature() != null)
        {
            MandatoryInspectionSecondSignature mandatoryInspectionSecondSignature = JPAUtils.getRecordById(em, MandatoryInspectionSecondSignature.class,
                    updatedStep.getMandatoryInspectionSecondSignature().getPk());
            updatedStep.setMandatoryInspectionSecondSignature(null);
            em.remove(mandatoryInspectionSecondSignature);
        }

        // get the two step groups involved
        StepGroupDef oldStepGroup = JPAUtils.getRecordById(em, StepGroupDef.class,
                updatedStep.getStepGroupDef().getPk());
        StepGroupDef newStepGroup = JPAUtils.getRecordById(em, StepGroupDef.class,
                data.getStepGroupDef().getPk());

        if (!Objects.equals(newStepGroup.getPk(), oldStepGroup.getPk()))
        {
            // the step was moved to a new step group
            // move step def from the old group
            SortedSet<StepDef> oldSteps = oldStepGroup.getStepDefs();
            oldSteps.remove(updatedStep);
            oldStepGroup.setStepDefs(oldSteps);

            // add step def to the new step group
            SortedSet<StepDef> newSteps = newStepGroup.getStepDefs();
            if (newSteps == null)
            {
                newSteps = new TreeSet<>();
            }
            newSteps.add(updatedStep);
            newStepGroup.setStepDefs(newSteps);
            updatedStep.setStepGroupDef(newStepGroup);
        }

        em.merge(updatedStep);
        em.merge(newStepGroup);
        if (!Objects.equals(oldStepGroup.getPk(), newStepGroup.getPk()))
        {
            em.merge(oldStepGroup);
        }

        ProcedureDetails procedureDetails = checkIfProcedureDetailsShouldBeMarkedHazardousOrEsd0(em, updatedStep);
        if (procedureDetails != null)
        {
            em.merge(procedureDetails);
        }

        return updatedStep;
    }

    public static ProcedureDetails checkIfProcedureDetailsShouldBeMarkedHazardousOrEsd0(EntityManager em, StepDef stepDef)
    {
        StepGroupDef parentGroup = stepDef.getStepGroupDef();

        while (parentGroup.getProcedureDetails() == null)
        {
            parentGroup = parentGroup.getStepGroupDefParent();
        }

        ProcedureDetails procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, parentGroup.getProcedureDetails().getPk());

        // KVF 12/8/2019: The logic here is that if a step is hazardous and the procedure is not, the procedure should be marked as
        // hazardous. If the procedure is already hazardous, it should not change regardless of the hazard level of the step.
        boolean changed = false;
        if (stepDef.getHazardous() && !procedureDetails.getHazardous())
        {
            procedureDetails.setHazardous(stepDef.getHazardous());
            changed = true;
        }
        // same logic for ESD0
        if (stepDef.getEsd0() && !procedureDetails.getEsd0())
        {
            procedureDetails.setEsd0(stepDef.getEsd0());
            changed = true;

        }
        if (changed)
        {
            return procedureDetails;
        }
        return null;
    }

    public static boolean deleteStep(EntityManager em, int pk)
    {
        boolean deletedStep = false;
        try
        {
            em.getTransaction().begin();
            StepDef stepDef = em.find(StepDef.class, pk);
            deleteStepNoTransaction(em, stepDef);
            em.getTransaction().commit();
            deletedStep = true;
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String msg = "Could not complete step deletion for step with primary key " + pk;
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
        finally
        {
            em.close();
        }
        return deletedStep;
    }

    //convenience method, should entity manager should wrap this in a transacation
    private static void deleteStepNoTransaction(EntityManager em, StepDef stepDef) throws Exception
    {
        // first get the step from the database

        StepGroupDef stepGroupDef = em.find(StepGroupDef.class, stepDef.getStepGroupDef().getPk());
        SortedSet<StepDef> stepDefs = stepGroupDef.getStepDefs();
        stepDefs.remove(stepDef);
        stepGroupDef.setStepDefs(stepDefs);
        em.merge(stepGroupDef);

        //delete any step attachments
        if (stepDef.getStepDefAttachments() != null)
        {
            for (StepDefAttachment sda : stepDef.getStepDefAttachments())
            {
                try
                {
                    AttachmentHandler.deleteAttachment(AttachmentHandler.getFullPathToFile(sda));
                }
                catch (Exception ee)
                {
                    LOGGER.error("Failed to delete attachment with pk: " + sda.getPk(), ee);
                    throw ee;
                }

            }
        }

        em.remove(stepDef);
    }

    /**
     * This method saves the run value for a single value step.
     * It assumes that all information about the step remains the same, and updates the run-related fields only
     * 
     * @param em
     * @param data
     * @return stepSingleValue
     */
    public static StepSingleValue saveRunValueForSingleValueStep(EntityManager em, StepSingleValue data, Users user)
    {
        StepSingleValue stepSingleValue = null;
        try
        {
            // find this step in the database using the primary key from the data
            stepSingleValue = JPAUtils.getRecordById(em, StepSingleValue.class, data.getPk());

            //integrity check
            if (stepSingleValue.getEditType() == EditType.ORIGINAL)
            {
                //CANNOT SET VALUE FOR ORIGINAL PROC STEP
                throw new RuntimeException("Error Occurred Saving Value for ORIGINAL Procedure Step pk=" + data.getPk() + ". Contact Application Administrator.");
            }

            if (stepSingleValue.getRunValue() == null || !stepSingleValue.getRunValue().equals(data.getRunValue()))
            {
                // start the entity manager transaction
                em.getTransaction().begin();

                //create new runstephistory
                saveHistoryForStep(em, stepSingleValue, data, stepSingleValue, user);

                // note: if stepSingleValue is null, it will throw an NPE at this next statement

                // we are only updating the run-related fields for the single value step
                stepSingleValue.setRunValue(data.getRunValue());
                // we are setting the run value saved timestamp to the current time.
                stepSingleValue.setRunValueSavedTimestamp(new Date(System.currentTimeMillis()));

                stepSingleValue.setRunValueEntryUser(user);

                // set the run step comments to stepSingleValue
                stepSingleValue.setRunStepComments(saveRunStepComments(em, data, stepSingleValue));

                // now save the step
                em.merge(stepSingleValue);
                em.getTransaction().commit();
            }
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String msg = "Could not save the run value for step with primary key " + data.getPk();
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
        return stepSingleValue;
    }

    /**
     * This method saves the run value for a checkbox step.
     * It assumes that all information about the step remains the same, and updates the run-related fields only
     * 
     * @param em
     * @param data
     * @return stepCheckbox
     */
    public static StepCheckbox saveRunValueForCheckboxStep(EntityManager em, StepCheckbox data, Users user)
    {
        StepCheckbox stepCheckbox = null;
        try
        {
            // find this step in the database using the primary key from the data
            stepCheckbox = JPAUtils.getRecordById(em, StepCheckbox.class, data.getPk());

            //integrity check
            if (stepCheckbox.getEditType() == EditType.ORIGINAL)
            {
                //CANNOT SET VALUE FOR ORIGINAL PROC STEP
                throw new RuntimeException("Error Occurred Saving Value for ORIGINAL Procedure Step pk=" + data.getPk() + ". Contact Application Administrator.");
            }

            //create new runstephistory
            String checkboxValue = stepCheckbox.getRunValue() == null ? null : stepCheckbox.getRunValue().toString();
            if (checkboxValue == null || !checkboxValue.equals(data.getRunValue()))
            {
                // start the entity manager transaction
                em.getTransaction().begin();

                saveHistoryForStep(em, stepCheckbox, data, stepCheckbox, user);

                // we are only updating the run-related fields for the checkbox step
                stepCheckbox.setRunValue(data.getRunValue());
                // we are setting the run value saved timestamp to the current time.
                stepCheckbox.setRunValueSavedTimestamp(new Date(System.currentTimeMillis()));

                stepCheckbox.setRunValueEntryUser(user);

                // if there are any runStepComments, we need to save those.

                // set the run step comments to stepCheckbox
                stepCheckbox.setRunStepComments(saveRunStepComments(em, data, stepCheckbox));

                // now save the step
                em.merge(stepCheckbox);
                em.getTransaction().commit();
            }
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String msg = "Could not save the run value for step with primary key " + data.getPk();
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
        return stepCheckbox;
    }

    public static synchronized StepTable saveRunValueForTableStep(EntityManager em, StepTable data, StepTableCell dataCell, Users user)
    {
        StepTable stepTable = null;
        try
        {
            em.getTransaction().begin();
            // find this step in the database using the primary key from the data
            stepTable = (StepTable) Hibernate.unproxy(JPAUtils.getRecordById(em, StepTable.class, data.getPk()));
            StepTableCell cell = JPAUtils.getRecordById(em, StepTableCell.class, dataCell.getPk());

            //integrity check
            if (stepTable.getEditType() == EditType.ORIGINAL)
            {
                //CANNOT SET VALUE FOR ORIGINAL PROC STEP
                throw new RuntimeException("Error Occurred Saving Value for ORIGINAL Procedure Step pk=" + data.getPk() + ". Contact Application Administrator.");
            }

            final StepTable FINAL_STEP_TABLE = stepTable;
            FINAL_STEP_TABLE.getStepTableRows().stream()
                    .filter(row -> row.getPk().equals(cell.getStepTableRow().getPk()))
                    .findFirst()
                    .get()
                    .getStepTableCells()
                    .stream()
                    .filter(c -> c.getPk().equals(cell.getPk()))
                    .findFirst()
                    .ifPresent(c ->
                    {
                        // We want to only update the cell in the database.
                        if (cell.getEditable())
                        {
                            if (cell.getNonEditableValue() == null
                                    || !cell.getNonEditableValue().equals(dataCell.getNonEditableValue()))
                            {
                                // create new history
                                // c.setStepTableRow(r);
                                dataCell.setStepTableRow(cell.getStepTableRow());
                                History history = saveHistoryForStep(em, cell, dataCell, FINAL_STEP_TABLE, user);

                                if (history != null)
                                {
                                    cell.setNonEditableValue(dataCell.getNonEditableValue());
                                    c = em.merge(cell);
                                }
                            }
                        }
                    });

            stepTable = FINAL_STEP_TABLE;

            // we are setting the run value saved timestamp to the current time.
            stepTable.setRunValueSavedTimestamp(new Date(System.currentTimeMillis()));

            stepTable.setRunValueEntryUser(user);

            // if there are any runStepComments, we need to save those.

            // set the run step comments to stepTable
            stepTable.setRunStepComments(saveRunStepComments(em, data, stepTable));

            // now save the step
            em.merge(stepTable);
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String msg = "Could not save the run value for step with primary key " + data.getPk();
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
        return stepTable;
    }

    /**
     * This is a helper method for creating and saving a run step history for a step.
     * 
     * @param em
     * @param oldValue
     * @param newValue
     * @param stepToSaveTo
     * @param user
     */
    private static History saveHistoryForStep(EntityManager em, Object oldValue, Object newValue, StepDef stepToSaveTo, Users user)
    {
        History history = new History(oldValue, newValue, user);
        if (oldValue instanceof StepTableCell || newValue instanceof StepTableCell)
        {
            history.setStepDef(stepToSaveTo);
        }
        em.persist(history);
        stepToSaveTo.getHistories().add(history);
        return history;
    }

    /**
     * This is a helper method for saving the run step comments from a stepDef data object to an existing stepDef.
     * 
     * @param em
     * @param data
     * @param stepDef
     * @return runStepComments
     */
    private static SortedSet<RunStepComment> saveRunStepComments(EntityManager em, StepDef data, StepDef stepDef)
    {
        SortedSet<RunStepComment> runStepComments = null;
        if (data.getRunStepComments() != null && !data.getRunStepComments().isEmpty())
        {
            // get the run step comments currently associated with the database step
            runStepComments = stepDef.getRunStepComments();

            // if comments are null, initialize
            if (runStepComments == null)
            {
                runStepComments = new TreeSet<>();
            }

            // if the run step comments in the data object have pks, they already exist in the database and should
            // already be associated with this step.
            for (RunStepComment comment : data.getRunStepComments())
            {
                if (comment.getPk() != null)
                {
                    // if here, the comment should already be in the database and associated to stepSingleValue. Look for it.

                    boolean alreadyExists = runStepComments.stream().anyMatch(p -> p.getPk().equals(comment.getPk()));
                    // alreadyExists ought to be true; if it's false, that means we have a comment in the data object
                    // that was previously saved to the database, but it wasn't saved to our step from the database.
                    // we need to throw a WebApplicationException.
                    if (!alreadyExists)
                    {
                        String message = "An existing run step comment was not properly associated " +
                                "to step with pk " + stepDef.getPk() + ". Please notify a system administrator.";
                        LOGGER.error(message);
                        throw new WebApplicationException(message);
                    }
                    // if alreadyExists == true, then we don't have to do anything else; this comment is already in
                    // the database and is associated with this step, so we can move on to the next comment in the loop.
                }
                else
                {
                    // if we are here, the comment does not have a PK, which means it is a new comment that needs to
                    // be associated with stepSingleValue.

                    // save the comment - set the timestamp to the current system time.
                    comment.setCommentTimestamp(new Date(System.currentTimeMillis()));
                    // set the type
                    comment.setCommentType(CommentType.RUN_STEP_COMMENT);
                    // set it's step to stepSingleValue
                    comment.setStepDef(stepDef);

                    // save the comment
                    em.persist(comment);
                    runStepComments.add(comment);
                }
            } // end of for loop
        }
        return runStepComments;
    }

    /**
     * Save a run step (value) comment
     * Assumes that the comment's stepDef has been previously set.
     * 
     * @param em
     * @param runStepComment
     * @return
     */
    public static RunStepComment saveRunStepComment(EntityManager em, RunStepComment runStepComment)
    {
        try
        {
            em.getTransaction().begin();
            runStepComment = em.merge(runStepComment);
            em.getTransaction().commit();
            return runStepComment;
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String msg = "Could not save the run value comment";
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
    }

    public static StepDef changeStepType(EntityManager em, Integer stepPk, StepType stepType)
    {
        try
        {
            em.getTransaction().begin();
            StepDef sourceStep = em.find(StepDef.class, stepPk);

            StepDef newStep = CopyUtils.copyStep(stepType, sourceStep, sourceStep.getEditType(), sourceStep.getStepGroupDef(), false);

            //save new step
            em.persist(newStep);

            //delete old step
            deleteStepNoTransaction(em, sourceStep);

            em.getTransaction().commit();
            return newStep;

        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String msg = "Could not change step type.";
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
    }

    public static StepDef copyStepToGroup(EntityManager em, Integer stepPk, Integer groupPk, EditType editType,
            Integer procedureDetailsPk, RedLineComment comment)
    {
        try
        {
            em.getTransaction().begin();
            StepDef sourceStep = em.find(StepDef.class, stepPk);
            StepGroupDef targetGroup = em.find(StepGroupDef.class, groupPk);

            StepDef newStep = CopyUtils.copyStep(sourceStep, editType, targetGroup, false);
            //adjust the display order to put it at the end of the list
            newStep.setDisplayOrder(targetGroup.getStepDefs().size() + 1);

            targetGroup.getStepDefs().add(newStep);

            em.persist(newStep);
            em.merge(targetGroup);

            if (comment != null)
            {
                ProcedureDetails redlinedRun = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDetailsPk);

                if (redlinedRun == null)
                {
                    // problem!
                    String msg = "Could not find a run with primary key of " + procedureDetailsPk;
                    LOGGER.error(msg);
                    throw new WebApplicationException(msg);
                }

                comment.setProcedureDetails(redlinedRun);
                comment.setStepDef(newStep);
                em.persist(comment);

                List<RedLineComment> redLineComments = newStep.getRedLineComments();
                if (redLineComments == null)
                {
                    redLineComments = new ArrayList<>();
                }
                redLineComments.add(comment);
                newStep.setRedLineComments(redLineComments);
            }

            em.getTransaction().commit();
            return newStep;
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String msg = "Could not copy step.";
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
    }
}
