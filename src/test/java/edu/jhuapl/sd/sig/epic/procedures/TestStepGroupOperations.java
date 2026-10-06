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

import edu.jhuapl.sd.sig.epic.data.StepGroupDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.EditType;
import edu.jhuapl.sd.sig.epic.model.ProcedureDef;
import edu.jhuapl.sd.sig.epic.model.ProcedureDetails;
import edu.jhuapl.sd.sig.epic.model.StepGroupDef;
import edu.jhuapl.sd.sig.epic.resource.util.PatchUtils;
import edu.jhuapl.sd.sig.epic.utils.DataGeneratorUtils;
import edu.jhuapl.sd.sig.epic.utils.DbTestContainer;
import edu.jhuapl.sd.sig.epic.utils.TestUtils;
import org.junit.jupiter.api.*;

import javax.json.Json;
import javax.json.JsonPatch;
import javax.json.JsonReader;
import javax.persistence.EntityManager;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
public class TestStepGroupOperations
{
    private static EntityManager em = null;

    @Context
    SecurityContext sc;

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
        container.stop();
    }

    /*
     * Tests related to testing deletion of step groups
     */
    @Test
    public void testStepGroupDeletionFromProcedureTopLevelWhenFirstGroup() throws Exception
    {
        int indexToRemove = 0;

        // first generate a procedure def with 5 top level step groups
        ProcedureDef procedureDef = DataGeneratorUtils.generateProcedureDef(5, 0, 1, 10);
        ProcedureDetails procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDef.getProcedureDetails().iterator().next().getPk());
        SortedSet<StepGroupDef> topLevelGroups = procedureDetails.getStepGroupDefs();

        // create simple groups to mimic what comes from the client
        SortedSet<StepGroupDef> simpleOriginalGroups = generateSimpleGroups(topLevelGroups);

        List<StepGroupDef> allGroups = applyPatch(indexToRemove, simpleOriginalGroups, topLevelGroups);

        // allGroups should have four groups
        assertNotNull(allGroups);
        assertEquals(4, allGroups.size());

        // the four groups should be ordered 1 to 4, and nothing else about those groups should have changed
        List<StepGroupDef> originalGroups = new ArrayList<>(topLevelGroups);
        originalGroups = originalGroups.stream().sorted().collect(Collectors.toList());

        for (int i = 1; i < originalGroups.size(); i++)
        {
            assertStepGroupsAreEqual(originalGroups.get(i), allGroups.get(i - 1), false);
            assertEquals(allGroups.get(i - 1).getDisplayOrder().intValue(), i);
        }

        // there should be four groups being updated, and 1 deleted group
        List<StepGroupDef> updateTheseGroups = getUpdatedGroups(allGroups, originalGroups);
        assertEquals(4, updateTheseGroups.size());

        List<StepGroupDef> deleteTheseGroups = getDeletedGroups(allGroups, originalGroups);
        assertEquals(1, deleteTheseGroups.size());

        // update the database
        StepGroupDAO.bulkAddUpdateDeleteTransaction(em, new ArrayList<>(), updateTheseGroups, deleteTheseGroups, procedureDetails.getPk());

        // assert that the deleted group is not in the database
        assertNull(JPAUtils.getRecordById(em, StepGroupDef.class, deleteTheseGroups.get(0).getPk()));

        // assert that the database groups have the display orders updated
        for (StepGroupDef group : updateTheseGroups)
        {
            StepGroupDef groupInDb = JPAUtils.getRecordById(em, StepGroupDef.class, group.getPk());
            assertStepGroupsAreEqual(group, groupInDb, true);
        }
    }

    @Test
    public void testStepGroupDeletionFromProcedureTopLevelWhenLastGroup() throws Exception
    {
        int indexToRemove = 4;

        // first generate a procedure def with 5 top level step groups
        ProcedureDef procedureDef = DataGeneratorUtils.generateProcedureDef(5, 0, 1, 10);
        ProcedureDetails procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDef.getProcedureDetails().iterator().next().getPk());
        SortedSet<StepGroupDef> topLevelGroups = procedureDetails.getStepGroupDefs();

        // create simple groups to mimic what comes from the client
        SortedSet<StepGroupDef> simpleOriginalGroups = generateSimpleGroups(topLevelGroups);

        List<StepGroupDef> allGroups = applyPatch(indexToRemove, simpleOriginalGroups, topLevelGroups);

        // allGroups should have four groups
        assertNotNull(allGroups);
        assertEquals(4, allGroups.size());

        // the four groups should be ordered 1 to 4, and nothing else about those groups should have changed
        List<StepGroupDef> originalGroups = new ArrayList<>(topLevelGroups);
        originalGroups.stream().sorted().collect(Collectors.toList());

        for (int i = 0; i < originalGroups.size() - 1; i++)
        {
            assertStepGroupsAreEqual(originalGroups.get(i), allGroups.get(i), false);
            assertEquals(allGroups.get(i).getDisplayOrder().intValue(), i + 1);
        }

        // there should be four groups being updated, and 1 deleted group
        List<StepGroupDef> updateTheseGroups = getUpdatedGroups(allGroups, originalGroups);
        assertEquals(0, updateTheseGroups.size());

        List<StepGroupDef> deleteTheseGroups = getDeletedGroups(allGroups, originalGroups);
        assertEquals(1, deleteTheseGroups.size());

        // update the database
        StepGroupDAO.bulkAddUpdateDeleteTransaction(em, new ArrayList<>(), updateTheseGroups, deleteTheseGroups, procedureDetails.getPk());

        // assert that the deleted group is not in the database
        assertNull(JPAUtils.getRecordById(em, StepGroupDef.class, deleteTheseGroups.get(0).getPk()));

        // assert that the database groups have the display orders updated
        for (StepGroupDef group : updateTheseGroups)
        {
            StepGroupDef groupInDb = JPAUtils.getRecordById(em, StepGroupDef.class, group.getPk());
            assertStepGroupsAreEqual(group, groupInDb, true);
        }
    }

    @Test
    public void testStepGroupDeletionFromProcedureTopLevelWhenMiddleGroup() throws Exception
    {
        int indexToRemove = 2;

        // first generate a procedure def with 5 top level step groups
        ProcedureDef procedureDef = DataGeneratorUtils.generateProcedureDef(5, 0, 1, 10);
        ProcedureDetails procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDef.getProcedureDetails().iterator().next().getPk());
        SortedSet<StepGroupDef> topLevelGroups = procedureDetails.getStepGroupDefs();

        // create simple groups to mimic what comes from the client
        SortedSet<StepGroupDef> simpleOriginalGroups = generateSimpleGroups(topLevelGroups);

        List<StepGroupDef> allGroups = applyPatch(indexToRemove, simpleOriginalGroups, topLevelGroups);

        // allGroups should have four groups
        assertNotNull(allGroups);
        assertEquals(4, allGroups.size());

        // the four groups should be ordered 1 to 4, and nothing else about those groups should have changed
        List<StepGroupDef> originalGroups = new ArrayList<>(topLevelGroups);
        originalGroups.stream().sorted().collect(Collectors.toList());

        for (int i = 0; i < originalGroups.size(); i++)
        {
            if (i == 2)
                continue;
            if (i < 2)
            {
                assertStepGroupsAreEqual(originalGroups.get(i), allGroups.get(i), false);
                assertEquals(allGroups.get(i).getDisplayOrder().intValue(), i + 1);
            }
            else
            {
                assertStepGroupsAreEqual(originalGroups.get(i), allGroups.get(i - 1), false);
                assertEquals(i, allGroups.get(i - 1).getDisplayOrder().intValue());
            }
        }

        // there should be four groups being updated, and 1 deleted group
        List<StepGroupDef> updateTheseGroups = getUpdatedGroups(allGroups, originalGroups);
        assertEquals(2, updateTheseGroups.size());

        List<StepGroupDef> deleteTheseGroups = getDeletedGroups(allGroups, originalGroups);
        assertEquals(1, deleteTheseGroups.size());

        // update the database
        StepGroupDAO.bulkAddUpdateDeleteTransaction(em, new ArrayList<>(), updateTheseGroups, deleteTheseGroups, procedureDetails.getPk());

        // assert that the deleted group is not in the database
        assertNull(JPAUtils.getRecordById(em, StepGroupDef.class, deleteTheseGroups.get(0).getPk()));

        // assert that the database groups have the display orders updated
        for (StepGroupDef group : updateTheseGroups)
        {
            StepGroupDef groupInDb = JPAUtils.getRecordById(em, StepGroupDef.class, group.getPk());
            assertStepGroupsAreEqual(group, groupInDb, true);
        }
    }

    @Test
    public void testStepGroupDeletionFromParentStepGroupWhenFirstGroup() throws Exception
    {
        int indexToRemove = 0;

        // generate a procedure with no groups
        ProcedureDef procedureDef = DataGeneratorUtils.generateProcedureDef(0, 0, 1, 10);
        ProcedureDetails procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDef.getProcedureDetails().iterator().next().getPk());

        // generate a group with five subgroups for the procedure
        SortedSet<StepGroupDef> parent = DataGeneratorUtils.generateStepGroupDefs(1, 1, 0, 3, procedureDetails, null, procedureDetails, 1);
        assertEquals(1, parent.size());
        DataGeneratorUtils.generateStepGroupDefs(5, 5, 0, 3, procedureDetails, parent.first(), procedureDetails, 1);
        SortedSet<StepGroupDef> subgroups = JPAUtils.getRecordById(em, StepGroupDef.class, parent.first().getPk()).getStepGroupDefsChildren();

        // create simple groups to mimic what comes from the client
        SortedSet<StepGroupDef> simpleOriginalGroups = generateSimpleGroups(subgroups);

        List<StepGroupDef> allGroups = applyPatch(indexToRemove, simpleOriginalGroups, subgroups);

        // allGroups should have four groups
        assertNotNull(allGroups);
        assertEquals(4, allGroups.size());

        // the four groups should be ordered 1 to 4, and nothing else about those groups should have changed
        List<StepGroupDef> originalGroups = new ArrayList<>(subgroups);
        originalGroups.stream().sorted().collect(Collectors.toList());

        for (int i = 1; i < originalGroups.size(); i++)
        {
            assertStepGroupsAreEqual(originalGroups.get(i), allGroups.get(i - 1), false);
            assertEquals(allGroups.get(i - 1).getDisplayOrder().intValue(), i);
        }

        // there should be four groups being updated, and 1 deleted group
        List<StepGroupDef> updateTheseGroups = getUpdatedGroups(allGroups, originalGroups);
        assertEquals(4, updateTheseGroups.size());

        List<StepGroupDef> deleteTheseGroups = getDeletedGroups(allGroups, originalGroups);
        assertEquals(1, deleteTheseGroups.size());

        // update the database
        StepGroupDAO.bulkAddUpdateDeleteTransaction(em, new ArrayList<>(), updateTheseGroups, deleteTheseGroups, procedureDetails.getPk());

        // assert that the deleted group is not in the database
        assertNull(JPAUtils.getRecordById(em, StepGroupDef.class, deleteTheseGroups.get(0).getPk()));

        // assert that the database groups have the display orders updated
        for (StepGroupDef group : updateTheseGroups)
        {
            StepGroupDef groupInDb = JPAUtils.getRecordById(em, StepGroupDef.class, group.getPk());
            assertStepGroupsAreEqual(group, groupInDb, true);
        }
    }

    @Test
    public void testStepGroupDeletionFromParentStepGroupWhenLastGroup() throws Exception
    {
        int indexToRemove = 4;

        // generate a procedure with no groups
        ProcedureDef procedureDef = DataGeneratorUtils.generateProcedureDef(0, 0, 1, 10);
        ProcedureDetails procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDef.getProcedureDetails().iterator().next().getPk());

        // generate a group with five subgroups for the procedure
        SortedSet<StepGroupDef> parent = DataGeneratorUtils.generateStepGroupDefs(1, 1, 0, 3, procedureDetails, null, procedureDetails, 1);
        assertEquals(1, parent.size());
        DataGeneratorUtils.generateStepGroupDefs(5, 5, 0, 3, procedureDetails, parent.first(), procedureDetails, 1);
        SortedSet<StepGroupDef> subgroups = JPAUtils.getRecordById(em, StepGroupDef.class, parent.first().getPk()).getStepGroupDefsChildren();

        // create simple groups to mimic what comes from the client
        SortedSet<StepGroupDef> simpleOriginalGroups = generateSimpleGroups(subgroups);

        List<StepGroupDef> allGroups = applyPatch(indexToRemove, simpleOriginalGroups, subgroups);

        // allGroups should have four groups
        assertNotNull(allGroups);
        assertEquals(4, allGroups.size());

        // the four groups should be ordered 1 to 4, and nothing else about those groups should have changed
        List<StepGroupDef> originalGroups = new ArrayList<>(subgroups);
        originalGroups.stream().sorted().collect(Collectors.toList());

        for (int i = 0; i < originalGroups.size() - 1; i++)
        {
            assertStepGroupsAreEqual(originalGroups.get(i), allGroups.get(i), false);
            assertEquals(allGroups.get(i).getDisplayOrder().intValue(), i + 1);
        }

        // there should be four groups being updated, and 1 deleted group
        List<StepGroupDef> updateTheseGroups = getUpdatedGroups(allGroups, originalGroups);
        assertEquals(0, updateTheseGroups.size());

        List<StepGroupDef> deleteTheseGroups = getDeletedGroups(allGroups, originalGroups);
        assertEquals(1, deleteTheseGroups.size());

        // update the database
        StepGroupDAO.bulkAddUpdateDeleteTransaction(em, new ArrayList<>(), updateTheseGroups, deleteTheseGroups, procedureDetails.getPk());

        // assert that the deleted group is not in the database
        assertNull(JPAUtils.getRecordById(em, StepGroupDef.class, deleteTheseGroups.get(0).getPk()));

        // assert that the database groups have the display orders updated
        for (StepGroupDef group : updateTheseGroups)
        {
            StepGroupDef groupInDb = JPAUtils.getRecordById(em, StepGroupDef.class, group.getPk());
            assertStepGroupsAreEqual(group, groupInDb, true);
        }
    }

    @Test
    public void testStepGroupDeletionFromParentStepGroupWhenMiddleGroup() throws Exception
    {
        int indexToRemove = 2;

        // generate a procedure with no groups
        ProcedureDef procedureDef = DataGeneratorUtils.generateProcedureDef(0, 0, 1, 10);
        ProcedureDetails procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDef.getProcedureDetails().iterator().next().getPk());

        // generate a group with five subgroups for the procedure
        SortedSet<StepGroupDef> parent = DataGeneratorUtils.generateStepGroupDefs(1, 1, 0, 3, procedureDetails, null, procedureDetails, 1);
        assertEquals(1, parent.size());
        DataGeneratorUtils.generateStepGroupDefs(5, 5, 0, 3, procedureDetails, parent.first(), procedureDetails, 1);
        SortedSet<StepGroupDef> subgroups = JPAUtils.getRecordById(em, StepGroupDef.class, parent.first().getPk()).getStepGroupDefsChildren();

        SortedSet<StepGroupDef> simpleOriginalGroups = generateSimpleGroups(subgroups);

        List<StepGroupDef> allGroups = applyPatch(indexToRemove, simpleOriginalGroups, subgroups);

        // allGroups should have four groups
        assertNotNull(allGroups);
        assertEquals(4, allGroups.size());

        // the four groups should be ordered 1 to 4, and nothing else about those groups should have changed
        List<StepGroupDef> originalGroups = new ArrayList<>(subgroups);
        originalGroups.stream().sorted().collect(Collectors.toList());

        for (int i = 0; i < originalGroups.size(); i++)
        {
            if (i == 2)
                continue;
            if (i < 2)
            {
                assertStepGroupsAreEqual(originalGroups.get(i), allGroups.get(i), false);
                assertEquals(allGroups.get(i).getDisplayOrder().intValue(), i + 1);
            }
            else
            {
                assertStepGroupsAreEqual(originalGroups.get(i), allGroups.get(i - 1), false);
                assertEquals(i, allGroups.get(i - 1).getDisplayOrder().intValue());
            }
        }

        // there should be four groups being updated, and 1 deleted group
        List<StepGroupDef> updateTheseGroups = getUpdatedGroups(allGroups, originalGroups);
        assertEquals(2, updateTheseGroups.size());

        List<StepGroupDef> deleteTheseGroups = getDeletedGroups(allGroups, originalGroups);

        assertEquals(1, deleteTheseGroups.size());

        // update the database
        StepGroupDAO.bulkAddUpdateDeleteTransaction(em, new ArrayList<>(), updateTheseGroups, deleteTheseGroups, procedureDetails.getPk());

        // assert that the deleted group is not in the database
        assertNull(JPAUtils.getRecordById(em, StepGroupDef.class, deleteTheseGroups.get(0).getPk()));

        // assert that the database groups have the display orders updated
        for (StepGroupDef group : updateTheseGroups)
        {
            StepGroupDef groupInDb = JPAUtils.getRecordById(em, StepGroupDef.class, group.getPk());
            assertStepGroupsAreEqual(group, groupInDb, true);
        }
    }

    /*
     * Tests related to cloning groups/steps function
     */
    @Test
    public void testCloningOfEmptyTopLevelGroup() throws Exception
    {
        // generate a procedure with one empty group (no steps)
        ProcedureDef procedureDef = DataGeneratorUtils.generateProcedureDef(1, 0, 0, 0);
        ProcedureDetails originalProcedureDetails = procedureDef.getProcedureDetails().iterator().next();

        // double check that procedureDetails has one group with no steps
        assertNotNull(originalProcedureDetails.getStepGroupDefs());
        assertEquals(1, originalProcedureDetails.getStepGroupDefs().size());

        StepGroupDef originalGroup = originalProcedureDetails.getStepGroupDefs().first();
        assertEquals(0, originalGroup.getStepDefs().size());

        // now make a new procedure with one group.
        ProcedureDef newProcedureDef = DataGeneratorUtils.generateProcedureDef(1, 0, 0, 1);
        ProcedureDetails newProcedureDetails = newProcedureDef.getProcedureDetails().iterator().next();

        // double check that the new procedure details has one group
        assertNotNull(newProcedureDetails.getStepGroupDefs());
        assertEquals(1, newProcedureDetails.getStepGroupDefs().size());

        StepGroupDef parentGroup = newProcedureDetails.getStepGroupDefs().first();
        SortedSet<StepGroupDef> subGroups = new TreeSet<>(parentGroup.getStepGroupDefsChildren());

        // we are going to clone the originalGroup into the parentGroup
        StepGroupDef newGroup = StepGroupDAO.copyGroupToGroup(em, originalGroup.getPk(), parentGroup.getPk(), newProcedureDetails.getPk(), EditType.ORIGINAL, null);

        // get the new procedure details from the database again
        newProcedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, newProcedureDetails.getPk());

        // check that it now has one parent group and one additional subgroup
        assertNotNull(newProcedureDetails.getStepGroupDefs());
        assertEquals(1, newProcedureDetails.getStepGroupDefs().size());

        // check that there is a new subgroup
        parentGroup = newProcedureDetails.getStepGroupDefs().first();
        assertEquals(subGroups.size() + 1, parentGroup.getStepGroupDefsChildren().size());

        // assert that the last subgroup in the parent group is the new group
        assertEquals(parentGroup.getStepGroupDefsChildren().last(), newGroup);

        // check that the new group is the same as the original group (name and description)
        assertEquals(originalGroup.getStepGroupName(), newGroup.getStepGroupName());
        assertEquals(originalGroup.getDescription(), newGroup.getDescription());
    }

    @Test
    public void testCloningOfEmptySubgroupGroup() throws Exception
    {
        // generate a procedure with no groups
        ProcedureDef procedureDef = DataGeneratorUtils.generateProcedureDef(0, 0, 1, 10);
        ProcedureDetails originalProcedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDef.getProcedureDetails().iterator().next().getPk());

        // generate a group with one empty subgroup for the procedure
        SortedSet<StepGroupDef> parent = DataGeneratorUtils.generateStepGroupDefs(1, 1, 0, 3, originalProcedureDetails, null, originalProcedureDetails, 1);
        assertEquals(1, parent.size());
        DataGeneratorUtils.generateStepGroupDefs(1, 1, 0, 0, originalProcedureDetails, parent.first(), originalProcedureDetails, 1);

        em.refresh(originalProcedureDetails);
        // double check that originalProcedureDetails has one group and one empty subgroup
        assertNotNull(originalProcedureDetails.getStepGroupDefs());
        assertEquals(1, originalProcedureDetails.getStepGroupDefs().size());
        assertEquals(1, originalProcedureDetails.getStepGroupDefs().first().getStepGroupDefsChildren().size());

        StepGroupDef originalSubgroup = originalProcedureDetails.getStepGroupDefs().first().getStepGroupDefsChildren().first();

        // confirm the subgroup is empty
        assertEquals(0, originalSubgroup.getStepDefs().size());
        assertEquals(0, originalSubgroup.getStepGroupDefsChildren().size());

        // now make a new procedure with one group.
        ProcedureDef newProcedureDef = DataGeneratorUtils.generateProcedureDef(1, 0, 0, 1);
        ProcedureDetails newProcedureDetails = newProcedureDef.getProcedureDetails().iterator().next();

        // double check that the new procedure details has one group
        assertNotNull(newProcedureDetails.getStepGroupDefs());
        assertEquals(1, newProcedureDetails.getStepGroupDefs().size());
        StepGroupDef parentGroup = newProcedureDetails.getStepGroupDefs().first();
        SortedSet<StepGroupDef> subGroups = parentGroup.getStepGroupDefsChildren();

        // we are going to clone the originalSubgroup into the parentGroup
        StepGroupDef newGroup = StepGroupDAO.copyGroupToGroup(em, originalSubgroup.getPk(), parentGroup.getPk(), newProcedureDetails.getPk(), EditType.ORIGINAL, null);

        // get the new procedure details from the database again
        newProcedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, newProcedureDetails.getPk());

        // check that it now has one parent group and one additional subgroup
        assertNotNull(newProcedureDetails.getStepGroupDefs());
        assertEquals(1, newProcedureDetails.getStepGroupDefs().size());

        // check that there is a new subgroup
        parentGroup = newProcedureDetails.getStepGroupDefs().first();
        assertEquals(subGroups.size() + 1, parentGroup.getStepGroupDefsChildren().size());

        // assert that the last subgroup in the parent group is the new group
        assertEquals(parentGroup.getStepGroupDefsChildren().last(), newGroup);

        // check that the new group is the same as the original group (name and description)
        assertEquals(originalSubgroup.getStepGroupName(), newGroup.getStepGroupName());
        assertEquals(originalSubgroup.getDescription(), newGroup.getDescription());
    }

    private SortedSet<StepGroupDef> generateSimpleGroups(SortedSet<StepGroupDef> originals)
    {
        // create simple groups to mimic what comes from the client
        SortedSet<StepGroupDef> simpleOriginalGroups = new TreeSet<>();
        for (StepGroupDef group : originals)
        {
            StepGroupDef simpleGroup = new StepGroupDef();
            simpleGroup.setDisplayOrder(group.getDisplayOrder());
            simpleGroup.setStepGroupName(group.getStepGroupName());
            simpleGroup.setDescription(group.getDescription());
            simpleGroup.setPk(group.getPk());
            simpleOriginalGroups.add(simpleGroup);
        }
        return simpleOriginalGroups;
    }

    private List<StepGroupDef> applyPatch(int indexToRemove, SortedSet<StepGroupDef> simpleOriginalGroups, SortedSet<StepGroupDef> originals)
    {
        // create a patch based on the standard Json Patch format: https://tools.ietf.org/html/rfc6902 and convert to an
        // input stream; this is what comes from the client
        InputStream operationsJson = TestUtils.createRemoveAndUpdateDisplayOrderPatchForArrayAsInputStream(indexToRemove, simpleOriginalGroups);

        // create a Json patch
        JsonReader jsonReader = Json.createReader(operationsJson);
        JsonPatch patch = Json.createPatch(jsonReader.readArray());
        jsonReader.close();

        List<StepGroupDef> allGroups = null;
        try
        {
            // apply the patch to the top level groups
            allGroups = PatchUtils.updateArrayUsingJsonPatch(patch, originals, StepGroupDef.class);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
        return allGroups;
    }

    private List<StepGroupDef> getUpdatedGroups(List<StepGroupDef> allGroups, List<StepGroupDef> originalGroups)
    {
        List<StepGroupDef> updateTheseGroups = new ArrayList<>();

        allGroups.stream().forEach(group ->
        {
            boolean noMatch = true;
            for (StepGroupDef sg : originalGroups)
            {
                if (group.equals(sg))
                {
                    noMatch = false;
                    break;
                }
            }
            if (noMatch)
            {
                updateTheseGroups.add(group);
            }
        });
        return updateTheseGroups;
    }

    private List<StepGroupDef> getDeletedGroups(List<StepGroupDef> allGroups, List<StepGroupDef> originalGroups)
    {
        List<StepGroupDef> finalAllGroups = allGroups;
        List<StepGroupDef> deleteTheseGroups = originalGroups.stream().filter(g ->
        {
            for (StepGroupDef sg : finalAllGroups)
            {
                if (g.getPk().equals(sg.getPk()))
                    return false;
            }
            return true;
        }).collect(Collectors.toList());
        return deleteTheseGroups;
    }

    private void assertStepGroupsAreEqual(StepGroupDef original, StepGroupDef changed, boolean compareDisplayOrder)
    {
        assertNotNull(original);
        assertNotNull(changed);

        assertEquals(original.getPk(), changed.getPk());
        assertEquals(original.getStepGroupName(), changed.getStepGroupName());
        assertEquals(original.getDescription(), changed.getDescription());
        assertEquals(original.getEditType(), changed.getEditType());
        if (compareDisplayOrder)
        {
            assertEquals(original.getDisplayOrder(), changed.getDisplayOrder());
        }
        assertEquals(original.getStepGroupDefParent(), changed.getStepGroupDefParent());

        AtomicBoolean allMatch = new AtomicBoolean(true);
        original.getStepDefs().stream().forEach(stepDef ->
        {
            boolean found = changed.getStepDefs().stream().anyMatch(stepDef1 -> stepDef.getPk().equals(stepDef1.getPk()));
            if (!found)
            {
                allMatch.set(false);
            }
        });
        assertTrue(allMatch.get());

        allMatch.set(true);
        original.getStepGroupDefsChildren().stream().forEach(child ->
        {
            boolean found = changed.getStepGroupDefsChildren().stream().anyMatch(child1 -> child.getPk().equals(child1.getPk()));
            if (!found)
            {
                allMatch.set(false);
            }
        });
        assertTrue(allMatch.get());
    }
}
