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

import edu.jhuapl.sd.sig.epic.data.SecondSignatureDAO;
import edu.jhuapl.sd.sig.epic.data.StepDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.*;
import edu.jhuapl.sd.sig.epic.model.util.CopyUtils;
import edu.jhuapl.sd.sig.epic.utils.DataGeneratorUtils;
import edu.jhuapl.sd.sig.epic.utils.TestUtils;
import org.junit.jupiter.api.*;

import javax.persistence.EntityManager;

import java.util.TreeSet;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.*;

public class TestRunStepOperations
{
    private static EntityManager em;
    private static ProcedureDetails run;

    @BeforeAll
    public static void beforeClass()
    {
        TestUtils.init();
    }

    @BeforeEach
    public void beforeEach() throws Exception
    {
        em = JPAUtils.getEntityManager();
        run = TestUtils.createCleanRun();
    }

    @AfterEach
    public void afterEach()
    {
        run = null;
        JPAUtils.closeEntityManager(em);
    }

    @Test
    public void testSavingInitialValueToCheckboxStep()
    {
        // find a checkbox step in the run
        StepCheckbox checkboxStep = (StepCheckbox) retrieveRunStepFromRunForType(StepType.CHECKBOX);

        // assert that the run value is null
        assertNull(checkboxStep.getRunValue());

        // set the run value to true
        Users user = DataGeneratorUtils.getRandomUser();
        StepDef data = null;
        try
        {
            data = CopyUtils.copyStep(StepType.CHECKBOX, checkboxStep, EditType.RUN, checkboxStep.getStepGroupDef(),
                    true);
            data.setPk(checkboxStep.getPk());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail();
        }
        ((StepCheckbox) data).setRunValue(true);
        StepCheckbox savedStep = StepDAO.saveRunValueForCheckboxStep(em, (StepCheckbox) data, user);

        // assert that the run value is true and a history has been created
        assertTrue(savedStep.getRunValue());
        assertNotNull(savedStep.getRunValueSavedTimestamp());
        assertEquals(user.getUserId(), savedStep.getRunValueEntryUser().getUserId());
        assertEquals(1, savedStep.getHistories().size());

        // this is the run step history we expect
        History expected = new History(checkboxStep, data, user);

        // assert the run step history is correct
        History history = savedStep.getHistories().first();
        assertNotNull(history.getPk());
        assertNotNull(history.getTimestamp());
        assertEquals(expected.getUser().getUserId(), history.getUser().getUserId());
        assertEquals(expected.getDescription(), history.getDescription());
    }

    @Test
    public void testChangingRunValueForCheckboxStep()
    {
        // find a checkbox step in the run
        StepCheckbox checkboxStep = (StepCheckbox) retrieveRunStepFromRunForType(StepType.CHECKBOX);

        // assert that the run value is null
        assertNull(checkboxStep.getRunValue());

        // set the run value to true
        Users user = DataGeneratorUtils.getRandomUser();
        StepDef data = null;
        try
        {
            data = CopyUtils.copyStep(StepType.CHECKBOX, checkboxStep, EditType.RUN, checkboxStep.getStepGroupDef(),
                    true);
            data.setPk(checkboxStep.getPk());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail();
        }
        ((StepCheckbox) data).setRunValue(true);
        StepCheckbox savedStep = StepDAO.saveRunValueForCheckboxStep(em, (StepCheckbox) data, user);

        // assert that the run value is true and a history has been created
        assertTrue(savedStep.getRunValue());
        assertNotNull(savedStep.getRunValueSavedTimestamp());
        assertEquals(user.getUserId(), savedStep.getRunValueEntryUser().getUserId());
        assertEquals(1, savedStep.getHistories().size());

        // we're not checking that this run step history is correct, because we do that
        // in the test above.

        // change the value of the step
        checkboxStep.setRunValue(((StepCheckbox) data).getRunValue());
        ((StepCheckbox) data).setRunValue(false);
        savedStep = StepDAO.saveRunValueForCheckboxStep(em, (StepCheckbox) data, user);

        // assert that the run value is false and a second history has been created
        assertFalse(savedStep.getRunValue());
        assertNotNull(savedStep.getRunValueSavedTimestamp());
        assertEquals(user.getUserId(), savedStep.getRunValueEntryUser().getUserId());
        assertEquals(2, savedStep.getHistories().size());

        // this is the run step history we expect
        History expected = new History(checkboxStep, data, user);

        // assert the run step history is correct
        History history = savedStep.getHistories().last();
        assertNotNull(history.getPk());
        assertNotNull(history.getTimestamp());
        assertEquals(expected.getUser().getUserId(), history.getUser().getUserId());
        assertEquals(expected.getDescription(), history.getDescription());
    }

