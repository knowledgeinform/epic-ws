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

import edu.jhuapl.sd.sig.epic.data.ProcedureDetailsDAO;
import edu.jhuapl.sd.sig.epic.data.RunDAO;
import edu.jhuapl.sd.sig.epic.data.TestProcedureDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.*;
import edu.jhuapl.sd.sig.epic.resource.Runs;
import edu.jhuapl.sd.sig.epic.utils.DataGeneratorUtils;
import edu.jhuapl.sd.sig.epic.utils.DbTestContainer;
import edu.jhuapl.sd.sig.epic.utils.TestUtils;
import org.junit.jupiter.api.*;

import javax.persistence.EntityManager;
import javax.ws.rs.core.*;
import java.security.Principal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestRunOperations
{
    private final static List<Integer> procedurePksAddedToDb = new ArrayList<>();
    private static EntityManager em = null;
    private final Runs runEndpoint = new Runs();

    @Context
    SecurityContext sc;

    private static DbTestContainer container;

    @BeforeAll
    static void beforeAll() throws Exception
    {
        container = new DbTestContainer();
        container.start();
        em = JPAUtils.getEntityManager();

        DataGeneratorUtils.generateRandomUsers(3);
    }

    @AfterAll
    static void afterAll()
    {
        for (Integer pk : procedurePksAddedToDb)
        {
            TestProcedureDAO.deleteProcedureDef(pk);
        }
        container.stop();
    }

    @Test
    public void getRun_whenStepBlacklineExists_returnsProcedureChangeTypeOnComment() throws Exception
    {
        ProcedureDetails run = TestUtils.createCleanRun();
        StepDef step = run.getAllSteps().iterator().next();
        Users user = DataGeneratorUtils.getRandomUser();
        ProcedureChangeType changeType = DataGeneratorUtils.generateProcedureChangeType();

        BlackLineComment comment = new BlackLineComment();
        comment.setCommentTimestamp(new Date());
        comment.setCommentText("step blackline regression");
        comment.setCommentType(CommentType.BLACK_LINE_COMMENT);
        comment.setUsers(user);
        comment.setStepDef(step);
        comment.setProcedureChangeType(changeType);

        JPAUtils.basicTransaction(txEm ->
        {
            BlackLineComment managedComment = new BlackLineComment();
            managedComment.setCommentTimestamp(comment.getCommentTimestamp());
            managedComment.setCommentText(comment.getCommentText());
            managedComment.setCommentType(comment.getCommentType());
            managedComment.setUsers(txEm.find(Users.class, user.getUserId()));
            managedComment.setStepDef(txEm.find(step.getClass(), step.getPk()));
            managedComment.setProcedureChangeType(txEm.find(ProcedureChangeType.class, changeType.getPk()));
            txEm.persist(managedComment);
        }, "Failed to persist step blackline test data");

        Run loadedRun = runEndpoint.getRun(run.getId());
        StepDef loadedStep = loadedRun.getProcedureDetails().getAllSteps().stream()
                .filter(candidate -> candidate.getPk().equals(step.getPk()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected run step was not returned"));

        BlackLineComment loadedComment = loadedStep.getBlackLineComments().stream()
                .filter(savedComment -> "step blackline regression".equals(savedComment.getCommentText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected step blackline was not returned"));

        assertNotNull(loadedComment.getProcedureChangeType());
        assertEquals(changeType.getPk(), loadedComment.getProcedureChangeType().getPk());

        // Before adding initialization of nested step definitions, we got a lazy initialization error
        assertEquals(changeType.getName(), loadedComment.getProcedureChangeType().getName());
    }

    //    @Test
    //    public void testRunCreation() {
    //        for (Integer pk : procedurePksAddedToDb) {
    //            TestProcedureDAO.deleteProcedureDef(pk);
    //        }
    //
    //        // first create a procedure
    //        // create a procedure
    //        ProcedureDef procedureDef = TestProcedureDAO.createProcedure();
    //        assertNotNull(procedureDef.getPk());
    //        procedurePksAddedToDb.add(procedureDef.getPk());
    //
    //        // assert that only have one procedure details
    //        assertNotNull(procedureDef.getProcedureDetails());
    //        assertEquals(procedureDef.getProcedureDetails().size(), 1);
    //
    //        // get the first procedure details - we'll add step groups to it
    //        ProcedureDetails procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();
    //        assertNotNull(procedureDetails.getPk());
    //
    //        // it shouldn't currently have any step groups
    //        assertNull(procedureDetails.getStepGroupDefs());
    //
    //        // get some step group test data
    //        // this first set will be our top level groups
    //        SortedSet<StepGroupDef> testTopLevelGroups = TestProcedureStepGroupData.STEP_GROUP_SET_12.getTestData();
    //        StepGroupDef group1FromTest = testTopLevelGroups.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
    //        StepGroupDef group2FromTest = testTopLevelGroups.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();
    //
    //        // for each one, set to procedureDetails and save to database
    //        SortedSet<StepGroupDef> savedGroups = new TreeSet<>();
    //        for (StepGroupDef group : testTopLevelGroups) {
    //            group.setProcedureDetails(procedureDetails);
    //            group.setStepGroupDefParent(null);
    //            savedGroups.add(TestProcedureDAO.saveStepGroupDefWrapper(group));
    //        }
    //
    //        // query the database for the procedure
    //        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());
    //
    //        // get the procedureDetails
    //        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();
    //
    //        // it should have two step groups in the top level
    //        assertEquals(procedureDetails.getStepGroupDefs().size(), 2);
    //
    //        SortedSet<StepGroupDef> groupsFromDb = procedureDetails.getStepGroupDefs();
    //        StepGroupDef group1FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
    //
    //        // now let's add subgroups to group1FromDb.
    //        SortedSet<StepGroupDef> testSubGroup11 = TestProcedureStepGroupData.STEP_GROUP_SET_ABC.getTestData();
    //        StepGroupDef subgroupAFromTest = testSubGroup11.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
    //        StepGroupDef subgroupBFromTest = testSubGroup11.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();
    //        StepGroupDef subgroupCFromTest = testSubGroup11.stream().filter(g -> g.getDisplayOrder().equals(3)).findFirst().get();
    //
    //        // for each one, set to procedureDetails, parent to group1FromDb and save to database
    //        savedGroups = new TreeSet<>();
    //        for (StepGroupDef group : testSubGroup11) {
    //            group.setProcedureDetails(procedureDetails);
    //            group.setStepGroupDefParent(group1FromDb);
    //            savedGroups.add(TestProcedureDAO.saveStepGroupDefWrapper(group));
    //        }
    //
    //        // query the database for the procedure
    //        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());
    //
    //        // get the procedureDetails
    //        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();
    //
    //        // it should have two step groups in the top level
    //        assertEquals(procedureDetails.getStepGroupDefs().size(), 2);
    //
    //        groupsFromDb = procedureDetails.getStepGroupDefs();
    //        group1FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
    //
    //        // group1FromDb ought to have 3 subgroups
    //        assertNotNull(group1FromDb.getStepGroupDefsChildren());
    //        assertEquals(group1FromDb.getStepGroupDefsChildren().size(), 3);
    //
    //        SortedSet<StepGroupDef> subgroups11FromDb = group1FromDb.getStepGroupDefsChildren();
    //        StepGroupDef subgroup11FromDb = subgroups11FromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
    //        StepGroupDef subgroup12FromDb = subgroups11FromDb.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();
    //        StepGroupDef subgroup13FromDb = subgroups11FromDb.stream().filter(g -> g.getDisplayOrder().equals(3)).findFirst().get();
    //
    //        // now let's add steps
    //        // get some test step data for checkbox and single value steps
    //        SortedSet<StepDef> testStepDefs = TestProcedureStepData.TEST_STEP_DATA_SET_1.getTestData();
    //        StepDef testStep1 = testStepDefs.stream().filter(s -> s.getDisplayOrder().equals(1)).findFirst().get();
    //        StepDef testStep2 = testStepDefs.stream().filter(s -> s.getDisplayOrder().equals(2)).findFirst().get();
    //
    //        // we'll add these two steps to group1FromDb
    //        SortedSet<StepDef> savedSteps = new TreeSet<>();
    //        for (StepDef step : testStepDefs) {
    //            step.setStepGroupDef(group1FromDb);
    //            savedSteps.add(TestProcedureDAO.saveStepDefWrapper(step));
    //        }
    //
    //        // query the database for the procedure
    //        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());
    //
    //        // get the procedureDetails
    //        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();
    //
    //        // it should have two step groups in the top level
    //        assertEquals(procedureDetails.getStepGroupDefs().size(), 2);
    //
    //        groupsFromDb = procedureDetails.getStepGroupDefs();
    //        group1FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
    //
    //        // group1FromDb should have 2 steps
    //        assertEquals(group1FromDb.getStepDefs().size(), 2);
    //
    //        SortedSet<StepDef> stepsFromDb = group1FromDb.getStepDefs();
    //        StepDef step1FromDb = stepsFromDb.stream().filter(s -> s.getDisplayOrder().equals(1)).findFirst().get();
    //        StepDef step2FromDb = stepsFromDb.stream().filter(s -> s.getDisplayOrder().equals(2)).findFirst().get();
    //
    //        // let's add a table step to subgroup12FromDb
    //        SortedSet<StepDef> testStepData = TestProcedureStepData.TEST_STEP_DATA_SET_2.getTestData();
    //        StepDef testStepTable1 = testStepData.stream().filter(s -> s.getDisplayOrder().equals(1)).findFirst().get();
    //
    //        // we'll add this step to subgroup12FromDb
    //        savedSteps = new TreeSet<>();
    //        for (StepDef step : testStepData) {
    //            step.setStepGroupDef(subgroup12FromDb);
    //            savedSteps.add(TestProcedureDAO.saveStepDefWrapper(step));
    //        }
    //
    //        // query the database for the procedure
    //        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());
    //
    //        // get the procedureDetails
    //        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();
    //
    //        // it should have two step groups in the top level
    //        assertEquals(procedureDetails.getStepGroupDefs().size(), 2);
    //
    //        groupsFromDb = procedureDetails.getStepGroupDefs();
    //        group1FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
    //
    //        // group1FromDb ought to have 3 subgroups
    //        assertNotNull(group1FromDb.getStepGroupDefsChildren());
    //        assertEquals(group1FromDb.getStepGroupDefsChildren().size(), 3);
    //
    //        subgroups11FromDb = group1FromDb.getStepGroupDefsChildren();
    //        subgroup12FromDb = subgroups11FromDb.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();
    //
    //        // subgroup12FromDb should have a step
    //        assertNotNull(subgroup12FromDb.getStepDefs());
    //        assertEquals(subgroup12FromDb.getStepDefs().size(), 1);
    //
    //        // steps and step groups are added. Now add procedure instructions
    //        // get some instruction test data
    //        SortedSet<ProcedureInstruction> testInstructions = TestProcedureInstructionData.PROCEDURE_INSTRUCTION_SET_1.getTestData();
    //
    //        // go through and set the procedure details for each instruction, then save each instruction to the database
    //        for (ProcedureInstruction instruction : testInstructions) {
    //            instruction.setProcedureDetails(procedureDetails);
    //            TestProcedureDAO.saveProcedureInstructionSectionWrapper(instruction);
    //        }
    //
    //        ProcedureInstruction testInstruction1 = testInstructions.stream().filter(o -> o.getDisplayOrder().equals(1)).findFirst().get();
    //        ProcedureInstruction testInstruction2 = testInstructions.stream().filter(o -> o.getDisplayOrder().equals(2)).findFirst().get();
    //        ProcedureInstruction testInstruction3 = testInstructions.stream().filter(o -> o.getDisplayOrder().equals(3)).findFirst().get();
    //
    //        // query the database for this procedure
    //        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());
    //
    //        // get the procedureDetails
    //        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();
    //
    //        // it should have three instructions
    //        assertEquals(procedureDetails.getProcedureInstructions().size(), 3);
    //
    //        // now get procedure approvals
    //        ProcedureApproval procedureApproval = TestProcedureApprovalData.PROCEDURE_APPROVAL_1.getTestData();
    //        procedureApproval.setProcedureDetails(procedureDetails);
    //
    //        ProcedureApproval procedureApprovalInDb = TestProcedureDAO.saveProcedureApprovalWrapper(procedureApproval);
    //        TestProcedureDAO.transitionToWaitingWrapper(procedureApprovalInDb, procedureApproval.getProcedureDetails().getProcedureApprovalDueDate().getTime());
    //        procedureApprovalInDb = TestProcedureDAO.setApprovalFlagWrapper(procedureApprovalInDb, procedureApproval.getIsApproved());
    //
    //        // get the procedure from the database
    //        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());
    //
    //        // ought to have one procedure details
    //        assertEquals(procedureDef.getProcedureDetails().size(), 1);
    //
    //        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();
    //
    //        // should have an approval
    //        assertNotNull(procedureDetails.getProcedureApprovals());
    //        assertEquals(procedureDetails.getProcedureApprovals().size(), 1);
    //
    //
    //        // now we have a full procedure in the database.
    //        // our procedure has 1 approval, 3 instructions
    //        // two top-level level groups - 1st group contains two steps and three subgroups
    //        // the second of the three subgroups has one step
    //
    //        // now let's create a run
    //        Users user = procedureDetails.getProcedureHeader().getUser();
    //        Integer runNumber = 1;
    //        Run runData = new Run();
    //        runData.setProcedureDetails(procedureDetails);
    //        runData.setName("Unit Test Run Creation");
    //        runData.setDescription("This is a unit test of a created run");
    //        runData.setTestingPhase(TestProcedureDAO.getAllTestingPhases().get(0));
    //        ProcedureDetails run = TestProcedureDAO.createRunWrapper(procedureDetails, runData, runNumber, user);
    //
    //        // get the procedure from the database
    //        // get the procedure from the database
    //        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());
    //
    //        // ought to have two procedure details
    //        assertEquals(procedureDef.getProcedureDetails().size(), 2);
    //
    //        // the two should be one original procedure details and one run.
    //        procedureDetails = procedureDef.getProcedureDetails().stream().filter(p -> p.getEditType().equals(EditType.ORIGINAL)).findFirst().get();
    //        run = procedureDef.getProcedureDetails().stream().filter(p -> p.getEditType().equals(EditType.RUN)).findFirst().get();
    //
    //        assertNotNull(procedureDetails);
    //        assertNotNull(run);
    //
    //        // they should have different pks
    //        assertNotNull(procedureDetails.getPk());
    //        assertNotNull(run.getPk());
    //        assertNotEquals(procedureDetails.getPk(), run.getPk());
    //
    //        // the run should be pointing to the original procedure details.
    //        assertEquals(run.getOriginalProcedureDetails().getPk(), procedureDetails.getPk());
    //
    //        // the original procedure details should point to the run
    //        assertNotNull(procedureDetails.getProcedureDetailRuns());
    //        assertEquals(procedureDetails.getProcedureDetailRuns().size(), 1);
    //        assertEquals(procedureDetails.getProcedureDetailRuns().get(0).getPk(), run.getPk());
    //
    //        // the run should have a run number
    //        assertNotNull(run.getRunNumber());
    //        assertEquals(run.getRunNumber(), runNumber);
    //
    //        // check the run data
    //        Run createdRunData = run.getRun();
    //        assertNotNull(createdRunData.getPk());
    //        assertEquals(createdRunData.getTestingPhase().getPk(), runData.getTestingPhase().getPk());
    //        assertEquals(createdRunData.getDescription(), runData.getDescription());
    //        assertEquals(createdRunData.getName(), runData.getName());
    //        assertEquals(createdRunData.getStatus(), RunStatus.RUNNING);
    //        assertEquals(createdRunData.getUser().getUserId(), user.getUserId());
    //
    //        // the run should have the same procedure header, instructions, steps, step groups, and approvals but with different pks
    //
    //        // procedure header user should match test data, other fields not equal to procedureDetails.getHeader
    //        ProcedureHeader clonedHeader = run.getProcedureHeader();
    //        assertEquals(clonedHeader.getUser().getUserId(), user.getUserId());
    //        assertEquals(clonedHeader.getCreationDate(), procedureDetails.getProcedureHeader().getCreationDate());
    //        assertNotEquals(clonedHeader.getPk(), procedureDetails.getProcedureHeader().getPk());
    //
    //        // there should not be approvals on the run
    //        assertNotNull(procedureDetails.getProcedureApprovals());
    //        assertEquals(procedureDetails.getProcedureApprovals().size(), 1);
    //
    //        assertEquals(run.getProcedureApprovals().size(), 0);
    //
    //        // check procedure instructions
    //        // run should have three instructions
    //        assertEquals(run.getProcedureInstructions().size(), 3);
    //
    //        SortedSet<ProcedureInstruction> procedureInstructions = procedureDetails.getProcedureInstructions();
    //        ProcedureInstruction procedureInstruction1 = procedureInstructions.stream().filter(o -> o.getDisplayOrder().equals(1)).findFirst().get();
    //        ProcedureInstruction procedureInstruction2 = procedureInstructions.stream().filter(o -> o.getDisplayOrder().equals(2)).findFirst().get();
    //        ProcedureInstruction procedureInstruction3 = procedureInstructions.stream().filter(o -> o.getDisplayOrder().equals(3)).findFirst().get();
    //
    //        SortedSet<ProcedureInstruction> clonedInstructions = run.getProcedureInstructions();
    //        ProcedureInstruction cloneInstruction1 = clonedInstructions.stream().filter(o -> o.getDisplayOrder().equals(1)).findFirst().get();
    //        ProcedureInstruction cloneInstruction2 = clonedInstructions.stream().filter(o -> o.getDisplayOrder().equals(2)).findFirst().get();
    //        ProcedureInstruction cloneInstruction3 = clonedInstructions.stream().filter(o -> o.getDisplayOrder().equals(3)).findFirst().get();
    //
    //        assertNotNull(cloneInstruction1.getPk());
    //        assertNotEquals(cloneInstruction1.getPk(), procedureInstruction1.getPk());
    //        assertEquals(procedureInstruction1.getSectionName(), cloneInstruction1.getSectionName());
    //        assertEquals(procedureInstruction1.getDisplayOrder(), cloneInstruction1.getDisplayOrder());
    //        assertEquals(cloneInstruction1.getEditType(), EditType.RUN);
    //        assertEquals(procedureInstruction1.getText(), cloneInstruction1.getText());
    //
    //        assertNotNull(cloneInstruction2.getPk());
    //        assertNotEquals(cloneInstruction2.getPk(), procedureInstruction2.getPk());
    //        assertEquals(procedureInstruction2.getSectionName(), cloneInstruction2.getSectionName());
    //        assertEquals(procedureInstruction2.getDisplayOrder(), cloneInstruction2.getDisplayOrder());
    //        assertEquals(cloneInstruction2.getEditType(), EditType.RUN);
    //        assertEquals(procedureInstruction2.getText(), cloneInstruction2.getText());
    //
    //        assertNotNull(procedureInstruction3.getPk());
    //        assertNotEquals(cloneInstruction3.getPk(), procedureInstruction3.getPk());
    //        assertEquals(procedureInstruction3.getSectionName(), cloneInstruction3.getSectionName());
    //        assertEquals(procedureInstruction3.getDisplayOrder(), cloneInstruction3.getDisplayOrder());
    //        assertEquals(cloneInstruction3.getEditType(), EditType.RUN);
    //        assertEquals(procedureInstruction3.getText(), cloneInstruction3.getText());
    //
    //
    //        // now check the step groups and steps
    //        // run should have two top level groups
    //        assertEquals(run.getStepGroupDefs().size(), 2);
    //
    //        groupsFromDb = procedureDetails.getStepGroupDefs();
    //        group1FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
    //        StepGroupDef group2FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();
    //
    //        SortedSet<StepGroupDef> topLevelGroupsFromClone = run.getStepGroupDefs();
    //        StepGroupDef group1FromClone = topLevelGroupsFromClone.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
    //        StepGroupDef group2FromClone = topLevelGroupsFromClone.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();
    //
    //        // compare the top level groups
    //        assertNotNull(group1FromClone.getPk());
    //        assertNotEquals(group1FromClone.getPk(), group1FromDb.getPk());
    //        assertNull(group1FromClone.getStepGroupDefParent());
    //        // first group should have three subgroups
    //        assertNotNull(group1FromDb.getStepGroupDefsChildren());
    //        assertEquals(group1FromDb.getStepGroupDefsChildren().size(), 3);
    //
    //        assertNotNull(group1FromClone.getStepGroupDefsChildren());
    //        assertEquals(group1FromClone.getStepGroupDefsChildren().size(), 3);
    //        // first group should have two steps
    //        assertEquals(group1FromClone.getStepDefs().size(), 2);
    //        assertEquals(group1FromDb.getDisplayOrder(), group1FromClone.getDisplayOrder());
    //        assertEquals(group1FromDb.getStepGroupName(), group1FromClone.getStepGroupName());
    //	    assertEquals(group1FromDb.getDescription(), group1FromClone.getDescription());
    //        assertEquals(group1FromClone.getEditType(), EditType.RUN);
    //
    //        assertNotNull(group2FromClone.getPk());
    //        assertNotEquals(group2FromClone.getPk(), group2FromDb.getPk());
    //        assertNull(group2FromClone.getStepGroupDefParent());
    //        assertEquals(group2FromClone.getStepGroupDefsChildren().size(), 0);
    //        assertEquals(group2FromClone.getStepDefs().size(), 0);
    //        assertEquals(group2FromClone.getDisplayOrder(), group2FromDb.getDisplayOrder());
    //        assertEquals(group2FromClone.getStepGroupName(), group2FromDb.getStepGroupName());
    //	    assertEquals(group2FromClone.getDescription(), group2FromDb.getDescription());
    //        assertEquals(group2FromClone.getEditType(), EditType.RUN);
    //
    //        // go back to group 1 and compare subgroups and steps
    //        // start with steps
    //        SortedSet<StepDef> stepsFromGroup1Db = group1FromDb.getStepDefs();
    //        StepDef step1FromGroup1Db = stepsFromGroup1Db.stream().filter(s -> s.getDisplayOrder().equals(1)).findFirst().get();
    //        StepDef step2FromGroup1Db = stepsFromGroup1Db.stream().filter(s -> s.getDisplayOrder().equals(2)).findFirst().get();
    //
    //        SortedSet<StepDef> stepsFromGroup1Clone = group1FromClone.getStepDefs();
    //        StepDef step1FromGroup1Clone = stepsFromGroup1Clone.stream().filter(s -> s.getDisplayOrder().equals(1)).findFirst().get();
    //        StepDef step2FromGroup1Clone = stepsFromGroup1Clone.stream().filter(s -> s.getDisplayOrder().equals(2)).findFirst().get();
    //
    //        // steps should have unique PKs, a step group parent, and their fields should match . All the run related
    //        // fields should be empty (for sets) or null
    //        assertNotNull(step1FromGroup1Clone.getPk());
    //        assertNotEquals(step1FromGroup1Clone.getPk(), step1FromGroup1Db.getPk());
    //        assertEquals(step1FromGroup1Clone.getType(), step1FromGroup1Db.getType());
    //        assertEquals(step1FromGroup1Clone.getStepGroupDef().getPk(), group1FromClone.getPk());
    //        assertEquals(step1FromGroup1Clone.getEditType(), EditType.RUN);
    //        assertEquals(step1FromGroup1Clone.getAllowEquipmentEntry(), step1FromGroup1Db.getAllowEquipmentEntry());
    //        assertEquals(step1FromGroup1Clone.getDisplayOrder(), step1FromGroup1Db.getDisplayOrder());
    //        assertEquals(step1FromGroup1Clone.getEsd0(), step1FromGroup1Db.getEsd0());
    //        assertEquals(step1FromGroup1Clone.getHazardous(), step1FromGroup1Db.getHazardous());
    //        assertEquals(step1FromGroup1Clone.getRequireWitness(), step1FromGroup1Db.getRequireWitness());
    //        assertEquals(step1FromGroup1Clone.getMandatoryInspection(), step1FromGroup1Db.getMandatoryInspection());
    //        assertEquals(step1FromGroup1Clone.getStepName(), step1FromGroup1Db.getStepName());
    //        assertEquals(step1FromGroup1Clone.getInstructions(), step1FromGroup1Db.getInstructions());
    //        assertEquals(step1FromGroup1Clone.getRunStepComments().size(), 0);
    //        assertEquals(step1FromGroup1Clone.getBlackLineComments().size(), 0);
    //        assertEquals(step1FromGroup1Clone.getEquipment().size(), 0);
    //        assertEquals(step1FromGroup1Clone.getHistories().size(), 0);
    //        assertFalse(step1FromGroup1Clone.getIsManualValidation());
    //        assertNull(step1FromGroup1Clone.getMandatoryInspectionSecondSignature());
    //        assertNull(step1FromGroup1Clone.getWitnessSecondSignature());
    //        assertNull(step1FromGroup1Clone.getRunValueEntryUser());
    //        assertNull(step1FromGroup1Clone.getRunValueSavedTimestamp());
    //
    //        assertNotNull(step2FromGroup1Clone.getPk());
    //        assertNotEquals(step2FromGroup1Clone.getPk(), step2FromGroup1Db.getPk());
    //        assertEquals(step2FromGroup1Clone.getType(), step2FromGroup1Db.getType());
    //        assertEquals(step2FromGroup1Clone.getStepGroupDef().getPk(), group1FromClone.getPk());
    //        assertEquals(step2FromGroup1Clone.getEditType(), EditType.RUN);
    //        assertEquals(step2FromGroup1Clone.getAllowEquipmentEntry(), step2FromGroup1Db.getAllowEquipmentEntry());
    //        assertEquals(step2FromGroup1Clone.getDisplayOrder(), step2FromGroup1Db.getDisplayOrder());
    //        assertEquals(step2FromGroup1Clone.getEsd0(), step2FromGroup1Db.getEsd0());
    //        assertEquals(step2FromGroup1Clone.getHazardous(), step2FromGroup1Db.getHazardous());
    //        assertEquals(step2FromGroup1Clone.getRequireWitness(), step2FromGroup1Db.getRequireWitness());
    //        assertEquals(step2FromGroup1Clone.getMandatoryInspection(), step2FromGroup1Db.getMandatoryInspection());
    //        assertEquals(step2FromGroup1Clone.getStepName(), step2FromGroup1Db.getStepName());
    //        assertEquals(step2FromGroup1Clone.getInstructions(), step2FromGroup1Db.getInstructions());
    //        assertEquals(step2FromGroup1Clone.getRunStepComments().size(), 0);
    //        assertEquals(step2FromGroup1Clone.getBlackLineComments().size(), 0);
    //        assertEquals(step2FromGroup1Clone.getEquipment().size(), 0);
    //        assertEquals(step2FromGroup1Clone.getHistories().size(), 0);
    //        assertFalse(step2FromGroup1Clone.getIsManualValidation());
    //        assertNull(step2FromGroup1Clone.getMandatoryInspectionSecondSignature());
    //        assertNull(step2FromGroup1Clone.getWitnessSecondSignature());
    //        assertNull(step2FromGroup1Clone.getRunValueEntryUser());
    //        assertNull(step2FromGroup1Clone.getRunValueSavedTimestamp());
    //
    //        // group1FromClone ought to have 3 subgroups
    //        assertNotNull(group1FromClone.getStepGroupDefsChildren());
    //        assertEquals(group1FromClone.getStepGroupDefsChildren().size(), 3);
    //
    //        subgroups11FromDb = group1FromDb.getStepGroupDefsChildren();
    //        subgroup11FromDb = subgroups11FromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
    //        subgroup12FromDb = subgroups11FromDb.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();
    //        subgroup13FromDb = subgroups11FromDb.stream().filter(g -> g.getDisplayOrder().equals(3)).findFirst().get();
    //
    //        SortedSet<StepGroupDef> subgroupsFromGroup1Clone = group1FromClone.getStepGroupDefsChildren();
    //        StepGroupDef subgroup11FromClone = subgroupsFromGroup1Clone.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
    //        StepGroupDef subgroup12FromClone = subgroupsFromGroup1Clone.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();
    //        StepGroupDef subgroup13FromClone = subgroupsFromGroup1Clone.stream().filter(g -> g.getDisplayOrder().equals(3)).findFirst().get();
    //
    //        // compare the subgroups
    //        assertNotNull(subgroup11FromClone.getPk());
    //        assertNotEquals(subgroup11FromClone.getPk(), subgroup11FromDb.getPk());
    //        assertNotNull(subgroup11FromClone.getStepGroupDefParent());
    //        assertEquals(subgroup11FromClone.getStepGroupDefParent().getPk(), group1FromClone.getPk());
    //        // subgroup11FromClone has neither subgroups nor children
    //        assertEquals(subgroup11FromClone.getStepGroupDefsChildren().size(), 0);
    //        assertEquals(subgroup11FromClone.getStepDefs().size(), 0);
    //        assertEquals(subgroup11FromClone.getStepGroupName(), subgroup11FromDb.getStepGroupName());
    //	    assertEquals(subgroup11FromClone.getDescription(), subgroup11FromDb.getDescription());
    //        assertEquals(subgroup11FromDb.getDisplayOrder(), subgroup11FromClone.getDisplayOrder());
    //        assertEquals(subgroup11FromClone.getEditType(), EditType.RUN);
    //
    //        assertNotNull(subgroup12FromClone.getPk());
    //        assertNotEquals(subgroup12FromClone.getPk(), subgroup12FromDb.getPk());
    //        assertNotNull(subgroup12FromClone.getStepGroupDefParent());
    //        assertEquals(subgroup12FromClone.getStepGroupDefParent().getPk(), group1FromClone.getPk());
    //        // subgroup12FromClone has one step and no subgroups
    //        assertEquals(subgroup12FromClone.getStepGroupDefsChildren().size(), 0);
    //        assertEquals(subgroup12FromClone.getStepDefs().size(), 1);
    //        assertEquals(subgroup12FromClone.getStepGroupName(), subgroup12FromDb.getStepGroupName());
    //	    assertEquals(subgroup12FromClone.getDescription(), subgroup12FromDb.getDescription());
    //        assertEquals(subgroup12FromDb.getDisplayOrder(), subgroup12FromClone.getDisplayOrder());
    //        assertEquals(subgroup12FromClone.getEditType(), EditType.RUN);
    //
    //        assertNotNull(subgroup13FromClone.getPk());
    //        assertNotEquals(subgroup13FromClone.getPk(), subgroup13FromDb.getPk());
    //        assertNotNull(subgroup13FromClone.getStepGroupDefParent());
    //        assertEquals(subgroup13FromClone.getStepGroupDefParent().getPk(), group1FromClone.getPk());
    //        // subgroup13FromClone has neither subgroups nor children
    //        assertEquals(subgroup13FromClone.getStepGroupDefsChildren().size(), 0);
    //        assertEquals(subgroup13FromClone.getStepDefs().size(), 0);
    //        assertEquals(subgroup13FromClone.getStepGroupName(), subgroup13FromDb.getStepGroupName());
    //	    assertEquals(subgroup13FromClone.getDescription(), subgroup13FromDb.getDescription());
    //        assertEquals(subgroup13FromClone.getDisplayOrder(), subgroup13FromDb.getDisplayOrder());
    //        assertEquals(subgroup13FromClone.getEditType(), EditType.RUN);
    //
    //        // finally, subgroup 2 should have one table step - check that
    //        stepsFromDb = subgroup12FromDb.getStepDefs();
    //        StepDef step1FromSubgroup12FromDb = stepsFromDb.stream().filter(s -> s.getDisplayOrder().equals(1)).findFirst().get();
    //
    //        SortedSet<StepDef> stepsFromSubgroup12Clone = subgroup12FromClone.getStepDefs();
    //        StepDef step1FromSubgroup12Clone = stepsFromSubgroup12Clone.stream().filter(s -> s.getDisplayOrder().equals(1)).findFirst().get();
    //
    //        // steps should have unique PK, a step group parent, and their fields should match. All the run related
    //        // fields should be empty (for sets) or null
    //        assertNotNull(step1FromSubgroup12Clone.getPk());
    //        assertNotEquals(step1FromSubgroup12Clone.getPk(), step1FromSubgroup12FromDb.getPk());
    //        assertEquals(step1FromSubgroup12Clone.getType(), step1FromSubgroup12FromDb.getType());
    //        assertEquals(step1FromSubgroup12Clone.getStepGroupDef().getPk(), subgroup12FromClone.getPk());
    //        assertEquals(step1FromSubgroup12Clone.getEditType(), EditType.RUN);
    //        assertEquals(step1FromSubgroup12Clone.getAllowEquipmentEntry(), step1FromSubgroup12FromDb.getAllowEquipmentEntry());
    //        assertEquals(step1FromSubgroup12Clone.getDisplayOrder(), step1FromSubgroup12FromDb.getDisplayOrder());
    //        assertEquals(step1FromSubgroup12Clone.getEsd0(), step1FromSubgroup12FromDb.getEsd0());
    //        assertEquals(step1FromSubgroup12Clone.getHazardous(), step1FromSubgroup12FromDb.getHazardous());
    //        assertEquals(step1FromSubgroup12Clone.getRequireWitness(), step1FromSubgroup12FromDb.getRequireWitness());
    //        assertEquals(step1FromSubgroup12Clone.getMandatoryInspection(), step1FromSubgroup12FromDb.getMandatoryInspection());
    //        assertEquals(step1FromSubgroup12Clone.getStepName(), step1FromSubgroup12FromDb.getStepName());
    //        assertEquals(step1FromSubgroup12Clone.getInstructions(), step1FromSubgroup12FromDb.getInstructions());
    //        assertEquals(step1FromSubgroup12Clone.getRunStepComments().size(), 0);
    //        assertEquals(step1FromSubgroup12Clone.getBlackLineComments().size(), 0);
    //        assertEquals(step1FromSubgroup12Clone.getEquipment().size(), 0);
    //        assertEquals(step1FromSubgroup12Clone.getHistories().size(), 0);
    //        assertFalse(step1FromSubgroup12Clone.getIsManualValidation());
    //        assertNull(step1FromSubgroup12Clone.getMandatoryInspectionSecondSignature());
    //        assertNull(step1FromSubgroup12Clone.getWitnessSecondSignature());
    //        assertNull(step1FromSubgroup12Clone.getRunValueEntryUser());
    //        assertNull(step1FromSubgroup12Clone.getRunValueSavedTimestamp());
    //
    //        // check the step's rows and cells
    //        StepTable stepTableFromDb = (StepTable) step1FromSubgroup12FromDb;
    //        StepTable stepTableFromClone = (StepTable) step1FromSubgroup12Clone;
    //
    //        // table should have two rows
    //        assertNotNull(stepTableFromClone.getStepTableRows());
    //        assertEquals(stepTableFromClone.getStepTableRows().size(), 2);
    //
    //        SortedSet<StepTableRow> rowsFromDb = stepTableFromDb.getStepTableRows();
    //        StepTableRow row1Db = rowsFromDb.first();
    //        StepTableRow row2Db = rowsFromDb.last();
    //
    //        SortedSet<StepTableRow> rowsFromClone = stepTableFromClone.getStepTableRows();
    //        StepTableRow row1Clone = rowsFromClone.first();
    //        StepTableRow row2Clone = rowsFromClone.last();
    //
    //        // checking first row
    //        assertNotNull(row1Clone.getPk());
    //        assertNotEquals(row1Clone.getPk(), row1Db.getPk());
    //        assertEquals(row1Clone.getRowNumber(), row1Db.getRowNumber());
    //        // row one should have two cells
    //        assertNotNull(row1Clone.getStepTableCells());
    //        assertEquals(row1Clone.getStepTableCells().size(), 2);
    //
    //        SortedSet<StepTableCell> cellsClone = row1Clone.getStepTableCells();
    //        StepTableCell cell1Clone = cellsClone.first();
    //        StepTableCell cell2Clone = cellsClone.last();
    //
    //        SortedSet<StepTableCell> cellsDb = row1Db.getStepTableCells();
    //        StepTableCell cell1Db = cellsDb.first();
    //        StepTableCell cell2Db = cellsDb.last();
    //
    //        assertNotNull(cell1Clone.getPk());
    //        assertNotEquals(cell1Clone.getPk(), cell1Db.getPk());
    //        assertEquals(cell1Clone.getNonEditableValue(), cell1Db.getNonEditableValue());
    //        assertEquals(cell1Clone.getEditable(), cell1Db.getEditable());
    //        assertEquals(cell1Clone.getCellIndex(), cell1Db.getCellIndex());
    //
    //        assertNotNull(cell2Clone.getPk());
    //        assertNotEquals(cell2Clone.getPk(), cell2Db.getPk());
    //        assertEquals(cell2Clone.getNonEditableValue(), cell2Db.getNonEditableValue());
    //        assertEquals(cell2Clone.getEditable(), cell2Db.getEditable());
    //        assertEquals(cell2Clone.getCellIndex(), cell2Db.getCellIndex());
    //
    //        // checking second row
    //        assertNotNull(row2Clone.getPk());
    //        assertNotEquals(row2Clone.getPk(), row2Db.getPk());
    //        assertEquals(row2Clone.getRowNumber(), row2Db.getRowNumber());
    //        // row two should have two cells
    //        assertNotNull(row2Clone.getStepTableCells());
    //        assertEquals(row2Clone.getStepTableCells().size(), 2);
    //
    //        cellsClone = row2Clone.getStepTableCells();
    //        cell1Clone = cellsClone.first();
    //        cell2Clone = cellsClone.last();
    //
    //        cellsDb = row2Db.getStepTableCells();
    //        cell1Db = cellsDb.first();
    //        cell2Db = cellsDb.last();
    //
    //        assertNotNull(cell1Clone.getPk());
    //        assertNotEquals(cell1Clone.getPk(), cell1Db.getPk());
    //        assertEquals(cell1Clone.getNonEditableValue(), cell1Db.getNonEditableValue());
    //        assertEquals(cell1Clone.getEditable(), cell1Db.getEditable());
    //        assertEquals(cell1Clone.getCellIndex(), cell1Db.getCellIndex());
    //
    //        assertNotNull(cell2Clone.getPk());
    //        assertNotEquals(cell2Clone.getPk(), cell2Db.getPk());
    //        assertEquals(cell2Clone.getNonEditableValue(), cell2Db.getNonEditableValue());
    //        assertEquals(cell2Clone.getEditable(), cell2Db.getEditable());
    //        assertEquals(cell2Clone.getCellIndex(), cell2Db.getCellIndex());
    //
    //        // done!
    //
    //
    //        // now delete this procedure from the database to set up for next test
    //        Integer procedurePk = procedureDef.getPk();
    //        TestProcedureDAO.deleteProcedureDef(procedurePk);
    //
    //        // confirm deletion
    //        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedurePk);
    //        assertNull(procedureDef);
    //
    //        procedurePksAddedToDb.remove(procedurePk);
    //    }

    @Test
    public void testThatNonAdminCanTransitionRunToCorrectingWhenNotAdminOnlyAction() throws Exception
    {
        ProcedureDef procedureDef = DataGeneratorUtils.generateProcedureDef(3, 1, 1, 3);
        ProcedureDetails procedureDetails = procedureDef.getProcedureDetails().iterator().next();
        procedureDetails = ProcedureDetailsDAO.transitionToReadyForRelease(em, procedureDetails.getPk(), procedureDetails.getProcedureHeader().getUser());
        Run run = DataGeneratorUtils.generateRuns(1, 1, procedureDetails).get(0).getRun();
        assertNotNull(run);

        // get a random user, make sure they are not an admin, and set security context.

        Users submitter = DataGeneratorUtils.getRandomUser();
        submitter.setIsAdmin(false);
        em.getTransaction().begin();
        em.merge(submitter);
        em.getTransaction().commit();

        runEndpoint.setSc(setSecurityContext(submitter));

        // set the closeout submission fields
        run.setCloseoutSubmissionUser(submitter);
        run.setCloseoutSubmittedDate(new Date());

        // transition run to REVIEWING status
        run = RunDAO.transitionRunToReviewing(em, run.getPk(), submitter);

        // assert that run is in approved status
        assertEquals(RunStatus.APPROVED, run.getStatus());

        // transition the run to correcting status
        runEndpoint.transitionRunToCorrecting(run.getPk(), false);
        run = JPAUtils.getRecordById(em, Run.class, run.getPk());
        em.refresh(run);

        // assert that run is now in correcting status
        assertEquals(RunStatus.CORRECTING, run.getStatus());
    }

    @Test
    public void testThatAdminCanTransitionRunToCorrectingWhenNotAdminOnlyAction() throws Exception
    {
        ProcedureDef procedureDef = DataGeneratorUtils.generateProcedureDef(3, 1, 1, 3);
        ProcedureDetails procedureDetails = procedureDef.getProcedureDetails().iterator().next();
        procedureDetails = ProcedureDetailsDAO.transitionToReadyForRelease(em, procedureDetails.getPk(), procedureDetails.getProcedureHeader().getUser());
        Run run = DataGeneratorUtils.generateRuns(1, 1, procedureDetails).get(0).getRun();
        assertNotNull(run);

        // get a random user, make sure they are not an admin, and set security context.
        Users submitter = DataGeneratorUtils.getRandomUser();
        submitter.setIsAdmin(false);
        em.getTransaction().begin();
        em.merge(submitter);
        em.getTransaction().commit();

        runEndpoint.setSc(setSecurityContext(submitter));

        // set the closeout submission fields
        run.setCloseoutSubmissionUser(submitter);
        run.setCloseoutSubmittedDate(new Date());

        // transition run to REVIEWING status
        run = RunDAO.transitionRunToReviewing(em, run.getPk(), submitter);

        // assert that run is in reviewing status
        assertEquals(RunStatus.APPROVED, run.getStatus());

        // now we want to test that an admin (a different user) can transition to correcting status
        Users admin = DataGeneratorUtils.getRandomUser();
        while (admin.getUserId() == submitter.getUserId())
        {
            admin = DataGeneratorUtils.getRandomUser();
        }
        admin.setIsAdmin(true);
        em.getTransaction().begin();
        em.merge(admin);
        em.getTransaction().commit();

        runEndpoint.setSc(setSecurityContext(admin));

        // transition the run to correcting status
        runEndpoint.transitionRunToCorrecting(run.getPk(), false);
        run = JPAUtils.getRecordById(em, Run.class, run.getPk());
        em.refresh(run);

        // assert that run is now in correcting status
        assertEquals(RunStatus.CORRECTING, run.getStatus());
    }

    @Test
    public void testThatNonAdminCannotTransitionRunToCorrectingWhenAdminOnlyAction() throws Exception
    {
        ProcedureDef procedureDef = DataGeneratorUtils.generateProcedureDef(3, 1, 1, 3);
        ProcedureDetails procedureDetails = procedureDef.getProcedureDetails().iterator().next();
        procedureDetails = ProcedureDetailsDAO.transitionToReadyForRelease(em, procedureDetails.getPk(), procedureDetails.getProcedureHeader().getUser());
        Run run = DataGeneratorUtils.generateRuns(1, 1, procedureDetails).get(0).getRun();
        assertNotNull(run);

        // get a random user, make sure they are not an admin, and set security context.
        Users submitter = DataGeneratorUtils.getRandomUser();
        submitter.setIsAdmin(false);
        em.getTransaction().begin();
        em.merge(submitter);
        em.getTransaction().commit();

        runEndpoint.setSc(setSecurityContext(submitter));

        // set the closeout submission fields
        run.setCloseoutSubmissionUser(submitter);
        run.setCloseoutSubmittedDate(new Date());

        // transition run to approved status
        run = RunDAO.transitionRunToReviewing(em, run.getPk(), submitter);

        // assert that run is in approved status, since no approvers were selected
        assertEquals(RunStatus.APPROVED, run.getStatus());

        // transition the run to correcting status
        Response response = runEndpoint.transitionRunToCorrecting(run.getPk(), true);
        run = JPAUtils.getRecordById(em, Run.class, run.getPk());
        em.refresh(run);

        // assert that run is still in approved status
        assertEquals(RunStatus.APPROVED, run.getStatus());

        // assert that the response is an error.
        assertTrue(response.getEntity().toString().startsWith("{\"error\":"));
    }

    @Test
    public void testThatAdminCanTransitionRunToCorrectingWhenAdminOnlyAction() throws Exception
    {
        ProcedureDef procedureDef = DataGeneratorUtils.generateProcedureDef(3, 1, 1, 3);
        ProcedureDetails procedureDetails = procedureDef.getProcedureDetails().iterator().next();
        procedureDetails = ProcedureDetailsDAO.transitionToReadyForRelease(em, procedureDetails.getPk(), procedureDetails.getProcedureHeader().getUser());
        Run run = DataGeneratorUtils.generateRuns(1, 1, procedureDetails).get(0).getRun();
        assertNotNull(run);

        // get a random user, make sure they are not an admin, and set security context.
        Users submitter = DataGeneratorUtils.getRandomUser();
        submitter.setIsAdmin(false);
        em.getTransaction().begin();
        em.merge(submitter);
        em.getTransaction().commit();

        runEndpoint.setSc(setSecurityContext(submitter));

        // set the closeout submission fields
        run.setCloseoutSubmissionUser(submitter);
        run.setCloseoutSubmittedDate(new Date());

        // transition run to REVIEWING status
        run = RunDAO.transitionRunToReviewing(em, run.getPk(), submitter);

        // assert that run is in approved status
        assertEquals(RunStatus.APPROVED, run.getStatus());

        // now we want to test that an admin (a different user) can transition to correcting status
        Users admin = DataGeneratorUtils.getRandomUser();
        while (admin.getUserId() == submitter.getUserId())
        {
            admin = DataGeneratorUtils.getRandomUser();
        }
        admin.setIsAdmin(true);
        em.getTransaction().begin();
        em.merge(admin);
        em.getTransaction().commit();

        runEndpoint.setSc(setSecurityContext(admin));

        // transition the run to correcting status
        runEndpoint.transitionRunToCorrecting(run.getPk(), true);
        run = JPAUtils.getRecordById(em, Run.class, run.getPk());
        em.refresh(run);

        // assert that run is now in correcting status
        assertEquals(RunStatus.CORRECTING, run.getStatus());
    }

    private SecurityContext setSecurityContext(Users loggedInUser)
    {
        SecurityContext securityContext = new SecurityContext()
        {
            @Override
            public Principal getUserPrincipal()
            {
                return new Principal()
                {
                    @Override
                    public String getName()
                    {
                        return loggedInUser.getUsername();
                    }
                };
            }

            @Override
            public boolean isUserInRole(String s)
            {
                return true;
            }

            @Override
            public boolean isSecure()
            {
                return true;
            }

            @Override
            public String getAuthenticationScheme()
            {
                return "Bearer";
            }
        };
        return securityContext;
    }
}
