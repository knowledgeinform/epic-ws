/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.resource;

import edu.jhuapl.sd.sig.epic.data.ProcedureDetailsDAO;
import edu.jhuapl.sd.sig.epic.data.TestProcedureDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.ProcedureDef;
import edu.jhuapl.sd.sig.epic.model.ProcedureDetails;
import edu.jhuapl.sd.sig.epic.model.Program;
import edu.jhuapl.sd.sig.epic.model.Run;
import edu.jhuapl.sd.sig.epic.model.RunStatus;
import edu.jhuapl.sd.sig.epic.model.Subsystem;
import edu.jhuapl.sd.sig.epic.model.Users;
import edu.jhuapl.sd.sig.epic.model.display.dto.RunListDTO;
import edu.jhuapl.sd.sig.epic.model.Status.ProgramStatusDTO;
import edu.jhuapl.sd.sig.epic.utils.DataGeneratorUtils;
import edu.jhuapl.sd.sig.epic.utils.DbTestContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration (database-backed) smoke tests for the EPIC-2306 program status
 * dashboard endpoints. These verify only what requires a real schema: that the
 * JPQL compiles and runs (joins, editType filters, grouping, constructor
 * expressions) and that the end-to-end response shape is correct.
 *
 * Everything else - response/error contracts, count mapping, and status
 * validation - is covered without a database in TestStatus.
 *
 * Each test creates its own program so the status counts for that program are
 * deterministic and unaffected by any other test.
 */
public class TestStatusOperations
{
    private static EntityManager em = null;
    private static DbTestContainer container;

    // The Status resource has no injected dependencies, so it can be instantiated directly
    private final Status statusEndpoint = new Status();

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
        container.stop();
    }

    private Program createTestProgram()
    {
        Program program = new Program();
        String suffix = Long.toString(System.nanoTime());
        program.setName("Status test program " + suffix);
        program.setCode("STST" + suffix);

        JPAUtils.basicTransaction(txEm -> txEm.persist(program), "Failed to persist test program");

        return program;
    }

    /**
     * Creates a procedure in the given program and returns its original DRAFT details.
     * Each procedure gets a unique name, since procedure_def.name is globally unique.
     */
    private ProcedureDetails createDraftProcedure(Program program)
    {
        Users user = DataGeneratorUtils.getRandomUser();
        Subsystem subsystem = DataGeneratorUtils.getRandomSubsystem();
        ProcedureDef procedureDef = TestProcedureDAO.createProcedureDefWrapper(
                "Status test procedure " + System.nanoTime(), "created by TestStatusOperations",
                program.getPk(), subsystem.getPk(), false, false, "", user.getUserId());
        return procedureDef.getProcedureDetails().iterator().next();
    }

    /**
     * Transitions a freshly created DRAFT procedure to WAITING through the real
     * state transition, with an approval due date one week out.
     */
    private ProcedureDetails transitionToWaiting(ProcedureDetails draft)
    {
        EntityManager em = JPAUtils.getEntityManager();
        ProcedureDetails waiting = ProcedureDetailsDAO.transitionToWaiting(em, draft.getPk(),
                System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7), DataGeneratorUtils.getRandomUser());
        JPAUtils.closeEntityManager(em);
        return waiting;
    }

    private void setRunStatus(Run run, RunStatus status)
    {
        JPAUtils.basicTransaction(txEm ->
        {
            Run managed = txEm.find(Run.class, run.getPk());
            managed.setStatus(status);
        }, "Failed to update run status");
    }

    @Test
    public void getProgramStatus_countsProceduresAndRunsByStatus()
    {
        // Arrange: one DRAFT, one WAITING, and two READY procedures (one run each),
        // of which one run is RUNNING and one run is COMPLETED
        Program program = createTestProgram();

        ProcedureDetails draft = createDraftProcedure(program);
        ProcedureDetails waiting = transitionToWaiting(createDraftProcedure(program));
        ProcedureDetails readyA = TestProcedureDAO.transitionToReadyWrapper(createDraftProcedure(program));
        ProcedureDetails readyB = TestProcedureDAO.transitionToReadyWrapper(createDraftProcedure(program));

        // A second run on the same original would get the same run number, so each
        // run is created on its own procedure
        ProcedureDetails runningRun = DataGeneratorUtils.generateRuns(1, 1, readyA).get(0);
        ProcedureDetails completedRun = DataGeneratorUtils.generateRuns(1, 1, readyB).get(0);
        setRunStatus(completedRun.getRun(), RunStatus.COMPLETED);

        // Act
        Response response = statusEndpoint.getProgramStatus(program.getPk());

        // Assert
        assertEquals(200, response.getStatus());
        ProgramStatusDTO dto = (ProgramStatusDTO) response.getEntity();
        assertEquals(program.getPk(), dto.getProgramPk());
        assertEquals(1, dto.getProcedureStatusCounts().getDRAFT());
        assertEquals(1, dto.getProcedureStatusCounts().getWAITING());
        assertEquals(0, dto.getProcedureStatusCounts().getAPPROVED());
        assertEquals(2, dto.getProcedureStatusCounts().getREADY());
        assertEquals(1, dto.getRunStatusCounts().getRUNNING());
        assertEquals(1, dto.getRunStatusCounts().getCOMPLETED());
        assertEquals(0, dto.getRunStatusCounts().getREVIEWING());
        assertEquals(0, dto.getRunStatusCounts().getCORRECTING());
        assertEquals(0, dto.getRunStatusCounts().getAPPROVED());
        assertEquals(0, dto.getRunStatusCounts().getABANDONED());
    }

    @Test
    public void getRunsByStatus_returnsOnlyRunsWithThatStatus()
    {
        // Arrange: one RUNNING and one COMPLETED run, one on each of two READY procedures
        Program program = createTestProgram();
        ProcedureDetails readyA = TestProcedureDAO.transitionToReadyWrapper(createDraftProcedure(program));
        ProcedureDetails readyB = TestProcedureDAO.transitionToReadyWrapper(createDraftProcedure(program));
        ProcedureDetails running = DataGeneratorUtils.generateRuns(1, 1, readyA).get(0);
        ProcedureDetails completed = DataGeneratorUtils.generateRuns(1, 1, readyB).get(0);
        setRunStatus(completed.getRun(), RunStatus.COMPLETED);

        // Act
        Response response = statusEndpoint.getRunsByStatus(program.getPk(), "RUNNING");

        // Assert
        assertEquals(200, response.getStatus());
        @SuppressWarnings("unchecked")
        List<RunListDTO> runs = (List<RunListDTO>) response.getEntity();
        assertEquals(1, runs.size());
        // The reporting RunListDTO constructor does not populate runName or status,
        // so assert on the embedded Run entity (which is what the UI binds to)
        assertEquals(running.getRun().getName(), runs.get(0).getRun().getName());
        assertEquals(RunStatus.RUNNING, runs.get(0).getRun().getStatus());
    }
}