    @Test
    public void testSavingInitialValueToSingleValueStep()
    {
        // find a checkbox step in the run
        StepSingleValue singleValueStep = (StepSingleValue) retrieveRunStepFromRunForType(StepType.SINGLE_VALUE);

        // assert that the run value is null
        assertNull(singleValueStep.getRunValue());

        // set the run value to true
        Users user = DataGeneratorUtils.getRandomUser();
        String testValue = "Test Value 1";
        StepDef data = null;
        try
        {
            data = CopyUtils.copyStep(StepType.SINGLE_VALUE, singleValueStep, EditType.RUN,
                    singleValueStep.getStepGroupDef(), true);
            data.setPk(singleValueStep.getPk());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail();
        }
        ((StepSingleValue) data).setRunValue(testValue);
        StepSingleValue savedStep = StepDAO.saveRunValueForSingleValueStep(em, (StepSingleValue) data, user);

        // assert that the run value is correct and a history has been created
        assertNotNull(savedStep.getRunValue());
        assertEquals(testValue, savedStep.getRunValue());
        assertNotNull(savedStep.getRunValueSavedTimestamp());
        assertEquals(user.getUserId(), savedStep.getRunValueEntryUser().getUserId());
        assertEquals(1, savedStep.getHistories().size());

        // this is the run step history we expect
        History expected = new History(singleValueStep, data, user);

        // assert the run step history is correct
        History history = savedStep.getHistories().first();
        assertNotNull(history.getPk());
        assertNotNull(history.getTimestamp());
        assertEquals(expected.getUser().getUserId(), history.getUser().getUserId());
        assertEquals(expected.getDescription(), history.getDescription());
    }

    @Test
    public void testChangingRunValueForSingleValueStep()
    {
        // find a table step in the run
        StepSingleValue singleValueStep = (StepSingleValue) retrieveRunStepFromRunForType(StepType.SINGLE_VALUE);

        // assert that the run value is null
        assertNull(singleValueStep.getRunValue());

        // set the run value to true
        Users user = DataGeneratorUtils.getRandomUser();
        String testValue = "Test Value 1";
        StepDef data = null;
        try
        {
            data = CopyUtils.copyStep(StepType.SINGLE_VALUE, singleValueStep, EditType.RUN,
                    singleValueStep.getStepGroupDef(), true);
            data.setPk(singleValueStep.getPk());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail();
        }
        ((StepSingleValue) data).setRunValue(testValue);
        StepSingleValue savedStep = StepDAO.saveRunValueForSingleValueStep(em, (StepSingleValue) data, user);

        // assert that the run value is correct and a history has been created
        assertNotNull(savedStep.getRunValue());
        assertEquals(testValue, savedStep.getRunValue());
        assertNotNull(savedStep.getRunValueSavedTimestamp());
        assertEquals(user.getUserId(), savedStep.getRunValueEntryUser().getUserId());
        assertEquals(1, savedStep.getHistories().size());

        // we're not checking that this run step history is correct, because we do that
        // in the test above.

        // change the value of the step
        String testValue2 = "Test Value 2";
        singleValueStep.setRunValue(((StepSingleValue) data).getRunValue());
        ((StepSingleValue) data).setRunValue(testValue2);
        savedStep = StepDAO.saveRunValueForSingleValueStep(em, (StepSingleValue) data, user);

        // assert that the run value is testValue2 and a second history has been created
        assertNotNull(savedStep.getRunValue());
        assertEquals(testValue2, savedStep.getRunValue());
        assertNotNull(savedStep.getRunValueSavedTimestamp());
        assertEquals(user.getUserId(), savedStep.getRunValueEntryUser().getUserId());
        assertEquals(2, savedStep.getHistories().size());

        // this is the run step history we expect
        History expected = new History(singleValueStep, data, user);

        // assert the run step history is correct
        History history = savedStep.getHistories().last();
        assertNotNull(history.getPk());
        assertNotNull(history.getTimestamp());
        assertEquals(expected.getUser().getUserId(), history.getUser().getUserId());
        assertEquals(expected.getDescription(), history.getDescription());
    }

