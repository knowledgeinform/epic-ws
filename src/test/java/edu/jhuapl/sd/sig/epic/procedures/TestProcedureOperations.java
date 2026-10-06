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

import edu.jhuapl.sd.sig.epic.data.TestProcedureDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.*;
import edu.jhuapl.sd.sig.epic.resource.model.NewProcData;
import edu.jhuapl.sd.sig.epic.utils.DbTestContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
public class TestProcedureOperations
{
    @Context
    SecurityContext sc;

    private static EntityManager em;
    private static List<Integer> procedurePksAddedToDb = new ArrayList<>();

    private static DbTestContainer container;

    @BeforeAll
    static void beforeAll() throws Exception
    {
        container = new DbTestContainer();
        container.start();
        em = JPAUtils.getEntityManager();
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

    /**
     * Test the create a procedure function
     */
    @Test
    public void testProcedureCreation()
    {
        for (Integer pk : procedurePksAddedToDb)
        {
            TestProcedureDAO.deleteProcedureDef(pk);
        }
        // first create the procedure and save to the database
        ProcedureDef newProcedure = TestProcedureDAO.createProcedure();
        procedurePksAddedToDb.add(newProcedure.getPk());

        // try to retrieve procedure from database
        ProcedureDef procedureDefFromDb = TestProcedureDAO.getProcedureDefByPk(newProcedure.getPk());

        assertNotNull(procedureDefFromDb);
        assertEquals(newProcedure.getPk(), procedureDefFromDb.getPk());

        // check that fields are as expected.
        assertEquals(procedureDefFromDb.getProgram().getPk(), newProcedure.getProgram().getPk());
        assertEquals(procedureDefFromDb.getSubsystem().getPk(), newProcedure.getSubsystem().getPk());
        assertEquals(procedureDefFromDb.getName(), newProcedure.getName());
        assertEquals(procedureDefFromDb.getDescription(), newProcedure.getDescription());
        assertNotNull(procedureDefFromDb.getProcedureDetails());
        assertEquals(procedureDefFromDb.getProcedureDetails().size(), 1);

        // check the associated procedure details and its components
        ProcedureDetails procedureDetails = procedureDefFromDb.getProcedureDetails().stream().findFirst().get();
        assertNotNull(procedureDetails.getPk());
        assertEquals(procedureDetails.getStatus(), ProcedureStatus.DRAFT);
        assertEquals(procedureDetails.getEditType(), EditType.ORIGINAL);
        assertEquals(procedureDetails.getProcedureInstructions().size(), 0);
        assertEquals(procedureDetails.getStepGroupDefs().size(), 0);
        assertEquals(procedureDetails.getProcedureApprovals().size(), 0);
        assertEquals(procedureDetails.getProcedureDefVersion(), 1);
        assertEquals(procedureDetails.getId(),
                newProcedure.getProgram().getCode() + "-" + newProcedure.getSubsystem().getCode() + "-" + procedureDefFromDb.getPk() + "-" + procedureDetails.getProcedureDefVersion());
        assertNull(procedureDetails.getRun());
        assertNull(procedureDetails.getRunNumber());
        assertEquals(procedureDetails.getBlackLineComments().size(), 0);
        assertNull(procedureDetails.getOriginalProcedureDetails());
        assertEquals(procedureDetails.getProcedureDetailRuns().size(), 0);

        assertNotNull(procedureDetails.getProcedureHeader());
        assertNotNull(procedureDetails.getProcedureHeader().getCreationDate());
        assertNotNull(procedureDetails.getProcedureHeader().getPk());
        assertEquals(procedureDetails.getProcedureHeader().getUser().getUserId(),
                newProcedure.getProcedureDetails().stream().findFirst().get().getProcedureHeader().getUser().getUserId());

        // now delete this procedure from the database to set up for next test
        Integer procedurePk = newProcedure.getPk();
        TestProcedureDAO.deleteProcedureDef(procedurePk);

        // confirm deletion
        ProcedureDef procedureDef = TestProcedureDAO.getProcedureDefByPk(procedurePk);
        assertNull(procedureDef);

        procedurePksAddedToDb.remove(procedurePk);
    }

    @Test
    public void testProcedureApprovalSaving()
    {
        for (Integer pk : procedurePksAddedToDb)
        {
            TestProcedureDAO.deleteProcedureDef(pk);
        }

        // create a procedure
        ProcedureDef procedureDef = TestProcedureDAO.createProcedure();
        procedurePksAddedToDb.add(procedureDef.getPk());

        // get the first procedure details - we'll add approvals to it
        ProcedureDetails procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();

        ProcedureApproval procedureApproval = TestProcedureApprovalData.PROCEDURE_APPROVAL_1.getTestData();
        procedureApproval.setProcedureDetails(procedureDetails);

        ProcedureApproval procedureApprovalInDb = TestProcedureDAO.saveProcedureApprovalWrapper(procedureApproval);
        TestProcedureDAO.transitionToWaitingWrapper(procedureApprovalInDb, procedureDetails.getProcedureApprovalDueDate().getTime());
        procedureApprovalInDb = TestProcedureDAO.setApprovalFlagWrapper(procedureApprovalInDb, procedureApproval.getIsApproved());

        // get the procedure from the database
        ProcedureDef procedureDefFromDb = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());

        // ought to have one procedure details
        assertEquals(procedureDefFromDb.getProcedureDetails().size(), 1);

        procedureDetails = procedureDefFromDb.getProcedureDetails().stream().findFirst().get();

        // should have an approval
        assertNotNull(procedureDetails.getProcedureApprovals());
        assertEquals(procedureDetails.getProcedureApprovals().size(), 1);

        ProcedureApproval approval = procedureDetails.getProcedureApprovals().stream().findFirst().get();
        assertNotNull(approval.getPk());
        assertEquals(procedureApproval.getApprovalType(), approval.getApprovalType());
        assertEquals(procedureApproval.getIsApproved(), approval.getIsApproved());
        assertEquals(procedureApproval.getProcedureDetails().getProcedureApprovalDueDate().getTime() / 1000, approval.getProcedureDetails().getProcedureApprovalDueDate().getTime() / 1000);
        assertEquals(procedureApproval.getUsers().getUserId(), approval.getUsers().getUserId());

        // now delete this procedure from the database to set up for next test
        Integer procedurePk = procedureDef.getPk();
        TestProcedureDAO.deleteProcedureDef(procedurePk);

        // confirm deletion
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedurePk);
        assertNull(procedureDef);

