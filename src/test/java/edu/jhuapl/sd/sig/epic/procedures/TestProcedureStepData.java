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

import edu.jhuapl.sd.sig.epic.model.*;

import java.util.SortedSet;
import java.util.TreeSet;

public enum TestProcedureStepData
{

    TEST_STEP_DATA_SET_1
    {
        @Override
        public SortedSet<StepDef> getTestData()
        {
            SortedSet<StepDef> stepDefs = new TreeSet<>();

            StepDef stepDef = new StepSingleValue();
            stepDef.setDisplayOrder(1);
            stepDef.setEsd0(false);
            stepDef.setHazardous(false);
            stepDef.setInstructions("These are instructions for step 1 of test data set 1");
            stepDef.setMandatoryInspection(false);
            stepDef.setRequireWitness(true);
            stepDef.setStepName("Test Step 1");
            stepDef.setAllowEquipmentEntry(false);
            stepDef.setType(StepType.SINGLE_VALUE);
            stepDefs.add(stepDef);

            stepDef = new StepCheckbox();
            stepDef.setDisplayOrder(2);
            stepDef.setEsd0(true);
            stepDef.setHazardous(true);
            stepDef.setInstructions("These are instructions for step 2 of test data set 1");
            stepDef.setMandatoryInspection(false);
            stepDef.setRequireWitness(false);
            stepDef.setStepName("Test Step 2");
            stepDef.setAllowEquipmentEntry(true);
            stepDef.setType(StepType.CHECKBOX);
            stepDefs.add(stepDef);

            return stepDefs;
        }
    },
    TEST_STEP_DATA_SET_2
    {
        @Override
        public SortedSet<StepDef> getTestData()
        {
            SortedSet<StepDef> stepDefs = new TreeSet<>();
            SortedSet<StepTableRow> rows = new TreeSet<>();
            SortedSet<StepTableCell> cells = new TreeSet<>();

            StepTableCell cell = new StepTableCell();
            cell.setCellIndex(0);
            cell.setEditable(false);
            cell.setNonEditableValue("Test Column A");
            cells.add(cell);

            cell = new StepTableCell();
            cell.setCellIndex(1);
            cell.setEditable(false);
            cell.setNonEditableValue("Test Column B");
            cells.add(cell);

            StepTableRow row = new StepTableRow();
            row.setRowNumber(0);
            row.setStepTableCells(cells);
            rows.add(row);

            cells = new TreeSet<>();

            cell = new StepTableCell();
            cell.setCellIndex(0);
            cell.setEditable(false);
            cell.setNonEditableValue("User 1's Score");
            cells.add(cell);

            cell = new StepTableCell();
            cell.setCellIndex(1);
            cell.setEditable(true);
            cells.add(cell);

            row = new StepTableRow();
            row.setRowNumber(1);
            row.setStepTableCells(cells);
            rows.add(row);

            StepDef stepDef = new StepTable();
            ((StepTable) stepDef).setStepTableRows(rows);
            stepDef.setDisplayOrder(1);
            stepDef.setEsd0(false);
            stepDef.setHazardous(false);
            stepDef.setInstructions("These are instructions for table step 1 of test data set 2");
            stepDef.setMandatoryInspection(false);
            stepDef.setRequireWitness(true);
            stepDef.setStepName("Test Step Table 1");
            stepDef.setAllowEquipmentEntry(false);
            stepDef.setType(StepType.TABLE);
            stepDefs.add(stepDef);

            return stepDefs;
        }
    };

    public abstract SortedSet<StepDef> getTestData();
}