    @Test
    public void testSavingInitialValueToTableStep()
    {
        // find a table step in the run
        StepTable tableStep = (StepTable) retrieveRunStepFromRunForType(StepType.TABLE);

        // find the first editable table cell and put a value in it
        Users user = DataGeneratorUtils.getRandomUser();
        String testValue = "Test Value 1";
        StepDef data = null;
        try
        {
            data = CopyUtils.copyStep(StepType.TABLE, tableStep, EditType.RUN, tableStep.getStepGroupDef(), true);
            data.setPk(tableStep.getPk());
            ((StepTable) data)
                    .setStepTableRows(new TreeSet<>(((StepTable) data).getStepTableRows().stream().map(row ->
                    {
                        StepTableRow tableStepRow = tableStep.getStepTableRows().stream()
                                .filter(r -> r.getRowNumber().equals(row.getRowNumber())).findFirst().get();
                        row.setPk(tableStepRow.getPk());
                        row.setStepTableCells(new TreeSet<StepTableCell>(row.getStepTableCells().stream().map(cell ->
                        {
                            cell.setPk(tableStepRow.getStepTableCells().stream()
                                    .filter(c -> c.getCellIndex().equals(cell.getCellIndex())).findFirst().get()
                                    .getPk());
                            return cell;
                        }).collect(toSet())));
                        return row;
                    }).collect(toSet())));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail();
        }

        StepTableCell dataCell = this.getFirstEditableStepTableCellInTableStep((StepTable) data);
        dataCell.setNonEditableValue(testValue);
        StepTable savedStep = StepDAO.saveRunValueForTableStep(em, (StepTable) data, dataCell, user);

        // assert that the run value is correct and a history has been created
        this.compareValueToFirstEditableTableCell(savedStep, testValue);
        assertNotNull(savedStep.getRunValueSavedTimestamp());
        assertEquals(user.getUserId(), savedStep.getRunValueEntryUser().getUserId());
        assertEquals(1, savedStep.getHistories().size());

        // this is the run step history we expect
        History expected = new History(this.getFirstEditableStepTableCellInTableStep(tableStep),
                this.getFirstEditableStepTableCellInTableStep((StepTable) data), user);

        // assert the run step history is correct
        History history = savedStep.getHistories().first();
        assertNotNull(history.getPk());
        assertNotNull(history.getTimestamp());
        assertEquals(expected.getUser().getUserId(), history.getUser().getUserId());
        assertEquals(expected.getDescription(), history.getDescription());
    }