        procedurePksAddedToDb.remove(procedurePk);
    }

    @Test
    public void testProcedureInstructionSaving()
    {
        for (Integer pk : procedurePksAddedToDb)
        {
            TestProcedureDAO.deleteProcedureDef(pk);
        }

        // create a procedure
        ProcedureDef procedureDef = TestProcedureDAO.createProcedure();
        assertNotNull(procedureDef.getPk());
        procedurePksAddedToDb.add(procedureDef.getPk());

        // assert that only have one procedure details
        assertNotNull(procedureDef.getProcedureDetails());
        assertEquals(procedureDef.getProcedureDetails().size(), 1);

        // get the first procedure details - we'll add instructions to it
        ProcedureDetails procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();
        assertNotNull(procedureDetails.getPk());

        // it shouldn't currently have instructions
        assertNull(procedureDetails.getProcedureInstructions());

        // get some instruction test data
        SortedSet<ProcedureInstruction> testInstructions = TestProcedureInstructionData.PROCEDURE_INSTRUCTION_SET_1.getTestData();

        // go through and set the procedure details for each instruction, then save each instruction to the database
        for (ProcedureInstruction instruction : testInstructions)
        {
            instruction.setProcedureDetails(procedureDetails);
            TestProcedureDAO.saveProcedureInstructionSectionWrapper(instruction);
        }

        ProcedureInstruction testInstruction1 = testInstructions.stream().filter(o -> o.getDisplayOrder().equals(1)).findFirst().get();
        ProcedureInstruction testInstruction2 = testInstructions.stream().filter(o -> o.getDisplayOrder().equals(2)).findFirst().get();
        ProcedureInstruction testInstruction3 = testInstructions.stream().filter(o -> o.getDisplayOrder().equals(3)).findFirst().get();

        // query the database for this procedure
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());

        // get the procedureDetails
        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();

        // it should have three instructions
        assertEquals(procedureDetails.getProcedureInstructions().size(), 3);

        // check the instruction names and display orders
        SortedSet<ProcedureInstruction> procedureInstructions = procedureDetails.getProcedureInstructions();
        ProcedureInstruction procedureInstruction1 = procedureInstructions.stream().filter(o -> o.getDisplayOrder().equals(1)).findFirst().get();
        ProcedureInstruction procedureInstruction2 = procedureInstructions.stream().filter(o -> o.getDisplayOrder().equals(2)).findFirst().get();
        ProcedureInstruction procedureInstruction3 = procedureInstructions.stream().filter(o -> o.getDisplayOrder().equals(3)).findFirst().get();

        assertNotNull(procedureInstruction1.getPk());
        assertEquals(procedureInstruction1.getSectionName(), testInstruction1.getSectionName());
        assertEquals(procedureInstruction1.getDisplayOrder(), testInstruction1.getDisplayOrder());
        assertEquals(procedureInstruction1.getEditType(), EditType.ORIGINAL);
        assertNotNull(procedureInstruction2.getPk());
        assertEquals(procedureInstruction2.getSectionName(), testInstruction2.getSectionName());
        assertEquals(procedureInstruction2.getDisplayOrder(), testInstruction2.getDisplayOrder());
        assertEquals(procedureInstruction2.getEditType(), EditType.ORIGINAL);
        assertNotNull(procedureInstruction3.getPk());
        assertEquals(procedureInstruction3.getSectionName(), testInstruction3.getSectionName());
        assertEquals(procedureInstruction3.getDisplayOrder(), testInstruction3.getDisplayOrder());
        assertEquals(procedureInstruction3.getEditType(), EditType.ORIGINAL);

        // now try updating the instructions
        procedureInstruction1 = TestProcedureDAO.updateInstructionSectionWrapper(procedureInstruction1, testInstruction1);
        procedureInstruction2 = TestProcedureDAO.updateInstructionSectionWrapper(procedureInstruction2, testInstruction2);
        procedureInstruction3 = TestProcedureDAO.updateInstructionSectionWrapper(procedureInstruction3, testInstruction3);

        // query the database again just to check
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());

        // get the procedureDetails
        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();

        // it should have three instructions
        assertEquals(procedureDetails.getProcedureInstructions().size(), 3);

        // check the add the instruction info
        procedureInstructions = procedureDetails.getProcedureInstructions();
        procedureInstruction1 = procedureInstructions.stream().filter(o -> o.getDisplayOrder().equals(1)).findFirst().get();
        procedureInstruction2 = procedureInstructions.stream().filter(o -> o.getDisplayOrder().equals(2)).findFirst().get();
        procedureInstruction3 = procedureInstructions.stream().filter(o -> o.getDisplayOrder().equals(3)).findFirst().get();

        assertNotNull(procedureInstruction1.getPk());
        assertEquals(procedureInstruction1.getSectionName(), testInstruction1.getSectionName());
        assertEquals(procedureInstruction1.getDisplayOrder(), testInstruction1.getDisplayOrder());
        assertEquals(procedureInstruction1.getEditType(), EditType.ORIGINAL);
        assertEquals(procedureInstruction1.getText(), testInstruction1.getText());
        assertNotNull(procedureInstruction2.getPk());
        assertEquals(procedureInstruction2.getSectionName(), testInstruction2.getSectionName());
        assertEquals(procedureInstruction2.getDisplayOrder(), testInstruction2.getDisplayOrder());
        assertEquals(procedureInstruction2.getEditType(), EditType.ORIGINAL);
        assertEquals(procedureInstruction2.getText(), testInstruction2.getText());
        assertNotNull(procedureInstruction3.getPk());
        assertEquals(procedureInstruction3.getSectionName(), testInstruction3.getSectionName());
        assertEquals(procedureInstruction3.getDisplayOrder(), testInstruction3.getDisplayOrder());
        assertEquals(procedureInstruction3.getEditType(), EditType.ORIGINAL);
        assertEquals(procedureInstruction3.getText(), testInstruction3.getText());

        // now delete this procedure from the database to set up for next test
        Integer procedurePk = procedureDef.getPk();
        TestProcedureDAO.deleteProcedureDef(procedurePk);

        // confirm deletion
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedurePk);
        assertNull(procedureDef);

        procedurePksAddedToDb.remove(procedurePk);
    }

    @Test
    public void testStepGroupSaving()
    {
        for (Integer pk : procedurePksAddedToDb)
        {
            TestProcedureDAO.deleteProcedureDef(pk);
        }

        // create a procedure
        ProcedureDef procedureDef = TestProcedureDAO.createProcedure();
        assertNotNull(procedureDef.getPk());
        procedurePksAddedToDb.add(procedureDef.getPk());

        // assert that only have one procedure details
        assertNotNull(procedureDef.getProcedureDetails());
        assertEquals(procedureDef.getProcedureDetails().size(), 1);

        // get the first procedure details - we'll add step groups to it
        ProcedureDetails procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();
        assertNotNull(procedureDetails.getPk());

        // it shouldn't currently have any step groups
        assertNull(procedureDetails.getStepGroupDefs());

        // get some step group test data
        // this first set will be our top level groups
        SortedSet<StepGroupDef> testTopLevelGroups = TestProcedureStepGroupData.STEP_GROUP_SET_12.getTestData();
        StepGroupDef group1FromTest = testTopLevelGroups.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
        StepGroupDef group2FromTest = testTopLevelGroups.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();

        // for each one, set to procedureDetails and save to database
        SortedSet<StepGroupDef> savedGroups = new TreeSet<>();
        for (StepGroupDef group : testTopLevelGroups)
        {
            group.setProcedureDetails(procedureDetails);
            group.setStepGroupDefParent(null);
            savedGroups.add(TestProcedureDAO.saveStepGroupDefWrapper(group));
        }

        // query the database for the procedure
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());

        // get the procedureDetails
        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();

        // it should have two step groups in the top level
        assertEquals(procedureDetails.getStepGroupDefs().size(), 2);

        SortedSet<StepGroupDef> groupsFromDb = procedureDetails.getStepGroupDefs();
        StepGroupDef group1FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
        StepGroupDef group2FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();

        // check has primary keys, null parents, empty children, null steps, and matching names and display orders
        assertNotNull(group1FromDb.getPk());
        assertNull(group1FromDb.getStepGroupDefParent());
        assertEquals(group1FromDb.getStepGroupDefsChildren().size(), 0);
        assertEquals(group1FromDb.getStepDefs().size(), 0);
        assertEquals(group1FromDb.getDisplayOrder(), group1FromTest.getDisplayOrder());
        assertEquals(group1FromDb.getStepGroupName(), group1FromTest.getStepGroupName());
        assertEquals(group1FromDb.getDescription(), group1FromTest.getDescription());
        assertNotNull(group2FromDb.getPk());
        assertNull(group2FromDb.getStepGroupDefParent());
        assertEquals(group2FromDb.getStepGroupDefsChildren().size(), 0);
        assertEquals(group2FromDb.getStepDefs().size(), 0);
        assertEquals(group2FromDb.getDisplayOrder(), group2FromTest.getDisplayOrder());
        assertEquals(group2FromDb.getStepGroupName(), group2FromTest.getStepGroupName());
        assertEquals(group2FromDb.getDescription(), group2FromTest.getDescription());

        // now let's add subgroups to group1FromDb.
        SortedSet<StepGroupDef> testSubGroup11 = TestProcedureStepGroupData.STEP_GROUP_SET_ABC.getTestData();
        StepGroupDef subgroupAFromTest = testSubGroup11.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
        StepGroupDef subgroupBFromTest = testSubGroup11.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();
        StepGroupDef subgroupCFromTest = testSubGroup11.stream().filter(g -> g.getDisplayOrder().equals(3)).findFirst().get();

        // for each one, set to procedureDetails, parent to group1FromDb and save to database
        savedGroups = new TreeSet<>();
        for (StepGroupDef group : testSubGroup11)
        {
            group.setProcedureDetails(procedureDetails);
            group.setStepGroupDefParent(group1FromDb);
            savedGroups.add(TestProcedureDAO.saveStepGroupDefWrapper(group));
        }

        // query the database for the procedure
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());

        // get the procedureDetails
        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();

        // it should have two step groups in the top level
        assertEquals(procedureDetails.getStepGroupDefs().size(), 2);

        groupsFromDb = procedureDetails.getStepGroupDefs();
        group1FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
        group2FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();

        // group1FromDb ought to have 3 subgroups
        assertNotNull(group1FromDb.getStepGroupDefsChildren());
        assertEquals(group1FromDb.getStepGroupDefsChildren().size(), 3);

        SortedSet<StepGroupDef> subgroups11FromDb = group1FromDb.getStepGroupDefsChildren();
        StepGroupDef subgroup11FromDb = subgroups11FromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
        StepGroupDef subgroup12FromDb = subgroups11FromDb.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();
        StepGroupDef subgroup13FromDb = subgroups11FromDb.stream().filter(g -> g.getDisplayOrder().equals(3)).findFirst().get();

        // check that subgroups have primary keys, not-null parents, null children, null steps, and matching names and display orders
        assertNotNull(subgroup11FromDb.getPk());
        assertNotNull(subgroup11FromDb.getStepGroupDefParent());
        assertEquals(subgroup11FromDb.getStepGroupDefsChildren().size(), 0);
        assertEquals(subgroup11FromDb.getStepDefs().size(), 0);
        assertEquals(subgroup11FromDb.getStepGroupName(), subgroupAFromTest.getStepGroupName());
        assertEquals(subgroup11FromDb.getDescription(), subgroupAFromTest.getDescription());
        assertEquals(subgroup11FromDb.getDisplayOrder(), subgroupAFromTest.getDisplayOrder());
        assertNotNull(subgroup12FromDb.getPk());
        assertNotNull(subgroup12FromDb.getStepGroupDefParent());
        assertEquals(subgroup12FromDb.getStepGroupDefsChildren().size(), 0);
        assertEquals(subgroup12FromDb.getStepDefs().size(), 0);
        assertEquals(subgroup12FromDb.getStepGroupName(), subgroupBFromTest.getStepGroupName());
        assertEquals(subgroup12FromDb.getDescription(), subgroupBFromTest.getDescription());
        assertEquals(subgroup12FromDb.getDisplayOrder(), subgroupBFromTest.getDisplayOrder());
        assertNotNull(subgroup13FromDb.getPk());
        assertNotNull(subgroup13FromDb.getStepGroupDefParent());
        assertEquals(subgroup13FromDb.getStepGroupDefsChildren().size(), 0);
        assertEquals(subgroup13FromDb.getStepDefs().size(), 0);
        assertEquals(subgroup13FromDb.getStepGroupName(), subgroupCFromTest.getStepGroupName());
        assertEquals(subgroup13FromDb.getDescription(), subgroupCFromTest.getDescription());
        assertEquals(subgroup13FromDb.getDisplayOrder(), subgroupCFromTest.getDisplayOrder());

        // now let's add subgroups to subgroup13FromDb.
        SortedSet<StepGroupDef> testSubGroup113 = TestProcedureStepGroupData.STEP_GROUP_SET_XYZ.getTestData();
        StepGroupDef subgroupXFromTest = testSubGroup113.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
        StepGroupDef subgroupYFromTest = testSubGroup113.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();
        StepGroupDef subgroupZFromTest = testSubGroup113.stream().filter(g -> g.getDisplayOrder().equals(3)).findFirst().get();

        // for each one, set to procedureDetails, parent to group1FromDb and save to database
        savedGroups = new TreeSet<>();
        for (StepGroupDef group : testSubGroup113)
        {
            group.setProcedureDetails(procedureDetails);
            group.setStepGroupDefParent(subgroup13FromDb);
            savedGroups.add(TestProcedureDAO.saveStepGroupDefWrapper(group));
        }

        // query the database for the procedure
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());

        // get the procedureDetails
        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();

        // it should have two step groups in the top level
        assertEquals(procedureDetails.getStepGroupDefs().size(), 2);

        groupsFromDb = procedureDetails.getStepGroupDefs();
        group1FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
        group2FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();

        // group1FromDb ought to have 3 subgroups
        assertNotNull(group1FromDb.getStepGroupDefsChildren());
        assertEquals(group1FromDb.getStepGroupDefsChildren().size(), 3);

        subgroups11FromDb = group1FromDb.getStepGroupDefsChildren();
        subgroup13FromDb = subgroups11FromDb.stream().filter(g -> g.getDisplayOrder().equals(3)).findFirst().get();

        // subgroup13FromDb ought to have three subgroups
        assertNotNull(subgroup13FromDb.getStepGroupDefsChildren());
        assertEquals(subgroup13FromDb.getStepGroupDefsChildren().size(), 3);

        SortedSet<StepGroupDef> subgroups113FromDb = subgroup13FromDb.getStepGroupDefsChildren();
        StepGroupDef subgroup131FromDb = subgroups113FromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
        StepGroupDef subgroup132FromDb = subgroups113FromDb.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();
        StepGroupDef subgroup133FromDb = subgroups113FromDb.stream().filter(g -> g.getDisplayOrder().equals(3)).findFirst().get();

        // check that the second layer subgroups have primary keys, not-null parents, null children, null steps, and matching names and display orders
        assertNotNull(subgroup131FromDb.getPk());
        assertNotNull(subgroup131FromDb.getStepGroupDefParent());
        assertEquals(subgroup131FromDb.getStepGroupDefsChildren().size(), 0);
        assertEquals(subgroup131FromDb.getStepDefs().size(), 0);
        assertEquals(subgroup131FromDb.getStepGroupName(), subgroupXFromTest.getStepGroupName());
        assertEquals(subgroup131FromDb.getDescription(), subgroupXFromTest.getDescription());
        assertEquals(subgroup131FromDb.getDisplayOrder(), subgroupXFromTest.getDisplayOrder());
        assertNotNull(subgroup132FromDb.getPk());
        assertNotNull(subgroup132FromDb.getStepGroupDefParent());
        assertEquals(subgroup132FromDb.getStepGroupDefsChildren().size(), 0);
        assertEquals(subgroup132FromDb.getStepDefs().size(), 0);
        assertEquals(subgroup132FromDb.getStepGroupName(), subgroupYFromTest.getStepGroupName());
        assertEquals(subgroup132FromDb.getDescription(), subgroupYFromTest.getDescription());
        assertEquals(subgroup132FromDb.getDisplayOrder(), subgroupYFromTest.getDisplayOrder());
        assertNotNull(subgroup133FromDb.getPk());
        assertNotNull(subgroup133FromDb.getStepGroupDefParent());
        assertEquals(subgroup133FromDb.getStepGroupDefsChildren().size(), 0);
        assertEquals(subgroup133FromDb.getStepDefs().size(), 0);
        assertEquals(subgroup133FromDb.getStepGroupName(), subgroupZFromTest.getStepGroupName());
        assertEquals(subgroup133FromDb.getDescription(), subgroupZFromTest.getDescription());
        assertEquals(subgroup133FromDb.getDisplayOrder(), subgroupZFromTest.getDisplayOrder());

        // done testing group creation.

        // now delete this procedure from the database to set up for next test
        Integer procedurePk = procedureDef.getPk();
        TestProcedureDAO.deleteProcedureDef(procedurePk);

        // confirm deletion
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedurePk);
        assertNull(procedureDef);

        procedurePksAddedToDb.remove(procedurePk);
    }

    @Test
    public void testCheckboxAndSingleValueStepSaving()
    {
        for (Integer pk : procedurePksAddedToDb)
        {
            TestProcedureDAO.deleteProcedureDef(pk);
        }

        // create a procedure
        ProcedureDef procedureDef = TestProcedureDAO.createProcedure();
        assertNotNull(procedureDef.getPk());
        procedurePksAddedToDb.add(procedureDef.getPk());

        // assert that only have one procedure details
        assertNotNull(procedureDef.getProcedureDetails());
        assertEquals(procedureDef.getProcedureDetails().size(), 1);

        // get the first procedure details - we'll add step groups to it
        ProcedureDetails procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();
        assertNotNull(procedureDetails.getPk());

        // it shouldn't currently have any step groups
        assertNull(procedureDetails.getStepGroupDefs());

        // get some step group test data
        // this first set will be our top level groups
        SortedSet<StepGroupDef> testTopLevelGroups = TestProcedureStepGroupData.STEP_GROUP_SET_12.getTestData();
        StepGroupDef group1FromTest = testTopLevelGroups.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
        StepGroupDef group2FromTest = testTopLevelGroups.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();

        // for each one, set to procedureDetails and save to database
        SortedSet<StepGroupDef> savedGroups = new TreeSet<>();
        for (StepGroupDef group : testTopLevelGroups)
        {
            group.setProcedureDetails(procedureDetails);
            group.setStepGroupDefParent(null);
            savedGroups.add(TestProcedureDAO.saveStepGroupDefWrapper(group));
        }

        // query the database for the procedure
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());

        // get the procedureDetails
        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();

        // it should have two step groups in the top level
        assertEquals(procedureDetails.getStepGroupDefs().size(), 2);

        SortedSet<StepGroupDef> groupsFromDb = procedureDetails.getStepGroupDefs();
        StepGroupDef group1FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
        StepGroupDef group2FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();

        // now let's add steps to these groups
        // get some test step data for checkbox and single value steps
        SortedSet<StepDef> testStepDefs = TestProcedureStepData.TEST_STEP_DATA_SET_1.getTestData();
        StepDef testStep1 = testStepDefs.stream().filter(s -> s.getDisplayOrder().equals(1)).findFirst().get();
        StepDef testStep2 = testStepDefs.stream().filter(s -> s.getDisplayOrder().equals(2)).findFirst().get();

        // we'll add these two steps to group1FromDb
        SortedSet<StepDef> savedSteps = new TreeSet<>();
        for (StepDef step : testStepDefs)
        {
            step.setStepGroupDef(group1FromDb);
            savedSteps.add(TestProcedureDAO.saveStepDefWrapper(step));
        }

        // query the database for the procedure
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());

        // get the procedureDetails
        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();

        // it should have two step groups in the top level
        assertEquals(procedureDetails.getStepGroupDefs().size(), 2);

        groupsFromDb = procedureDetails.getStepGroupDefs();
        group1FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();

        // group1FromDb should have 2 steps
        assertEquals(group1FromDb.getStepDefs().size(), 2);

        SortedSet<StepDef> stepsFromDb = group1FromDb.getStepDefs();
        StepDef step1FromDb = stepsFromDb.stream().filter(s -> s.getDisplayOrder().equals(1)).findFirst().get();
        StepDef step2FromDb = stepsFromDb.stream().filter(s -> s.getDisplayOrder().equals(2)).findFirst().get();

        // steps should have PK, a step group parent, and their fields should match the test data. All the run related
        // fields should be empty (for sets) or null
        assertNotNull(step1FromDb.getPk());
        assertEquals(step1FromDb.getType(), testStep1.getType());
        assertEquals(step1FromDb.getStepGroupDef().getPk(), group1FromDb.getPk());
        assertEquals(step1FromDb.getEditType(), EditType.ORIGINAL);
        assertEquals(step1FromDb.getAllowEquipmentEntry(), testStep1.getAllowEquipmentEntry());
        assertEquals(step1FromDb.getDisplayOrder(), testStep1.getDisplayOrder());
        assertEquals(step1FromDb.getEsd0(), testStep1.getEsd0());
        assertEquals(step1FromDb.getHazardous(), testStep1.getHazardous());
        assertEquals(step1FromDb.getRequireWitness(), testStep1.getRequireWitness());
        assertEquals(step1FromDb.getMandatoryInspection(), testStep1.getMandatoryInspection());
        assertEquals(step1FromDb.getStepName(), testStep1.getStepName());
        assertEquals(step1FromDb.getInstructions(), testStep1.getInstructions());
        assertEquals(step1FromDb.getRunStepComments().size(), 0);
        assertEquals(step1FromDb.getBlackLineComments().size(), 0);
        assertEquals(step1FromDb.getEquipment().size(), 0);
        assertEquals(step1FromDb.getHistories().size(), 0);
        assertFalse(step1FromDb.getIsManualValidation());
        assertNull(step1FromDb.getMandatoryInspectionSecondSignature());
        assertNull(step1FromDb.getWitnessSecondSignature());
        assertNull(step1FromDb.getRunValueEntryUser());
        assertNull(step1FromDb.getRunValueSavedTimestamp());

        assertNotNull(step2FromDb.getPk());
        assertEquals(step2FromDb.getType(), testStep2.getType());
        assertEquals(step2FromDb.getStepGroupDef().getPk(), group1FromDb.getPk());
        assertEquals(step2FromDb.getEditType(), EditType.ORIGINAL);
        assertEquals(step2FromDb.getAllowEquipmentEntry(), testStep2.getAllowEquipmentEntry());
        assertEquals(step2FromDb.getDisplayOrder(), testStep2.getDisplayOrder());
        assertEquals(step2FromDb.getEsd0(), testStep2.getEsd0());
        assertEquals(step2FromDb.getHazardous(), testStep2.getHazardous());
        assertEquals(step2FromDb.getRequireWitness(), testStep2.getRequireWitness());
        assertEquals(step2FromDb.getMandatoryInspection(), testStep2.getMandatoryInspection());
        assertEquals(step2FromDb.getStepName(), testStep2.getStepName());
        assertEquals(step2FromDb.getInstructions(), testStep2.getInstructions());
        assertEquals(step2FromDb.getRunStepComments().size(), 0);
        assertEquals(step2FromDb.getBlackLineComments().size(), 0);
        assertEquals(step2FromDb.getEquipment().size(), 0);
        assertEquals(step2FromDb.getHistories().size(), 0);
        assertFalse(step2FromDb.getIsManualValidation());
        assertNull(step2FromDb.getMandatoryInspectionSecondSignature());
        assertNull(step2FromDb.getWitnessSecondSignature());
        assertNull(step2FromDb.getRunValueEntryUser());
        assertNull(step2FromDb.getRunValueSavedTimestamp());

        // done testing step creation.

        // now delete this procedure from the database to set up for next test
        Integer procedurePk = procedureDef.getPk();
        TestProcedureDAO.deleteProcedureDef(procedurePk);

        // confirm deletion
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedurePk);
        assertNull(procedureDef);

        procedurePksAddedToDb.remove(procedurePk);
    }

    @Test
    public void testTableStepSaving()
    {
        for (Integer pk : procedurePksAddedToDb)
        {
            TestProcedureDAO.deleteProcedureDef(pk);
        }

        // create a procedure
        ProcedureDef procedureDef = TestProcedureDAO.createProcedure();
        assertNotNull(procedureDef.getPk());
        procedurePksAddedToDb.add(procedureDef.getPk());

        // assert that only have one procedure details
        assertNotNull(procedureDef.getProcedureDetails());
        assertEquals(procedureDef.getProcedureDetails().size(), 1);

        // get the first procedure details - we'll add step groups to it
        ProcedureDetails procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();
        assertNotNull(procedureDetails.getPk());

        // it shouldn't currently have any step groups
        assertNull(procedureDetails.getStepGroupDefs());

        // get some step group test data
        // this first set will be our top level groups
        SortedSet<StepGroupDef> testTopLevelGroups = TestProcedureStepGroupData.STEP_GROUP_SET_12.getTestData();
        StepGroupDef group1FromTest = testTopLevelGroups.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
        StepGroupDef group2FromTest = testTopLevelGroups.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();

        // for each one, set to procedureDetails and save to database
        SortedSet<StepGroupDef> savedGroups = new TreeSet<>();
        for (StepGroupDef group : testTopLevelGroups)
        {
            group.setProcedureDetails(procedureDetails);
            group.setStepGroupDefParent(null);
            savedGroups.add(TestProcedureDAO.saveStepGroupDefWrapper(group));
        }

        // query the database for the procedure
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());

        // get the procedureDetails
        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();

        // it should have two step groups in the top level
        assertEquals(procedureDetails.getStepGroupDefs().size(), 2);

        SortedSet<StepGroupDef> groupsFromDb = procedureDetails.getStepGroupDefs();
        StepGroupDef group1FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();

        // let's add a table step to group1FromDb
        SortedSet<StepDef> testStepData = TestProcedureStepData.TEST_STEP_DATA_SET_2.getTestData();
        StepDef testStep1 = testStepData.stream().filter(s -> s.getDisplayOrder().equals(1)).findFirst().get();

        // we'll add this step to group1FromDb
        SortedSet<StepDef> savedSteps = new TreeSet<>();
        for (StepDef step : testStepData)
        {
            step.setStepGroupDef(group1FromDb);
            savedSteps.add(TestProcedureDAO.saveStepDefWrapper(step));
        }

        // query the database for the procedure
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());

        // get the procedureDetails
        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();

        // it should have two step groups in the top level
        assertEquals(procedureDetails.getStepGroupDefs().size(), 2);

        groupsFromDb = procedureDetails.getStepGroupDefs();
        group1FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();

        // group1FromDb should have one step
        assertEquals(group1FromDb.getStepDefs().size(), 1);

        SortedSet<StepDef> stepsFromDb = group1FromDb.getStepDefs();
        StepDef step1FromDb = stepsFromDb.stream().filter(s -> s.getDisplayOrder().equals(1)).findFirst().get();

        // steps should have PK, a step group parent, and their fields should match the test data. All the run related
        // fields should be empty (for sets) or null
        assertNotNull(step1FromDb.getPk());
        assertEquals(step1FromDb.getType(), testStep1.getType());
        assertEquals(step1FromDb.getStepGroupDef().getPk(), group1FromDb.getPk());
        assertEquals(step1FromDb.getEditType(), EditType.ORIGINAL);
        assertEquals(step1FromDb.getAllowEquipmentEntry(), testStep1.getAllowEquipmentEntry());
        assertEquals(step1FromDb.getDisplayOrder(), testStep1.getDisplayOrder());
        assertEquals(step1FromDb.getEsd0(), testStep1.getEsd0());
        assertEquals(step1FromDb.getHazardous(), testStep1.getHazardous());
        assertEquals(step1FromDb.getRequireWitness(), testStep1.getRequireWitness());
        assertEquals(step1FromDb.getMandatoryInspection(), testStep1.getMandatoryInspection());
        assertEquals(step1FromDb.getStepName(), testStep1.getStepName());
        assertEquals(step1FromDb.getInstructions(), testStep1.getInstructions());
        assertEquals(step1FromDb.getRunStepComments().size(), 0);
        assertEquals(step1FromDb.getBlackLineComments().size(), 0);
        assertEquals(step1FromDb.getEquipment().size(), 0);
        assertEquals(step1FromDb.getHistories().size(), 0);
        assertFalse(step1FromDb.getIsManualValidation());
        assertNull(step1FromDb.getMandatoryInspectionSecondSignature());
        assertNull(step1FromDb.getWitnessSecondSignature());
        assertNull(step1FromDb.getRunValueEntryUser());
        assertNull(step1FromDb.getRunValueSavedTimestamp());

        // check the step's rows and cells
        StepTable stepTableFromDb = (StepTable) step1FromDb;
        assertNotNull(stepTableFromDb.getStepTableRows());
        assertEquals(stepTableFromDb.getStepTableRows().size(), 2);

        SortedSet<StepTableRow> rows = stepTableFromDb.getStepTableRows();
        StepTableRow row1 = rows.first();
        StepTableRow row2 = rows.last();

        SortedSet<StepTableRow> testRows = ((StepTable) testStep1).getStepTableRows();
        StepTableRow testRow1 = testRows.first();
        StepTableRow testRow2 = testRows.last();

        // checking first row
        assertNotNull(row1.getPk());
        assertEquals(row1.getRowNumber(), testRow1.getRowNumber());
        assertNotNull(row1.getStepTableCells());
        assertEquals(row1.getStepTableCells().size(), 2);

        SortedSet<StepTableCell> cells = row1.getStepTableCells();
        StepTableCell cell1 = cells.first();
        StepTableCell cell2 = cells.last();

        SortedSet<StepTableCell> testCells = testRow1.getStepTableCells();
        StepTableCell testCell1 = testCells.first();
        StepTableCell testCell2 = testCells.last();

        assertNotNull(cell1.getPk());
        assertEquals(cell1.getNonEditableValue(), testCell1.getNonEditableValue());
        assertEquals(cell1.getEditable(), testCell1.getEditable());
        assertEquals(cell1.getCellIndex(), testCell1.getCellIndex());

        assertNotNull(cell2.getPk());
        assertEquals(cell2.getNonEditableValue(), testCell2.getNonEditableValue());
        assertEquals(cell2.getEditable(), testCell2.getEditable());
        assertEquals(cell2.getCellIndex(), testCell2.getCellIndex());

        // checking second row
        assertNotNull(row2.getPk());
        assertEquals(row2.getRowNumber(), testRow2.getRowNumber());
        assertNotNull(row2.getStepTableCells());
        assertEquals(row2.getStepTableCells().size(), 2);

        cells = row2.getStepTableCells();
        cell1 = cells.first();
        cell2 = cells.last();

        testCells = testRow2.getStepTableCells();
        testCell1 = testCells.first();
        testCell2 = testCells.last();

        assertNotNull(cell1.getPk());
        assertEquals(cell1.getNonEditableValue(), testCell1.getNonEditableValue());
        assertEquals(cell1.getEditable(), testCell1.getEditable());
        assertEquals(cell1.getCellIndex(), testCell1.getCellIndex());

        assertNotNull(cell2.getPk());
        assertEquals(cell2.getNonEditableValue(), testCell2.getNonEditableValue());
        assertEquals(cell2.getEditable(), testCell2.getEditable());
        assertEquals(cell2.getCellIndex(), testCell2.getCellIndex());

        // done testing step creation.

        // now delete this procedure from the database to set up for next test
        Integer procedurePk = procedureDef.getPk();
        TestProcedureDAO.deleteProcedureDef(procedurePk);

        // confirm deletion
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedurePk);
        assertNull(procedureDef);

        procedurePksAddedToDb.remove(procedurePk);
    }

    @Test
    public void testCloneProcedureWithNoRuns()
    {
        for (Integer pk : procedurePksAddedToDb)
        {
            TestProcedureDAO.deleteProcedureDef(pk);
        }

        // first create a procedure
        // create a procedure
        ProcedureDef procedureDef = TestProcedureDAO.createProcedure();
        assertNotNull(procedureDef.getPk());
        procedurePksAddedToDb.add(procedureDef.getPk());

        // assert that only have one procedure details
        assertNotNull(procedureDef.getProcedureDetails());
        assertEquals(procedureDef.getProcedureDetails().size(), 1);

        // get the first procedure details - we'll add step groups to it
        ProcedureDetails procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();
        assertNotNull(procedureDetails.getPk());

        // it shouldn't currently have any step groups
        assertNull(procedureDetails.getStepGroupDefs());

        // get some step group test data
        // this first set will be our top level groups
        SortedSet<StepGroupDef> testTopLevelGroups = TestProcedureStepGroupData.STEP_GROUP_SET_12.getTestData();
        StepGroupDef group1FromTest = testTopLevelGroups.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
        StepGroupDef group2FromTest = testTopLevelGroups.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();

        // for each one, set to procedureDetails and save to database
        SortedSet<StepGroupDef> savedGroups = new TreeSet<>();
        for (StepGroupDef group : testTopLevelGroups)
        {
            group.setProcedureDetails(procedureDetails);
            group.setStepGroupDefParent(null);
            savedGroups.add(TestProcedureDAO.saveStepGroupDefWrapper(group));
        }

        // query the database for the procedure
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());

        // get the procedureDetails
        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();

        // it should have two step groups in the top level
        assertEquals(procedureDetails.getStepGroupDefs().size(), 2);

        SortedSet<StepGroupDef> groupsFromDb = procedureDetails.getStepGroupDefs();
        StepGroupDef group1FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();

        // now let's add subgroups to group1FromDb.
        SortedSet<StepGroupDef> testSubGroup11 = TestProcedureStepGroupData.STEP_GROUP_SET_ABC.getTestData();
        StepGroupDef subgroupAFromTest = testSubGroup11.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
        StepGroupDef subgroupBFromTest = testSubGroup11.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();
        StepGroupDef subgroupCFromTest = testSubGroup11.stream().filter(g -> g.getDisplayOrder().equals(3)).findFirst().get();

        // for each one, set to procedureDetails, parent to group1FromDb and save to database
        savedGroups = new TreeSet<>();
        for (StepGroupDef group : testSubGroup11)
        {
            group.setProcedureDetails(procedureDetails);
            group.setStepGroupDefParent(group1FromDb);
            savedGroups.add(TestProcedureDAO.saveStepGroupDefWrapper(group));
        }

        // query the database for the procedure
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());

        // get the procedureDetails
        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();

        // it should have two step groups in the top level
        assertEquals(procedureDetails.getStepGroupDefs().size(), 2);

        groupsFromDb = procedureDetails.getStepGroupDefs();
        group1FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();

        // group1FromDb ought to have 3 subgroups
        assertNotNull(group1FromDb.getStepGroupDefsChildren());
        assertEquals(group1FromDb.getStepGroupDefsChildren().size(), 3);

        SortedSet<StepGroupDef> subgroups11FromDb = group1FromDb.getStepGroupDefsChildren();
        StepGroupDef subgroup11FromDb = subgroups11FromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
        StepGroupDef subgroup12FromDb = subgroups11FromDb.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();
        StepGroupDef subgroup13FromDb = subgroups11FromDb.stream().filter(g -> g.getDisplayOrder().equals(3)).findFirst().get();

        // now let's add steps
        // get some test step data for checkbox and single value steps
        SortedSet<StepDef> testStepDefs = TestProcedureStepData.TEST_STEP_DATA_SET_1.getTestData();
        StepDef testStep1 = testStepDefs.stream().filter(s -> s.getDisplayOrder().equals(1)).findFirst().get();
        StepDef testStep2 = testStepDefs.stream().filter(s -> s.getDisplayOrder().equals(2)).findFirst().get();

        // we'll add these two steps to group1FromDb
        SortedSet<StepDef> savedSteps = new TreeSet<>();
        for (StepDef step : testStepDefs)
        {
            step.setStepGroupDef(group1FromDb);
            savedSteps.add(TestProcedureDAO.saveStepDefWrapper(step));
        }

        // query the database for the procedure
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());

        // get the procedureDetails
        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();

        // it should have two step groups in the top level
        assertEquals(procedureDetails.getStepGroupDefs().size(), 2);

        groupsFromDb = procedureDetails.getStepGroupDefs();
        group1FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();

        // group1FromDb should have 2 steps
        assertEquals(group1FromDb.getStepDefs().size(), 2);

        SortedSet<StepDef> stepsFromDb = group1FromDb.getStepDefs();
        StepDef step1FromDb = stepsFromDb.stream().filter(s -> s.getDisplayOrder().equals(1)).findFirst().get();
        StepDef step2FromDb = stepsFromDb.stream().filter(s -> s.getDisplayOrder().equals(2)).findFirst().get();

        // let's add a table step to subgroup12FromDb
        SortedSet<StepDef> testStepData = TestProcedureStepData.TEST_STEP_DATA_SET_2.getTestData();
        StepDef testStepTable1 = testStepData.stream().filter(s -> s.getDisplayOrder().equals(1)).findFirst().get();

        // we'll add this step to subgroup12FromDb
        savedSteps = new TreeSet<>();
        for (StepDef step : testStepData)
        {
            step.setStepGroupDef(subgroup12FromDb);
            savedSteps.add(TestProcedureDAO.saveStepDefWrapper(step));
        }

        // query the database for the procedure
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());

        // get the procedureDetails
        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();

        // it should have two step groups in the top level
        assertEquals(procedureDetails.getStepGroupDefs().size(), 2);

        groupsFromDb = procedureDetails.getStepGroupDefs();
        group1FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();

        // group1FromDb ought to have 3 subgroups
        assertNotNull(group1FromDb.getStepGroupDefsChildren());
        assertEquals(group1FromDb.getStepGroupDefsChildren().size(), 3);

        subgroups11FromDb = group1FromDb.getStepGroupDefsChildren();
        subgroup12FromDb = subgroups11FromDb.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();

        // subgroup12FromDb should have a step
        assertNotNull(subgroup12FromDb.getStepDefs());
        assertEquals(subgroup12FromDb.getStepDefs().size(), 1);

        // steps and step groups are added. Now add procedure instructions
        // get some instruction test data
        SortedSet<ProcedureInstruction> testInstructions = TestProcedureInstructionData.PROCEDURE_INSTRUCTION_SET_1.getTestData();

        // go through and set the procedure details for each instruction, then save each instruction to the database
        for (ProcedureInstruction instruction : testInstructions)
        {
            instruction.setProcedureDetails(procedureDetails);
            TestProcedureDAO.saveProcedureInstructionSectionWrapper(instruction);
        }

        ProcedureInstruction testInstruction1 = testInstructions.stream().filter(o -> o.getDisplayOrder().equals(1)).findFirst().get();
        ProcedureInstruction testInstruction2 = testInstructions.stream().filter(o -> o.getDisplayOrder().equals(2)).findFirst().get();
        ProcedureInstruction testInstruction3 = testInstructions.stream().filter(o -> o.getDisplayOrder().equals(3)).findFirst().get();

        // query the database for this procedure
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());

        // get the procedureDetails
        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();

        // it should have three instructions
        assertEquals(procedureDetails.getProcedureInstructions().size(), 3);

        // now get procedure approvals
        ProcedureApproval procedureApproval = TestProcedureApprovalData.PROCEDURE_APPROVAL_1.getTestData();
        procedureApproval.setProcedureDetails(procedureDetails);

        ProcedureApproval procedureApprovalInDb = TestProcedureDAO.saveProcedureApprovalWrapper(procedureApproval);
        TestProcedureDAO.transitionToWaitingWrapper(procedureApprovalInDb, procedureApproval.getProcedureDetails().getProcedureApprovalDueDate().getTime());
        procedureApprovalInDb = TestProcedureDAO.setApprovalFlagWrapper(procedureApprovalInDb, procedureApproval.getIsApproved());

        // get the procedure from the database
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());

        // ought to have one procedure details
        assertEquals(procedureDef.getProcedureDetails().size(), 1);

        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();

        // should have an approval
        assertNotNull(procedureDetails.getProcedureApprovals());
        assertEquals(procedureDetails.getProcedureApprovals().size(), 1);

        // now we have a full procedure in the database.
        // our procedure has 1 approval, 3 instructions
        // two top-level level groups - 1st group contains two steps and three subgroups
        // the second of the three subgroups has one step
        NewProcData npd = new NewProcData();
        npd.setName("Test Procedure 1 - Clone");
        npd.setDescription("This is a unit test clone of procedure 1");
        npd.setProgram(procedureDef.getProgram().getPk());
        npd.setSubsystem(procedureDef.getSubsystem().getPk());
        Users user = procedureDetails.getProcedureHeader().getUser();

        ProcedureDef clonedProcedure = TestProcedureDAO.cloneProcedureWrapper(npd, procedureDetails.getPk(), user);

        assertNotNull(clonedProcedure);
        assertNotNull(clonedProcedure.getPk());
        procedurePksAddedToDb.add(clonedProcedure.getPk());
        assertNotEquals(procedureDef.getPk(), clonedProcedure.getPk());

        assertEquals(clonedProcedure.getDescription(), npd.getDescription());
        assertEquals(clonedProcedure.getName(), npd.getName());
        assertEquals(clonedProcedure.getProgram().getPk(), npd.getProgram());
        assertEquals(clonedProcedure.getSubsystem().getPk(), npd.getSubsystem());

        // cloned procedure ought to have one procedure details
        assertNotNull(clonedProcedure.getProcedureDetails());
        assertEquals(clonedProcedure.getProcedureDetails().size(), 1);

        ProcedureDetails clonedProcedureDetails = clonedProcedure.getProcedureDetails().stream().findFirst().get();

        // the two procedure details should not have the same pk, but other fields should be the same
        assertNotEquals(clonedProcedureDetails.getPk(), procedureDetails.getPk());
        assertEquals(clonedProcedureDetails.getProcedureDefVersion(), procedureDetails.getProcedureDefVersion());
        assertEquals(clonedProcedureDetails.getStatus(), ProcedureStatus.DRAFT);
        assertEquals(clonedProcedureDetails.getEditType(), EditType.ORIGINAL);
        assertNull(clonedProcedureDetails.getOriginalProcedureDetails());
        assertNull(clonedProcedureDetails.getRun());
        assertNull(clonedProcedureDetails.getRunNumber());
        assertNull(clonedProcedureDetails.getProcedureDetailRuns());

        // should not have approvals
        assertNull(clonedProcedureDetails.getProcedureApprovals());

        // history: the cloned revision should record which procedure it was cloned from
        assertEquals(1, clonedProcedureDetails.getHistories().size());
        History cloneHistory = clonedProcedureDetails.getHistories().stream().findFirst().get();
        assertEquals("Cloned from procedure " + procedureDetails.getId() + " (" + procedureDetails.getProcedureDef().getName() + ")",
                cloneHistory.getDescription());
        assertEquals(user.getUserId(), cloneHistory.getUser().getUserId());

        // procedure header user should match test data, other fields not equal to procedureDetails.getHeader
        ProcedureHeader clonedHeader = clonedProcedureDetails.getProcedureHeader();
        assertEquals(clonedHeader.getUser().getUserId(), user.getUserId());
        assertNotEquals(clonedHeader.getCreationDate(), procedureDetails.getProcedureHeader().getCreationDate());
        assertNotEquals(clonedHeader.getPk(), procedureDetails.getProcedureHeader().getPk());

        // check procedure instructions
        // cloned procedure should have three instructions
        assertEquals(clonedProcedureDetails.getProcedureInstructions().size(), 3);

        SortedSet<ProcedureInstruction> procedureInstructions = procedureDetails.getProcedureInstructions();
        ProcedureInstruction procedureInstruction1 = procedureInstructions.stream().filter(o -> o.getDisplayOrder().equals(1)).findFirst().get();
        ProcedureInstruction procedureInstruction2 = procedureInstructions.stream().filter(o -> o.getDisplayOrder().equals(2)).findFirst().get();
        ProcedureInstruction procedureInstruction3 = procedureInstructions.stream().filter(o -> o.getDisplayOrder().equals(3)).findFirst().get();

        SortedSet<ProcedureInstruction> clonedInstructions = clonedProcedureDetails.getProcedureInstructions();
        ProcedureInstruction cloneInstruction1 = clonedInstructions.stream().filter(o -> o.getDisplayOrder().equals(1)).findFirst().get();
        ProcedureInstruction cloneInstruction2 = clonedInstructions.stream().filter(o -> o.getDisplayOrder().equals(2)).findFirst().get();
        ProcedureInstruction cloneInstruction3 = clonedInstructions.stream().filter(o -> o.getDisplayOrder().equals(3)).findFirst().get();

        assertNotNull(cloneInstruction1.getPk());
        assertNotEquals(cloneInstruction1.getPk(), procedureInstruction1.getPk());
        assertEquals(procedureInstruction1.getSectionName(), cloneInstruction1.getSectionName());
        assertEquals(procedureInstruction1.getDisplayOrder(), cloneInstruction1.getDisplayOrder());
        assertEquals(cloneInstruction1.getEditType(), EditType.ORIGINAL);
        assertEquals(procedureInstruction1.getText(), cloneInstruction1.getText());

        assertNotNull(cloneInstruction2.getPk());
        assertNotEquals(cloneInstruction2.getPk(), procedureInstruction2.getPk());
        assertEquals(procedureInstruction2.getSectionName(), cloneInstruction2.getSectionName());
        assertEquals(procedureInstruction2.getDisplayOrder(), cloneInstruction2.getDisplayOrder());
        assertEquals(cloneInstruction2.getEditType(), EditType.ORIGINAL);
        assertEquals(procedureInstruction2.getText(), cloneInstruction2.getText());

        assertNotNull(procedureInstruction3.getPk());
        assertNotEquals(cloneInstruction3.getPk(), procedureInstruction3.getPk());
        assertEquals(procedureInstruction3.getSectionName(), cloneInstruction3.getSectionName());
        assertEquals(procedureInstruction3.getDisplayOrder(), cloneInstruction3.getDisplayOrder());
        assertEquals(cloneInstruction3.getEditType(), EditType.ORIGINAL);
        assertEquals(procedureInstruction3.getText(), cloneInstruction3.getText());

        // now check the step groups and steps
        // cloned procedure should have two top level groups
        assertEquals(clonedProcedureDetails.getStepGroupDefs().size(), 2);

        groupsFromDb = procedureDetails.getStepGroupDefs();
        group1FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
        StepGroupDef group2FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();

        SortedSet<StepGroupDef> topLevelGroupsFromClone = clonedProcedureDetails.getStepGroupDefs();
        StepGroupDef group1FromClone = topLevelGroupsFromClone.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
        StepGroupDef group2FromClone = topLevelGroupsFromClone.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();

        // compare the top level groups
        assertNotNull(group1FromClone.getPk());
        assertNotEquals(group1FromClone.getPk(), group1FromDb.getPk());
        assertNull(group1FromClone.getStepGroupDefParent());
        // first group should have three subgroups
        assertNotNull(group1FromDb.getStepGroupDefsChildren());
        assertEquals(group1FromDb.getStepGroupDefsChildren().size(), 3);

        assertNotNull(group1FromClone.getStepGroupDefsChildren());
        assertEquals(group1FromClone.getStepGroupDefsChildren().size(), 3);
        // first group should have two steps
        assertEquals(group1FromClone.getStepDefs().size(), 2);
        assertEquals(group1FromDb.getDisplayOrder(), group1FromClone.getDisplayOrder());
        assertEquals(group1FromDb.getStepGroupName(), group1FromClone.getStepGroupName());
        assertEquals(group1FromDb.getDescription(), group1FromClone.getDescription());

        assertNotNull(group2FromClone.getPk());
        assertNotEquals(group2FromClone.getPk(), group2FromDb.getPk());
        assertNull(group2FromClone.getStepGroupDefParent());
        assertEquals(group2FromClone.getStepGroupDefsChildren().size(), 0);
        assertEquals(group2FromClone.getStepDefs().size(), 0);
        assertEquals(group2FromClone.getDisplayOrder(), group2FromDb.getDisplayOrder());
        assertEquals(group2FromClone.getStepGroupName(), group2FromDb.getStepGroupName());
        assertEquals(group2FromClone.getDescription(), group2FromDb.getDescription());

        // go back to group 1 and compare subgroups and steps
        // start with steps
        SortedSet<StepDef> stepsFromGroup1Db = group1FromDb.getStepDefs();
        StepDef step1FromGroup1Db = stepsFromGroup1Db.stream().filter(s -> s.getDisplayOrder().equals(1)).findFirst().get();
        StepDef step2FromGroup1Db = stepsFromGroup1Db.stream().filter(s -> s.getDisplayOrder().equals(2)).findFirst().get();

        SortedSet<StepDef> stepsFromGroup1Clone = group1FromClone.getStepDefs();
        StepDef step1FromGroup1Clone = stepsFromGroup1Clone.stream().filter(s -> s.getDisplayOrder().equals(1)).findFirst().get();
        StepDef step2FromGroup1Clone = stepsFromGroup1Clone.stream().filter(s -> s.getDisplayOrder().equals(2)).findFirst().get();

        // steps should have unique PKs, a step group parent, and their fields should match . All the run related
        // fields should be empty (for sets) or null
        assertNotNull(step1FromGroup1Clone.getPk());
        assertNotEquals(step1FromGroup1Clone.getPk(), step1FromGroup1Db.getPk());
        assertEquals(step1FromGroup1Clone.getType(), step1FromGroup1Db.getType());
        assertEquals(step1FromGroup1Clone.getStepGroupDef().getPk(), group1FromClone.getPk());
        assertEquals(step1FromGroup1Clone.getEditType(), EditType.ORIGINAL);
        assertEquals(step1FromGroup1Clone.getAllowEquipmentEntry(), step1FromGroup1Db.getAllowEquipmentEntry());
        assertEquals(step1FromGroup1Clone.getDisplayOrder(), step1FromGroup1Db.getDisplayOrder());
        assertEquals(step1FromGroup1Clone.getEsd0(), step1FromGroup1Db.getEsd0());
        assertEquals(step1FromGroup1Clone.getHazardous(), step1FromGroup1Db.getHazardous());
        assertEquals(step1FromGroup1Clone.getRequireWitness(), step1FromGroup1Db.getRequireWitness());
        assertEquals(step1FromGroup1Clone.getMandatoryInspection(), step1FromGroup1Db.getMandatoryInspection());
        assertEquals(step1FromGroup1Clone.getStepName(), step1FromGroup1Db.getStepName());
        assertEquals(step1FromGroup1Clone.getInstructions(), step1FromGroup1Db.getInstructions());
        assertNull(step1FromGroup1Clone.getRunStepComments());
        assertNull(step1FromGroup1Clone.getBlackLineComments());
        assertNull(step1FromGroup1Clone.getEquipment());
        assertNull(step1FromGroup1Clone.getHistories());
        assertFalse(step1FromGroup1Clone.getIsManualValidation());
        assertNull(step1FromGroup1Clone.getMandatoryInspectionSecondSignature());
        assertNull(step1FromGroup1Clone.getWitnessSecondSignature());
        assertNull(step1FromGroup1Clone.getRunValueEntryUser());
        assertNull(step1FromGroup1Clone.getRunValueSavedTimestamp());

        assertNotNull(step2FromGroup1Clone.getPk());
        assertNotEquals(step2FromGroup1Clone.getPk(), step2FromGroup1Db.getPk());
        assertEquals(step2FromGroup1Clone.getType(), step2FromGroup1Db.getType());
        assertEquals(step2FromGroup1Clone.getStepGroupDef().getPk(), group1FromClone.getPk());
        assertEquals(step2FromGroup1Clone.getEditType(), EditType.ORIGINAL);
        assertEquals(step2FromGroup1Clone.getAllowEquipmentEntry(), step2FromGroup1Db.getAllowEquipmentEntry());
        assertEquals(step2FromGroup1Clone.getDisplayOrder(), step2FromGroup1Db.getDisplayOrder());
        assertEquals(step2FromGroup1Clone.getEsd0(), step2FromGroup1Db.getEsd0());
        assertEquals(step2FromGroup1Clone.getHazardous(), step2FromGroup1Db.getHazardous());
        assertEquals(step2FromGroup1Clone.getRequireWitness(), step2FromGroup1Db.getRequireWitness());
        assertEquals(step2FromGroup1Clone.getMandatoryInspection(), step2FromGroup1Db.getMandatoryInspection());
        assertEquals(step2FromGroup1Clone.getStepName(), step2FromGroup1Db.getStepName());
        assertEquals(step2FromGroup1Clone.getInstructions(), step2FromGroup1Db.getInstructions());
        assertNull(step2FromGroup1Clone.getRunStepComments());
        assertNull(step2FromGroup1Clone.getBlackLineComments());
        assertNull(step2FromGroup1Clone.getEquipment());
        assertNull(step2FromGroup1Clone.getHistories());
        assertFalse(step2FromGroup1Clone.getIsManualValidation());
        assertNull(step2FromGroup1Clone.getMandatoryInspectionSecondSignature());
        assertNull(step2FromGroup1Clone.getWitnessSecondSignature());
        assertNull(step2FromGroup1Clone.getRunValueEntryUser());
        assertNull(step2FromGroup1Clone.getRunValueSavedTimestamp());

        // group1FromClone ought to have 3 subgroups
        assertNotNull(group1FromClone.getStepGroupDefsChildren());
        assertEquals(group1FromClone.getStepGroupDefsChildren().size(), 3);

        subgroups11FromDb = group1FromDb.getStepGroupDefsChildren();
        subgroup11FromDb = subgroups11FromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
        subgroup12FromDb = subgroups11FromDb.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();
        subgroup13FromDb = subgroups11FromDb.stream().filter(g -> g.getDisplayOrder().equals(3)).findFirst().get();

        SortedSet<StepGroupDef> subgroupsFromGroup1Clone = group1FromClone.getStepGroupDefsChildren();
        StepGroupDef subgroup11FromClone = subgroupsFromGroup1Clone.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
        StepGroupDef subgroup12FromClone = subgroupsFromGroup1Clone.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();
        StepGroupDef subgroup13FromClone = subgroupsFromGroup1Clone.stream().filter(g -> g.getDisplayOrder().equals(3)).findFirst().get();

        // compare the subgroups
        assertNotNull(subgroup11FromClone.getPk());
        assertNotEquals(subgroup11FromClone.getPk(), subgroup11FromDb.getPk());
        assertNotNull(subgroup11FromClone.getStepGroupDefParent());
        assertEquals(subgroup11FromClone.getStepGroupDefParent().getPk(), group1FromClone.getPk());
        // subgroup11FromClone has neither subgroups nor children
        assertEquals(subgroup11FromClone.getStepGroupDefsChildren().size(), 0);
        assertEquals(subgroup11FromClone.getStepDefs().size(), 0);
        assertEquals(subgroup11FromClone.getStepGroupName(), subgroup11FromDb.getStepGroupName());
        assertEquals(subgroup11FromClone.getDescription(), subgroup11FromDb.getDescription());
        assertEquals(subgroup11FromDb.getDisplayOrder(), subgroup11FromClone.getDisplayOrder());

        assertNotNull(subgroup12FromClone.getPk());
        assertNotEquals(subgroup12FromClone.getPk(), subgroup12FromDb.getPk());
        assertNotNull(subgroup12FromClone.getStepGroupDefParent());
        assertEquals(subgroup12FromClone.getStepGroupDefParent().getPk(), group1FromClone.getPk());
        // subgroup12FromClone has one step and no subgroups
        assertEquals(subgroup12FromClone.getStepGroupDefsChildren().size(), 0);
        assertEquals(subgroup12FromClone.getStepDefs().size(), 1);
        assertEquals(subgroup12FromClone.getStepGroupName(), subgroup12FromDb.getStepGroupName());
        assertEquals(subgroup12FromClone.getDescription(), subgroup12FromDb.getDescription());
        assertEquals(subgroup12FromDb.getDisplayOrder(), subgroup12FromClone.getDisplayOrder());

        assertNotNull(subgroup13FromClone.getPk());
        assertNotEquals(subgroup13FromClone.getPk(), subgroup13FromDb.getPk());
        assertNotNull(subgroup13FromClone.getStepGroupDefParent());
        assertEquals(subgroup13FromClone.getStepGroupDefParent().getPk(), group1FromClone.getPk());
        // subgroup13FromClone has neither subgroups nor children
        assertEquals(subgroup13FromClone.getStepGroupDefsChildren().size(), 0);
        assertEquals(subgroup13FromClone.getStepDefs().size(), 0);
        assertEquals(subgroup13FromClone.getStepGroupName(), subgroup13FromDb.getStepGroupName());
        assertEquals(subgroup13FromClone.getDescription(), subgroup13FromDb.getDescription());
        assertEquals(subgroup13FromClone.getDisplayOrder(), subgroup13FromDb.getDisplayOrder());

        // finally, subgroup 2 should have one table step - check that
        stepsFromDb = subgroup12FromDb.getStepDefs();
        StepDef step1FromSubgroup12FromDb = stepsFromDb.stream().filter(s -> s.getDisplayOrder().equals(1)).findFirst().get();

        SortedSet<StepDef> stepsFromSubgroup12Clone = subgroup12FromClone.getStepDefs();
        StepDef step1FromSubgroup12Clone = stepsFromSubgroup12Clone.stream().filter(s -> s.getDisplayOrder().equals(1)).findFirst().get();

        // steps should have unique PK, a step group parent, and their fields should match. All the run related
        // fields should be empty (for sets) or null
        assertNotNull(step1FromSubgroup12Clone.getPk());
        assertNotEquals(step1FromSubgroup12Clone.getPk(), step1FromSubgroup12FromDb.getPk());
        assertEquals(step1FromSubgroup12Clone.getType(), step1FromSubgroup12FromDb.getType());
        assertEquals(step1FromSubgroup12Clone.getStepGroupDef().getPk(), subgroup12FromClone.getPk());
        assertEquals(step1FromSubgroup12Clone.getEditType(), EditType.ORIGINAL);
        assertEquals(step1FromSubgroup12Clone.getAllowEquipmentEntry(), step1FromSubgroup12FromDb.getAllowEquipmentEntry());
        assertEquals(step1FromSubgroup12Clone.getDisplayOrder(), step1FromSubgroup12FromDb.getDisplayOrder());
        assertEquals(step1FromSubgroup12Clone.getEsd0(), step1FromSubgroup12FromDb.getEsd0());
        assertEquals(step1FromSubgroup12Clone.getHazardous(), step1FromSubgroup12FromDb.getHazardous());
        assertEquals(step1FromSubgroup12Clone.getRequireWitness(), step1FromSubgroup12FromDb.getRequireWitness());
        assertEquals(step1FromSubgroup12Clone.getMandatoryInspection(), step1FromSubgroup12FromDb.getMandatoryInspection());
        assertEquals(step1FromSubgroup12Clone.getStepName(), step1FromSubgroup12FromDb.getStepName());
        assertEquals(step1FromSubgroup12Clone.getInstructions(), step1FromSubgroup12FromDb.getInstructions());
        assertNull(step1FromSubgroup12Clone.getRunStepComments());
        assertNull(step1FromSubgroup12Clone.getBlackLineComments());
        assertNull(step1FromSubgroup12Clone.getEquipment());
        assertNull(step1FromSubgroup12Clone.getHistories());
        assertFalse(step1FromSubgroup12Clone.getIsManualValidation());
        assertNull(step1FromSubgroup12Clone.getMandatoryInspectionSecondSignature());
        assertNull(step1FromSubgroup12Clone.getWitnessSecondSignature());
        assertNull(step1FromSubgroup12Clone.getRunValueEntryUser());
        assertNull(step1FromSubgroup12Clone.getRunValueSavedTimestamp());

        // check the step's rows and cells
        StepTable stepTableFromDb = (StepTable) step1FromSubgroup12FromDb;
        StepTable stepTableFromClone = (StepTable) step1FromSubgroup12Clone;

        // table should have two rows
        assertNotNull(stepTableFromClone.getStepTableRows());
        assertEquals(stepTableFromClone.getStepTableRows().size(), 2);

        SortedSet<StepTableRow> rowsFromDb = stepTableFromDb.getStepTableRows();
        StepTableRow row1Db = rowsFromDb.first();
        StepTableRow row2Db = rowsFromDb.last();

        SortedSet<StepTableRow> rowsFromClone = stepTableFromClone.getStepTableRows();
        StepTableRow row1Clone = rowsFromClone.first();
        StepTableRow row2Clone = rowsFromClone.last();

        // checking first row
        assertNotNull(row1Clone.getPk());
        assertNotEquals(row1Clone.getPk(), row1Db.getPk());
        assertEquals(row1Clone.getRowNumber(), row1Db.getRowNumber());
        // row one should have two cells
        assertNotNull(row1Clone.getStepTableCells());
        assertEquals(row1Clone.getStepTableCells().size(), 2);

        SortedSet<StepTableCell> cellsClone = row1Clone.getStepTableCells();
        StepTableCell cell1Clone = cellsClone.first();
        StepTableCell cell2Clone = cellsClone.last();

        SortedSet<StepTableCell> cellsDb = row1Db.getStepTableCells();
        StepTableCell cell1Db = cellsDb.first();
        StepTableCell cell2Db = cellsDb.last();

        assertNotNull(cell1Clone.getPk());
        assertNotEquals(cell1Clone.getPk(), cell1Db.getPk());
        assertEquals(cell1Clone.getNonEditableValue(), cell1Db.getNonEditableValue());
        assertEquals(cell1Clone.getEditable(), cell1Db.getEditable());
        assertEquals(cell1Clone.getCellIndex(), cell1Db.getCellIndex());

        assertNotNull(cell2Clone.getPk());
        assertNotEquals(cell2Clone.getPk(), cell2Db.getPk());
        assertEquals(cell2Clone.getNonEditableValue(), cell2Db.getNonEditableValue());
        assertEquals(cell2Clone.getEditable(), cell2Db.getEditable());
        assertEquals(cell2Clone.getCellIndex(), cell2Db.getCellIndex());

        // checking second row
        assertNotNull(row2Clone.getPk());
        assertNotEquals(row2Clone.getPk(), row2Db.getPk());
        assertEquals(row2Clone.getRowNumber(), row2Db.getRowNumber());
        // row two should have two cells
        assertNotNull(row2Clone.getStepTableCells());
        assertEquals(row2Clone.getStepTableCells().size(), 2);

        cellsClone = row2Clone.getStepTableCells();
        cell1Clone = cellsClone.first();
        cell2Clone = cellsClone.last();

        cellsDb = row2Db.getStepTableCells();
        cell1Db = cellsDb.first();
        cell2Db = cellsDb.last();

        assertNotNull(cell1Clone.getPk());
        assertNotEquals(cell1Clone.getPk(), cell1Db.getPk());
        assertEquals(cell1Clone.getNonEditableValue(), cell1Db.getNonEditableValue());
        assertEquals(cell1Clone.getEditable(), cell1Db.getEditable());
        assertEquals(cell1Clone.getCellIndex(), cell1Db.getCellIndex());

        assertNotNull(cell2Clone.getPk());
        assertNotEquals(cell2Clone.getPk(), cell2Db.getPk());
        assertEquals(cell2Clone.getNonEditableValue(), cell2Db.getNonEditableValue());
        assertEquals(cell2Clone.getEditable(), cell2Db.getEditable());
        assertEquals(cell2Clone.getCellIndex(), cell2Db.getCellIndex());

        // done!

        // now delete this procedure from the database to set up for next test
        Integer procedurePk = procedureDef.getPk();
        Integer clonePk = clonedProcedure.getPk();
        TestProcedureDAO.deleteProcedureDef(procedurePk);
        TestProcedureDAO.deleteProcedureDef(clonePk);

        // confirm deletion
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedurePk);
        assertNull(procedureDef);

        clonedProcedure = TestProcedureDAO.getProcedureDefByPk(clonePk);
        assertNull(clonedProcedure);

        procedurePksAddedToDb.remove(procedurePk);
        procedurePksAddedToDb.remove(clonePk);
    }

    @Test
    public void testCloningDoesNotCopyRuns()
    {
        for (Integer pk : procedurePksAddedToDb)
        {
            TestProcedureDAO.deleteProcedureDef(pk);
        }

        // first create a procedure
        // create a procedure
        ProcedureDef procedureDef = TestProcedureDAO.createProcedure();
        assertNotNull(procedureDef.getPk());
        procedurePksAddedToDb.add(procedureDef.getPk());

        // assert that only have one procedure details
        assertNotNull(procedureDef.getProcedureDetails());
        assertEquals(procedureDef.getProcedureDetails().size(), 1);

        // get the first procedure details - we'll add step groups to it
        ProcedureDetails procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();
        assertNotNull(procedureDetails.getPk());

        // it shouldn't currently have any step groups
        assertNull(procedureDetails.getStepGroupDefs());

        // get some step group test data
        // this first set will be our top level groups
        SortedSet<StepGroupDef> testTopLevelGroups = TestProcedureStepGroupData.STEP_GROUP_SET_12.getTestData();
        StepGroupDef group1FromTest = testTopLevelGroups.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
        StepGroupDef group2FromTest = testTopLevelGroups.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();

        // for each one, set to procedureDetails and save to database
        SortedSet<StepGroupDef> savedGroups = new TreeSet<>();
        for (StepGroupDef group : testTopLevelGroups)
        {
            group.setProcedureDetails(procedureDetails);
            group.setStepGroupDefParent(null);
            savedGroups.add(TestProcedureDAO.saveStepGroupDefWrapper(group));
        }

        // query the database for the procedure
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());

        // get the procedureDetails
        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();

        // it should have two step groups in the top level
        assertEquals(procedureDetails.getStepGroupDefs().size(), 2);

        SortedSet<StepGroupDef> groupsFromDb = procedureDetails.getStepGroupDefs();
        StepGroupDef group1FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();

        // now let's add subgroups to group1FromDb.
        SortedSet<StepGroupDef> testSubGroup11 = TestProcedureStepGroupData.STEP_GROUP_SET_ABC.getTestData();
        StepGroupDef subgroupAFromTest = testSubGroup11.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
        StepGroupDef subgroupBFromTest = testSubGroup11.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();
        StepGroupDef subgroupCFromTest = testSubGroup11.stream().filter(g -> g.getDisplayOrder().equals(3)).findFirst().get();

        // for each one, set to procedureDetails, parent to group1FromDb and save to database
        savedGroups = new TreeSet<>();
        for (StepGroupDef group : testSubGroup11)
        {
            group.setProcedureDetails(procedureDetails);
            group.setStepGroupDefParent(group1FromDb);
            savedGroups.add(TestProcedureDAO.saveStepGroupDefWrapper(group));
        }

        // query the database for the procedure
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());

        // get the procedureDetails
        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();

        // it should have two step groups in the top level
        assertEquals(procedureDetails.getStepGroupDefs().size(), 2);

        groupsFromDb = procedureDetails.getStepGroupDefs();
        group1FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();

        // group1FromDb ought to have 3 subgroups
        assertNotNull(group1FromDb.getStepGroupDefsChildren());
        assertEquals(group1FromDb.getStepGroupDefsChildren().size(), 3);

        SortedSet<StepGroupDef> subgroups11FromDb = group1FromDb.getStepGroupDefsChildren();
        StepGroupDef subgroup11FromDb = subgroups11FromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();
        StepGroupDef subgroup12FromDb = subgroups11FromDb.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();
        StepGroupDef subgroup13FromDb = subgroups11FromDb.stream().filter(g -> g.getDisplayOrder().equals(3)).findFirst().get();

        // now let's add steps
        // get some test step data for checkbox and single value steps
        SortedSet<StepDef> testStepDefs = TestProcedureStepData.TEST_STEP_DATA_SET_1.getTestData();
        StepDef testStep1 = testStepDefs.stream().filter(s -> s.getDisplayOrder().equals(1)).findFirst().get();
        StepDef testStep2 = testStepDefs.stream().filter(s -> s.getDisplayOrder().equals(2)).findFirst().get();

        // we'll add these two steps to group1FromDb
        SortedSet<StepDef> savedSteps = new TreeSet<>();
        for (StepDef step : testStepDefs)
        {
            step.setStepGroupDef(group1FromDb);
            savedSteps.add(TestProcedureDAO.saveStepDefWrapper(step));
        }

        // query the database for the procedure
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());

        // get the procedureDetails
        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();

        // it should have two step groups in the top level
        assertEquals(procedureDetails.getStepGroupDefs().size(), 2);

        groupsFromDb = procedureDetails.getStepGroupDefs();
        group1FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();

        // group1FromDb should have 2 steps
        assertEquals(group1FromDb.getStepDefs().size(), 2);

        SortedSet<StepDef> stepsFromDb = group1FromDb.getStepDefs();
        StepDef step1FromDb = stepsFromDb.stream().filter(s -> s.getDisplayOrder().equals(1)).findFirst().get();
        StepDef step2FromDb = stepsFromDb.stream().filter(s -> s.getDisplayOrder().equals(2)).findFirst().get();

        // let's add a table step to subgroup12FromDb
        SortedSet<StepDef> testStepData = TestProcedureStepData.TEST_STEP_DATA_SET_2.getTestData();
        StepDef testStepTable1 = testStepData.stream().filter(s -> s.getDisplayOrder().equals(1)).findFirst().get();

        // we'll add this step to subgroup12FromDb
        savedSteps = new TreeSet<>();
        for (StepDef step : testStepData)
        {
            step.setStepGroupDef(subgroup12FromDb);
            savedSteps.add(TestProcedureDAO.saveStepDefWrapper(step));
        }

        // query the database for the procedure
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());

        // get the procedureDetails
        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();

        // it should have two step groups in the top level
        assertEquals(procedureDetails.getStepGroupDefs().size(), 2);

        groupsFromDb = procedureDetails.getStepGroupDefs();
        group1FromDb = groupsFromDb.stream().filter(g -> g.getDisplayOrder().equals(1)).findFirst().get();

        // group1FromDb ought to have 3 subgroups
        assertNotNull(group1FromDb.getStepGroupDefsChildren());
        assertEquals(group1FromDb.getStepGroupDefsChildren().size(), 3);

        subgroups11FromDb = group1FromDb.getStepGroupDefsChildren();
        subgroup12FromDb = subgroups11FromDb.stream().filter(g -> g.getDisplayOrder().equals(2)).findFirst().get();

        // subgroup12FromDb should have a step
        assertNotNull(subgroup12FromDb.getStepDefs());
        assertEquals(subgroup12FromDb.getStepDefs().size(), 1);

        // steps and step groups are added. Now add procedure instructions
        // get some instruction test data
        SortedSet<ProcedureInstruction> testInstructions = TestProcedureInstructionData.PROCEDURE_INSTRUCTION_SET_1.getTestData();

        // go through and set the procedure details for each instruction, then save each instruction to the database
        for (ProcedureInstruction instruction : testInstructions)
        {
            instruction.setProcedureDetails(procedureDetails);
            TestProcedureDAO.saveProcedureInstructionSectionWrapper(instruction);
        }

        ProcedureInstruction testInstruction1 = testInstructions.stream().filter(o -> o.getDisplayOrder().equals(1)).findFirst().get();
        ProcedureInstruction testInstruction2 = testInstructions.stream().filter(o -> o.getDisplayOrder().equals(2)).findFirst().get();
        ProcedureInstruction testInstruction3 = testInstructions.stream().filter(o -> o.getDisplayOrder().equals(3)).findFirst().get();

        // query the database for this procedure
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());

        // get the procedureDetails
        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();

        // it should have three instructions
        assertEquals(procedureDetails.getProcedureInstructions().size(), 3);

        // now get procedure approvals
        ProcedureApproval procedureApproval = TestProcedureApprovalData.PROCEDURE_APPROVAL_1.getTestData();
        procedureApproval.setProcedureDetails(procedureDetails);

        ProcedureApproval procedureApprovalInDb = TestProcedureDAO.saveProcedureApprovalWrapper(procedureApproval);
        TestProcedureDAO.transitionToWaitingWrapper(procedureApprovalInDb, procedureApproval.getProcedureDetails().getProcedureApprovalDueDate().getTime());
        procedureApprovalInDb = TestProcedureDAO.setApprovalFlagWrapper(procedureApprovalInDb, procedureApproval.getIsApproved());

        // get the procedure from the database
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());

        // ought to have one procedure details
        assertEquals(procedureDef.getProcedureDetails().size(), 1);

        procedureDetails = procedureDef.getProcedureDetails().stream().findFirst().get();

        // should have an approval
        assertNotNull(procedureDetails.getProcedureApprovals());
        assertEquals(procedureDetails.getProcedureApprovals().size(), 1);

        // now we have a full procedure in the database.
        // our procedure has 1 approval, 3 instructions
        // two top-level level groups - 1st group contains two steps and three subgroups
        // the second of the three subgroups has one step

        // now let's create a run
        Users user = procedureDetails.getProcedureHeader().getUser();
        Integer runNumber = 1;
        Run runData = new Run();
        runData.setProcedureDetails(procedureDetails);
        runData.setName("Unit Test Run Creation");
        runData.setDescription("This is a unit test of a created run");
        runData.setTestingPhase(TestProcedureDAO.getAllTestingPhases().get(0));
        ProcedureDetails run = TestProcedureDAO.createRunWrapper(procedureDetails, runData, runNumber, user);

        // get the procedure from the database
        // get the procedure from the database
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedureDef.getPk());

        // ought to have two procedure details
        assertEquals(procedureDef.getProcedureDetails().size(), 2);

        // the two should be one original procedure details and one run.
        procedureDetails = procedureDef.getProcedureDetails().stream().filter(p -> p.getEditType().equals(EditType.ORIGINAL)).findFirst().get();
        run = procedureDef.getProcedureDetails().stream().filter(p -> p.getEditType().equals(EditType.RUN)).findFirst().get();

        // now, we're going to clone the original procedure
        NewProcData npd = new NewProcData();
        npd.setName("Test Procedure 1 - Clone");
        npd.setDescription("This is a unit test clone of procedure 1");
        npd.setProgram(procedureDef.getProgram().getPk());
        npd.setSubsystem(procedureDef.getSubsystem().getPk());
        user = procedureDetails.getProcedureHeader().getUser();

        ProcedureDef clonedProcedure = TestProcedureDAO.cloneProcedureWrapper(npd, procedureDetails.getPk(), user);

        // the cloned procedure should have only one procedure details (an original) and no runs
        assertNotNull(clonedProcedure.getProcedureDetails());
        assertEquals(clonedProcedure.getProcedureDetails().size(), 1);

        ProcedureDetails clonedProcedureDetails = clonedProcedure.getProcedureDetails().stream().findFirst().get();

        // check that it is edit type original with no run information.
        assertNotEquals(clonedProcedureDetails.getPk(), procedureDetails.getPk());
        assertEquals(clonedProcedureDetails.getProcedureDefVersion(), 1);
        assertEquals(clonedProcedureDetails.getStatus(), ProcedureStatus.DRAFT);
        assertEquals(clonedProcedureDetails.getEditType(), EditType.ORIGINAL);
        assertNull(clonedProcedureDetails.getOriginalProcedureDetails());
        assertNull(clonedProcedureDetails.getRun());
        assertNull(clonedProcedureDetails.getRunNumber());
        assertNull(clonedProcedureDetails.getProcedureDetailRuns());

        // done!

        // now delete this procedure from the database to set up for next test
        Integer procedurePk = procedureDef.getPk();
        Integer clonePk = clonedProcedure.getPk();
        TestProcedureDAO.deleteProcedureDef(procedurePk);
        TestProcedureDAO.deleteProcedureDef(clonePk);

        // confirm deletion
        procedureDef = TestProcedureDAO.getProcedureDefByPk(procedurePk);
        assertNull(procedureDef);

        clonedProcedure = TestProcedureDAO.getProcedureDefByPk(clonePk);
        assertNull(clonedProcedure);

        procedurePksAddedToDb.remove(procedurePk);
        procedurePksAddedToDb.remove(clonePk);

    }
}
