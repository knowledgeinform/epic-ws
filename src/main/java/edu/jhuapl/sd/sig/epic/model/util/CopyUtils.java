/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.model.util;

import edu.jhuapl.sd.sig.epic.model.*;
import org.hibernate.Hibernate;

import java.io.*;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

public class CopyUtils
{

    public static SortedSet<ProcedureInstruction> copyProcedureInstructions(SortedSet<ProcedureInstruction> instructionsToCopy, ProcedureDetails targetProcDetails, EditType editTypeForNewInstructions,
            Boolean omitRedlines)
    {
        SortedSet<ProcedureInstruction> pis = new TreeSet<>();

        if (instructionsToCopy == null || instructionsToCopy.isEmpty())
            return pis;

        for (ProcedureInstruction piid : instructionsToCopy)
        {

            if (omitRedlines && piid.getEditType() == EditType.REDLINE_DELETE)
                continue;

            ProcedureInstruction pi = new ProcedureInstruction();
            pi.setSectionName(piid.getSectionName());
            pi.setDisplayOrder(piid.getDisplayOrder());
            pi.setText(piid.getText());
            pi.setProcedureDetails(targetProcDetails);

            // logic - if the editType is RUN or LOCKED_RUN, then we want to preserve any REDLINE editType for the
            // instruction, so that we can mark on the UI that this is a redlined change

            // KVF 12-09-2019: Revising the logic - if the run is locked, no instruction copying should be done in the
            // first place. If this is a redline instruction copy, then the new instruction should always be
            // marked as REDLINE_ADD - edit type does not change on the original instruction. If this is an instruction
            // being copied on a procedure in draft mode, edit type should always be ORIGINAL. Edit type should be passed
            // in as a parameter by whatever the originating resource method is - see the step group/step copy/clone
            // functions for an example.
            pi.setEditType(editTypeForNewInstructions);

            pis.add(pi);
        }

        // make sure the display orders are numbered correctly.
        int displayOrder = 1;
        for (ProcedureInstruction instruction : pis)
        {
            instruction.setDisplayOrder(displayOrder++);
        }

        return pis;
    }

    public static SortedSet<StepGroupDef> copyStepGroupDefs(SortedSet<StepGroupDef> groupsToCopy, ProcedureDetails targetProcDetails, EditType targetEditType, StepGroupDef parentGroup,
            List<Integer> stepPksToCopy, List<Integer> stepGroupPksToCopy, Boolean omitRedlines) throws Exception
    {
        if (groupsToCopy == null || groupsToCopy.size() == 0)
        {
            return new TreeSet<>();
        }
        SortedSet<StepGroupDef> stepGroupSet = new TreeSet<>();
        for (StepGroupDef sgd : groupsToCopy)
        {

            if (omitRedlines && sgd.getEditType() == EditType.REDLINE_DELETE)
                continue;

            // add group to set
            StepGroupDef copiedGroup = copyGroup(sgd, targetEditType, parentGroup, targetProcDetails, stepPksToCopy, stepGroupPksToCopy, omitRedlines);
            if (copiedGroup != null)
            {
                stepGroupSet.add(copiedGroup);
            }
        } // end for-loop for groups in this array

        // should now have all the step groups in array copied to stepGroupSet. Return stepGroupSet
        int displayOrder = 1;
        for (StepGroupDef s : stepGroupSet)
        {
            s.setDisplayOrder(displayOrder++);
        }
        return stepGroupSet;
    }