    @Test
    public void testChangingRunValueForTableStep()
    {
        // find a table step in the run
        StepTable tableStep = (StepTable) retrieveRunStepFromRunForType(StepType.TABLE);

        // find the first editable table cell and put a value in it
        Users user = DataGeneratorUtils.getRandomUser();
        String testValue = "Test Value 1";
        StepDef data = null;
        try
        {
            data = CopyUtils.copyStep(StepType.TABLE, tableStep, EditType.RUN, tableStep.getStepGroupDef(), true);
            data.setPk(tableStep.getPk());
            StepTable finalTableStep = tableStep;
            ((StepTable) data)
                    .setStepTableRows(new TreeSet<>(((StepTable) data).getStepTableRows().stream().map(row ->
                    {
                        StepTableRow tableStepRow = finalTableStep.getStepTableRows().stream()
                                .filter(r -> r.getRowNumber().equals(row.getRowNumber())).findFirst().get();
                        row.setPk(tableStepRow.getPk());
                        row.setStepTableCells(new TreeSet<StepTableCell>(row.getStepTableCells().stream().map(cell ->
                        {
                            cell.setPk(tableStepRow.getStepTableCells().stream()
                                    .filter(c -> c.getCellIndex().equals(cell.getCellIndex())).findFirst().get()
                                    .getPk());
                            return cell;
                        }).collect(toSet())));
                        return row;
                    }).collect(toSet())));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail();
        }
        StepTableCell dataCell = this.getFirstEditableStepTableCellInTableStep((StepTable) data);
        dataCell.setNonEditableValue(testValue);
        StepTable savedStep = StepDAO.saveRunValueForTableStep(em, (StepTable) data, dataCell, user);

        // assert that the run value is correct and a history has been created
        this.compareValueToFirstEditableTableCell(savedStep, testValue);
        assertNotNull(savedStep.getRunValueSavedTimestamp());
        assertEquals(user.getUserId(), savedStep.getRunValueEntryUser().getUserId());
        assertEquals(1, savedStep.getHistories().size());

        // we're not checking that this run step history is correct, because we do that
        // in the test above.

        // change the value of the step
        String testValue2 = "Test Value 2";
        tableStep = this.addAValueToFirstEditableCellInTable(tableStep, testValue);
        data = this.addAValueToFirstEditableCellInTable(data, testValue);
        dataCell = this.getFirstEditableStepTableCellInTableStep((StepTable) data);
        dataCell.setNonEditableValue(testValue2);
        savedStep = StepDAO.saveRunValueForTableStep(em, (StepTable) data, dataCell, user);

        // assert that the run value is testValue2 and a second history has been created
        this.compareValueToFirstEditableTableCell(savedStep, testValue2);
        assertNotNull(savedStep.getRunValueSavedTimestamp());
        assertEquals(user.getUserId(), savedStep.getRunValueEntryUser().getUserId());
        assertEquals(2, savedStep.getHistories().size());

        // this is the history we expect
        History expected = new History(this.getFirstEditableStepTableCellInTableStep(tableStep),
                this.getFirstEditableStepTableCellInTableStep((StepTable) data), user);

        // assert the history is correct
        History history = savedStep.getHistories().last();
        assertNotNull(history.getPk());
        assertNotNull(history.getTimestamp());
        assertEquals(expected.getUser().getUserId(), history.getUser().getUserId());
        assertEquals(expected.getDescription(), history.getDescription());
    }

    @Test
    public void signStepWithWitnessSignature()
    {
        StepDef stepDef = retrieveRunStepWithSignatureBlock(SecondSignatureType.WITNESS);

        // create a signature object
        RunStepSecondSignature witnessSignature = new WitnessSecondSignature();
        Users user = DataGeneratorUtils.getRandomUser();
        witnessSignature.setStepDef(stepDef);
        witnessSignature.setType(SecondSignatureType.WITNESS);
        witnessSignature.setUser(user);

        RunStepSecondSignature savedSignature = SecondSignatureDAO.saveRunStepSecondSignature(em, witnessSignature).getWitnessSecondSignature();

        // confirm the savedSignature is as expected.
        assertNotNull(savedSignature.getPk());
        assertNotNull(savedSignature.getTimestamp());
        assertEquals(SecondSignatureType.WITNESS, savedSignature.getType());
        assertEquals(user.getUserId(), savedSignature.getUser().getUserId());
        assertEquals(stepDef.getPk(), savedSignature.getStepDef().getPk());

        // should have created one run step history
        StepDef savedStep = savedSignature.getStepDef();
        assertEquals(1, savedStep.getHistories().size());

        // this is the run step history we expect
        History expected = new History(null, witnessSignature, user);

        // assert the run step history is correct
        History history = savedStep.getHistories().first();
        assertNotNull(history.getPk());
        assertNotNull(history.getTimestamp());
        assertEquals(expected.getUser().getUserId(), history.getUser().getUserId());
        assertEquals(expected.getDescription(), history.getDescription());
    }

