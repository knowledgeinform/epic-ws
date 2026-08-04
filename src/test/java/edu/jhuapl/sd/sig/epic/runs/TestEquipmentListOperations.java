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

import edu.jhuapl.sd.sig.epic.data.EquipmentListDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.*;
import edu.jhuapl.sd.sig.epic.utils.DataGeneratorUtils;
import edu.jhuapl.sd.sig.epic.utils.TestUtils;
import org.junit.jupiter.api.*;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TestEquipmentListOperations
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

        // assert that the run has no equipment
        assertNull(run.getRun().getEquipmentList());
    }

    @AfterEach
    public void afterEach()
    {
        run = null;
        JPAUtils.closeEntityManager(em);
    }

    @Test
    public void addEquipmentListToRun()
    {
        // add equipment to the run
        EquipmentList data = createNewEquipment();
        Users randomUser = DataGeneratorUtils.getRandomUser();
        EquipmentListDAO.setEquipmentListItemForRun(em, run.getRun().getPk(), null, data, randomUser);

        // get the run from the database and assert there is one equipment item
        Run dbRun = JPAUtils.getRecordById(em, Run.class, run.getRun().getPk());
        assertEquals(1, dbRun.getEquipmentList().size());

        // assert statements
        EquipmentList equipmentListFromDb = dbRun.getEquipmentList().iterator().next();
        assertNotNull(equipmentListFromDb.getPk());
        assertEquals(dbRun.getPk(), equipmentListFromDb.getRun().getPk());
        assertNull(equipmentListFromDb.getSteps());
        assertEqualEquipmentItems(data, equipmentListFromDb);

        // check that there is a history object for the equipment addition
        List<History> histories = dbRun.getProcedureDetails().getHistories().stream()
                .filter(hist -> hist.getDescription().equalsIgnoreCase("Added " + equipmentListFromDb.toString()))
                .collect(Collectors.toList());

        assertNotNull(histories);
        assertEquals(1, histories.size());
        assertEquals(randomUser.getUserId(), histories.get(0).getUser().getUserId());
        assertNotNull(histories.get(0).getTimestamp());
    }

    @Test
    public void addEquipmentToStep()
    {
        // get a step from the run
        StepDef step = run.getAllSteps().stream().findFirst().get();

        // add equipment to the run
        EquipmentList data = createNewEquipment();
        Users user = DataGeneratorUtils.getRandomUser();
        EquipmentListDAO.setEquipmentListItemForRun(em, run.getRun().getPk(), step.getPk(), data, user);

        // get the run from the database and assert there is one equipment item
        Run dbRun = JPAUtils.getRecordById(em, Run.class, run.getRun().getPk());
        assertEquals(1, dbRun.getEquipmentList().size());

        // get the step from the database and assert that there is one equipment item
        StepDef dbStep = JPAUtils.getRecordById(em, StepDef.class, step.getPk());
        assertEquals(1, dbStep.getEquipment().size());

        // assert that the step equipment is the same as the data
        EquipmentList equipmentListFromDb = dbStep.getEquipment().iterator().next();
        assertNotNull(equipmentListFromDb.getPk());
        assertEquals(dbRun.getPk(), equipmentListFromDb.getRun().getPk());
        assertEquals(1, equipmentListFromDb.getSteps().size());
        assertEquals(dbStep.getPk(), equipmentListFromDb.getSteps().iterator().next().getPk());
        assertEqualEquipmentItems(data, equipmentListFromDb);

        // assert that the run and the step equipment is the same
        EquipmentList runEquipmentFromDb = dbRun.getEquipmentList().iterator().next();
        assertEquals(runEquipmentFromDb.getPk(), equipmentListFromDb.getPk());
        assertEquals(runEquipmentFromDb.getRun().getPk(), equipmentListFromDb.getRun().getPk());
        assertEquals(runEquipmentFromDb.getSteps().iterator().next().getPk(), equipmentListFromDb.getSteps().iterator().next().getPk());
        assertEqualEquipmentItems(runEquipmentFromDb, equipmentListFromDb);

        // assert that there is a history on the step
        assertEquals(1, dbStep.getHistories().size());
        History history = dbStep.getHistories().first();
        assertEquals(user.getUserId(), history.getUser().getUserId());

        // check that there is a history object for the equipment addition on the run
        List<History> histories = dbRun.getProcedureDetails().getHistories().stream()
                .filter(hist -> hist.getDescription().equalsIgnoreCase("Added " + runEquipmentFromDb.toString()))
                .collect(Collectors.toList());

        assertNotNull(histories);
        assertEquals(1, histories.size());
        assertEquals(user.getUserId(), histories.get(0).getUser().getUserId());
        assertNotNull(histories.get(0).getTimestamp());
    }

    @Test
    public void removeEquipmentFromRunWhenNotInAStep()
    {
        // add equipment to the run
        EquipmentList data = createNewEquipment();
        Users user = DataGeneratorUtils.getRandomUser();
        EquipmentListDAO.setEquipmentListItemForRun(em, run.getRun().getPk(), null, data, user);

        // get the run from the database and assert there is one equipment item
        Run dbRun = JPAUtils.getRecordById(em, Run.class, run.getRun().getPk());
        assertEquals(1, dbRun.getEquipmentList().size());

        // delete the equipment from the run
        EquipmentList equipmentList = dbRun.getEquipmentList().iterator().next();
        Users user2 = DataGeneratorUtils.getRandomUser();
        EquipmentListDAO.deleteEquipmentListItem(em, equipmentList.getPk(), user2);

        // get the run from the database and assert there are no equipment items.
        dbRun = JPAUtils.getRecordById(em, Run.class, run.getRun().getPk());
        assertEquals(0, dbRun.getEquipmentList().size());

        // check that there is a history object for the equipment deletion on the run
        List<History> histories = dbRun.getProcedureDetails().getHistories().stream()
                .filter(hist -> hist.getDescription().equalsIgnoreCase("Removed " + equipmentList.toString()))
                .collect(Collectors.toList());

        assertNotNull(histories);
        assertEquals(1, histories.size());
        assertEquals(user2.getUserId(), histories.get(0).getUser().getUserId());
        assertNotNull(histories.get(0).getTimestamp());
    }

    @Test
    public void removeEquipmentFromAStepButNotRun()
    {
        // get a step from the run
        StepDef step = run.getAllSteps().stream().findFirst().get();

        // add equipment to the run
        EquipmentList data = createNewEquipment();
        Users user = DataGeneratorUtils.getRandomUser();
        EquipmentListDAO.setEquipmentListItemForRun(em, run.getRun().getPk(), step.getPk(), data, user);

        // get the run from the database and assert there is one equipment item
        Run dbRun = JPAUtils.getRecordById(em, Run.class, run.getRun().getPk());
        assertEquals(1, dbRun.getEquipmentList().size());

        // get the step from the database and assert that there is one equipment item
        StepDef dbStep = JPAUtils.getRecordById(em, StepDef.class, step.getPk());
        assertEquals(1, dbStep.getEquipment().size());

        // assert that there is a history on the step
        assertEquals(1, dbStep.getHistories().size());
        History history = dbStep.getHistories().first();
        assertEquals(user.getUserId(), history.getUser().getUserId());

        // remove the equipment from the step but not the run
        EquipmentList equipmentList = dbStep.getEquipment().iterator().next();
        EquipmentListDAO.removeItemFromStep(em, equipmentList.getPk(), dbStep.getPk(), DataGeneratorUtils.getRandomUser());

        // get the run from the database and assert there is one unchanged equipment item
        dbRun = JPAUtils.getRecordById(em, Run.class, run.getRun().getPk());
        assertEquals(1, dbRun.getEquipmentList().size());
        assertEquals(equipmentList.getPk(), dbRun.getEquipmentList().iterator().next().getPk());

        // get the step from the database and assert that there are no equipment items
        dbStep = JPAUtils.getRecordById(em, StepDef.class, step.getPk());
        assertEquals(0, dbStep.getEquipment().size());

        // assert that the steps list for the equipment is empty
        equipmentList = JPAUtils.getRecordById(em, EquipmentList.class, equipmentList.getPk());
        assertEquals(0, equipmentList.getSteps().size());

        // assert that there are two run step histories on the step
        assertEquals(2, dbStep.getHistories().size());

        // assert that there are two run histories
        assertEquals(2, dbRun.getProcedureDetails().getHistories().size());
    }

    @Test
    public void removeEquipmentListFromRunAndStep()
    {
        // get a step from the run
        StepDef step = run.getAllSteps().stream().findFirst().get();

        // add equipment to the run
        EquipmentList data = createNewEquipment();
        Users user = DataGeneratorUtils.getRandomUser();
        EquipmentListDAO.setEquipmentListItemForRun(em, run.getRun().getPk(), step.getPk(), data, user);

        // get the run from the database and assert there is one equipment item
        Run dbRun = JPAUtils.getRecordById(em, Run.class, run.getRun().getPk());
        assertEquals(1, dbRun.getEquipmentList().size());

        // get the step from the database and assert that there is one equipment item
        StepDef dbStep = JPAUtils.getRecordById(em, StepDef.class, step.getPk());
        assertEquals(1, dbStep.getEquipment().size());

        // assert that there is a run step history on the step
        assertEquals(1, dbStep.getHistories().size());
        History history = dbStep.getHistories().first();
        assertEquals(user.getUserId(), history.getUser().getUserId());

        // delete the equipment
        EquipmentList equipmentList = dbStep.getEquipment().iterator().next();
        Users user2 = DataGeneratorUtils.getRandomUser();
        EquipmentListDAO.deleteEquipmentListItem(em, equipmentList.getPk(), user2);

        // assert that the equipment is no longer in database
        assertNull(JPAUtils.getRecordById(em, EquipmentList.class, equipmentList.getPk()));

        // get the run from the database and assert there are no equipment items.
        dbRun = JPAUtils.getRecordById(em, Run.class, run.getRun().getPk());
        assertEquals(0, dbRun.getEquipmentList().size());

        // get the step from the database and assert that there are no equipment items
        dbStep = JPAUtils.getRecordById(em, StepDef.class, step.getPk());
        assertEquals(0, dbStep.getEquipment().size());

        // assert that there are two run step histories on the step
        assertEquals(2, dbStep.getHistories().size());

        // check that there is a history object for the equipment deletion on the run
        List<History> histories = dbRun.getProcedureDetails().getHistories().stream()
                .filter(hist -> hist.getDescription().equalsIgnoreCase("Removed " + equipmentList.toString()))
                .collect(Collectors.toList());

        assertNotNull(histories);
        assertEquals(1, histories.size());
        assertEquals(user2.getUserId(), histories.get(0).getUser().getUserId());
        assertNotNull(histories.get(0).getTimestamp());
    }

    private EquipmentList createNewEquipment()
    {
        EquipmentList equipmentList = new EquipmentList();
        equipmentList.setName("TestEquipmentItem");
        equipmentList.setSerialNumber("11111");
        equipmentList.setPropertyNumber("22222");
        equipmentList.setCalibrationDate(new Date());
        equipmentList.setCalibrationDueDate(new Date(equipmentList.getCalibrationDate().getTime() + 86500000));
        return equipmentList;
    }

    private void assertEqualEquipmentItems(EquipmentList expected, EquipmentList actual)
    {
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getSerialNumber(), actual.getSerialNumber());
        assertEquals(expected.getPropertyNumber(), actual.getPropertyNumber());
        assertEquals(expected.getCalibrationDate(), actual.getCalibrationDate());
        assertEquals(expected.getCalibrationDueDate(), actual.getCalibrationDueDate());
    }
}