    public static StepGroupDef copyGroup(StepGroupDef groupToCopy, EditType targetEditType, StepGroupDef parentGroup, ProcedureDetails targetProcDetails, List<Integer> stepPksToCopy,
            List<Integer> stepGroupPksToCopy,
            Boolean omitRedlines) throws Exception
    {
        StepGroupDef group = new StepGroupDef();

        // KVF 11/21/2019: New step groups should only ever have edit type of ORIGINAL (if added during authoring mode) or
        // REDLINE_ADD (if added during redlining). This edit type should be passed into this function. It should not
        // be the edit type of the source step group, because in the case of redlining, the source step group may have a different
        // edit type than that of the new step group
        // first set the details of this group
        group.setEditType(targetEditType);

        group.setDisplayOrder(groupToCopy.getDisplayOrder());

        // only top level groups link to the procedure def version
        if (parentGroup == null)
        {
            group.setProcedureDetails(targetProcDetails);
        }
        else
        {
            group.setProcedureDetails(null);
        }
        group.setStepGroupName(groupToCopy.getStepGroupName());
        group.setDescription(groupToCopy.getDescription());
        group.setStepGroupDefParent(parentGroup);

        // add all of sgd's step to this new group.
        SortedSet<StepDef> childSteps = copySteps(groupToCopy.getStepDefs(), targetEditType, group, stepPksToCopy, omitRedlines);
        group.setStepDefs(childSteps);

        // now need to recurse through sgd's children
        SortedSet<StepGroupDef> children = copyStepGroupDefs(groupToCopy.getStepGroupDefsChildren(), targetProcDetails, targetEditType, group, stepPksToCopy, stepGroupPksToCopy, omitRedlines);
        group.setStepGroupDefsChildren(children);

        //if stepsPkToCopy has values, then we're filtering results
        if (((stepPksToCopy != null && stepPksToCopy.size() > 0) || (stepGroupPksToCopy != null && !stepGroupPksToCopy.isEmpty())) &&
                childSteps.size() == 0 && children.size() == 0)
        {
            // check if this group is on the list of groups that should be copied
            if (stepGroupPksToCopy == null || stepGroupPksToCopy.isEmpty() || !stepGroupPksToCopy.contains(groupToCopy.getPk()))
            {
                //do not copy this group, there are no child stepsg present
                return null;
            }
        }
        return group;
    }

    public static SortedSet<StepDef> copySteps(SortedSet<StepDef> stepsToCopy, EditType targetEditType, StepGroupDef targetGroup, List<Integer> stepPksToCopy, Boolean omitRedlines) throws Exception
    {

        SortedSet<StepDef> stepsForGroup = new TreeSet<>();
        if (stepsToCopy != null && stepsToCopy.size() > 0)
        {
            for (StepDef s : stepsToCopy)
            {
                // add the step to the set of steps for this group
                if (stepPksToCopy == null || stepPksToCopy.contains(s.getPk()))
                {
                    if (omitRedlines && s.getEditType() == EditType.REDLINE_DELETE)
                        continue;
                    // FIXME: KVF 2021-03-30: Not super happy with the addition of Hibernate.unproxy(s) here - it doesn't seem
                    // quite right, but the s was coming through as a Hibernate proxy and the only solution I could find
                    // was to unproxy it before copying.
                    stepsForGroup.add(copyStep((StepDef) Hibernate.unproxy(s), targetEditType, targetGroup, omitRedlines));
                }
            } // end for-loop for steps
        }
        int displayOrder = 1;
        for (StepDef s : stepsForGroup)
        {
            s.setDisplayOrder(displayOrder++);
        }
        return stepsForGroup;
    }

    public static StepDef copyStep(StepDef stepToCopy, EditType targetEditType, StepGroupDef targetGroup, boolean omitRedlines) throws Exception
    {
        return copyStep(stepToCopy.getType(), stepToCopy, targetEditType, targetGroup, omitRedlines);
    }