    @Test
    public void signStepWithInspectionSignature()
    {
        StepDef stepDef = retrieveRunStepWithSignatureBlock(SecondSignatureType.MANDATORY_INSPECTION);

        // create a signature object
        RunStepSecondSignature inspectionSecondSignature = new MandatoryInspectionSecondSignature();
        Users user = DataGeneratorUtils.getRandomUser();
        inspectionSecondSignature.setStepDef(stepDef);
        inspectionSecondSignature.setType(SecondSignatureType.MANDATORY_INSPECTION);
        inspectionSecondSignature.setUser(user);

        RunStepSecondSignature savedSignature = SecondSignatureDAO.saveRunStepSecondSignature(em,
                inspectionSecondSignature).getMandatoryInspectionSecondSignature();

        // confirm the savedSignature is as expected.
        assertNotNull(savedSignature.getPk());
        assertNotNull(savedSignature.getTimestamp());
        assertEquals(SecondSignatureType.MANDATORY_INSPECTION, savedSignature.getType());
        assertEquals(user.getUserId(), savedSignature.getUser().getUserId());
        assertEquals(stepDef.getPk(), savedSignature.getStepDef().getPk());

        // should have created one run step history
        StepDef savedStep = savedSignature.getStepDef();
        assertEquals(1, savedStep.getHistories().size());

        // this is the run step history we expect
        History expected = new History(null, savedSignature, user);

        // assert the run step history is correct
        History history = savedStep.getHistories().first();
        assertNotNull(history.getPk());
        assertNotNull(history.getTimestamp());
        assertEquals(expected.getUser().getUserId(), history.getUser().getUserId());
        assertEquals(expected.getDescription(), history.getDescription());
    }

    private StepDef retrieveRunStepFromRunForType(StepType stepType)
    {
        StepDef stepDef = null;
        while (stepDef == null)
        {
            try
            {
                stepDef = run.getAllSteps().stream().filter(step -> step.getType().equals(stepType)).findFirst().get();
            }
            catch (Exception e)
            {
                stepDef = null;
            }
            if (stepDef == null)
            {
                try
                {
                    run = TestUtils.createCleanRun();
                }
                catch (Exception e)
                {
                    fail();
                }
            }
        }
        return stepDef;
    }

    private StepTable addAValueToFirstEditableCellInTable(StepDef table, String valueToSave)
    {
        boolean found = false;

        while (!found)
        {
            for (StepTableRow row : ((StepTable) table).getStepTableRows())
            {
                for (StepTableCell cell : row.getStepTableCells())
                {
                    if (cell.getEditable())
                    {
                        cell.setNonEditableValue(valueToSave);
                        found = true;
                        break;
                    }
                }
                if (found)
                    break;
            }

            // what if an editable cell doesn't exist in the table?
            if (!found)
            {
                try
                {
                    run = TestUtils.createCleanRun();
                }
                catch (Exception e)
                {
                    fail();
                }
                table = retrieveRunStepFromRunForType(StepType.TABLE);
            }
        }
        return (StepTable) table;
    }

    private void compareValueToFirstEditableTableCell(StepTable table, String valueToCompare)
    {
        boolean found = false;
        for (StepTableRow row : table.getStepTableRows())
        {
            for (StepTableCell cell : row.getStepTableCells())
            {
                if (cell.getEditable())
                {
                    assertNotNull(cell.getNonEditableValue());
                    assertEquals(valueToCompare, cell.getNonEditableValue());
                    found = true;
                    break;
                }
            }
            if (found)
                break;
        }
    }

    private StepTableCell getFirstEditableStepTableCellInTableStep(StepTable stepTable)
    {
        for (StepTableRow row : stepTable.getStepTableRows())
        {
            for (StepTableCell cell : row.getStepTableCells())
            {
                if (cell.getEditable())
                {
                    return cell;
                }
            }
        }
        return null;
    }

    private StepDef retrieveRunStepWithSignatureBlock(SecondSignatureType secondSignatureType)
    {
        StepDef stepDef = null;
        while (stepDef == null)
        {
            try
            {
                stepDef = run.getAllSteps().stream().filter(step ->
                {
                    if (secondSignatureType.equals(SecondSignatureType.WITNESS))
                    {
                        return step.getRequireWitness();
                    }
                    else if (secondSignatureType.equals(SecondSignatureType.MANDATORY_INSPECTION))
                    {
                        return step.getMandatoryInspection();
                    }
                    return false;
                }).findFirst().get();
            }
            catch (Exception e)
            {
                stepDef = null;
            }
            if (stepDef == null)
            {
                try
                {
                    run = TestUtils.createCleanRun();
                }
                catch (Exception e)
                {
                    fail();
                }
            }
        }
        return stepDef;
    }
}