    public static StepDef copyStep(StepType newStepType, StepDef stepToCopy, EditType targetEditType, StepGroupDef targetGroup, boolean omitRedlines) throws Exception
    {
        StepDef step;
        if (newStepType == StepType.CHECKBOX)
        {
            step = new StepCheckbox();
        }
        else if (newStepType == StepType.SINGLE_VALUE)
        {
            step = new StepSingleValue();
        }
        else
        {
            step = new StepTable();
        }

        // KVF 11/21/2019: New steps should only ever have edit type of ORIGINAL (if added during authoring mode) or
        // REDLINE_ADD (if added during redlining). This edit type should be passed into this function. It should not
        // be the edit type of the source step, because in the case of redlining, the source step may have a different
        // edit type than that of the new step
        // set the editType
        step = copyStepDefCommonFields(stepToCopy, step);
        step.setEditType(targetEditType);
        step.setStepGroupDef(targetGroup);
        step.setType(newStepType);

        //copy step attachments
        if (stepToCopy.getStepDefAttachments() != null)
        {
            step.setStepDefAttachments(new TreeSet<StepDefAttachment>()
            {});
            for (StepDefAttachment originalAttachment : stepToCopy.getStepDefAttachments())
            {
                // if we are omitting redline deletes, don't copy over deleted step attachments
                if (omitRedlines && originalAttachment.getEditType().equals(EditType.REDLINE_DELETE))
                    continue;

                StepDefAttachment newSa = new StepDefAttachment();
                newSa.setStepDef(step);
                newSa.setIsImage(originalAttachment.getIsImage());
                newSa.setOriginalFilename(originalAttachment.getOriginalFilename());
                newSa.setFilename(UUID.randomUUID().toString());
                newSa.setEditType(targetEditType);
                // Copy the file
                AttachmentHandler.saveAttachment(AttachmentHandler.getFullPathToFile(originalAttachment),
                        AttachmentHandler.UPLOAD_ROOT_DIR + File.separator + newSa.getFilename());
                //add to the new step
                step.getStepDefAttachments().add(newSa);
            }
        }

        // if this is a table step, we also need to copy rows and cells
        if (newStepType == StepType.TABLE)
        {
            if (newStepType.equals(stepToCopy.getType()))
            {
                ((StepTable) step).setStepTableRows(copyStepTableRows(((StepTable) stepToCopy).getStepTableRows(), (StepTable) step));
            }
            else
            {
                SortedSet<StepTableRow> rows = new TreeSet<>();
                ((StepTable) step).setStepTableRows(rows);

                //create an empty 1x2 table
                StepTableRow row = new StepTableRow();

                row.setRowNumber(0);
                row.setStepTable((StepTable) step);
                rows.add(row);
                SortedSet<StepTableCell> cells = new TreeSet<>();
                row.setStepTableCells(cells);

                StepTableCell cell1 = new StepTableCell();
                cell1.setCellIndex(0);
                cell1.setStepTableRow(row);
                cell1.setEditable(true);
                cell1.setOptional(false);
                cells.add(cell1);

                StepTableCell cell2 = new StepTableCell();
                cell2.setCellIndex(1);
                cell2.setStepTableRow(row);
                cell2.setEditable(true);
                cell2.setOptional(false);
                cells.add(cell2);

            }
        }
        return step;
    }

    public static StepDef copyStepDefCommonFields(StepDef stepToCopy, StepDef step)
    {
        step.setStepName(stepToCopy.getStepName());
        step.setRequireWitness(stepToCopy.getRequireWitness());
        step.setMandatoryInspection(stepToCopy.getMandatoryInspection());
        step.setEsd0(stepToCopy.getEsd0());
        step.setHazardous(stepToCopy.getHazardous());
        step.setDisplayOrder(stepToCopy.getDisplayOrder());
        step.setInstructions(stepToCopy.getInstructions());
        step.setAllowEquipmentEntry(stepToCopy.getAllowEquipmentEntry());
        return step;
    }

    public static SortedSet<StepTableRow> copyStepTableRows(SortedSet<StepTableRow> originalRows, StepTable table)
    {
        SortedSet<StepTableRow> copyOfRows = new TreeSet<>();
        if (originalRows != null && originalRows.size() > 0)
        {
            for (StepTableRow r : originalRows)
            {
                StepTableRow row = new StepTableRow();
                row.setRowNumber(r.getRowNumber());
                row.setStepTable(table);
                // copy over the cells in the row
                SortedSet<StepTableCell> copyOfCells = new TreeSet<>();
                if (r.getStepTableCells() != null && r.getStepTableCells().size() > 0)
                {
                    for (StepTableCell c : r.getStepTableCells())
                    {
                        StepTableCell cell = new StepTableCell();
                        cell.setStepTableRow(row);
                        cell.setEditable(c.getEditable());
                        cell.setOptional(c.isOptional());
                        cell.setCellIndex(c.getCellIndex());

                        // only set the value is this is a non-editable cell. We do not want
                        // to copy over the contents of editable cells.
                        // TODO: At some point we may wish to allow for users to place default values or placeholders
                        // in editable cells, and those would need to be copied over.
                        if (!cell.getEditable())
                        {
                            cell.setNonEditableValue(c.getNonEditableValue());
                        }

                        copyOfCells.add(cell);
                    } // end for-loop
                }
                row.setStepTableCells(copyOfCells);
                copyOfRows.add(row);
            } // end for-loop
        }
        return copyOfRows;
    }

    public static StepGroupDef getRootGroup(StepGroupDef groupDef)
    {
        StepGroupDef parentGroup = groupDef.getStepGroupDefParent();
        if (parentGroup != null)
        {
            return getRootGroup(parentGroup);
        }
        return groupDef;
    }

}
