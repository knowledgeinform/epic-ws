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

import edu.jhuapl.sd.sig.epic.data.ProcedureDAO;
import edu.jhuapl.sd.sig.epic.data.RunDAO;
import edu.jhuapl.sd.sig.epic.data.util.ConfigureAPI;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.ProcedureStatus;
import edu.jhuapl.sd.sig.epic.model.RunStatus;
import edu.jhuapl.sd.sig.epic.model.Status.ProgramStatusDTO;
import edu.jhuapl.sd.sig.epic.model.Status.ProcedureStatusCounts;
import edu.jhuapl.sd.sig.epic.model.Status.RunStatusCounts;
import edu.jhuapl.sd.sig.epic.model.display.dto.ProcedureListDTO;
import edu.jhuapl.sd.sig.epic.model.display.dto.RunListDTO;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the EPIC-2306 program status dashboard: the {@link Status} resource
 * and the DAO status methods it delegates to. No database is involved -
 * JPAUtils/DAO statics are mocked at the resource level, and the DAO methods are
 * exercised with a mocked EntityManager.
 *
 * The JPQL itself is only verified against a real schema by the integration tests
 * in TestStatusOperations.
 *
 * Note the nested static-mock setup in the resource tests (same pattern as TestExport):
 * ConfigureAPI must be mocked and stubbed to "initialized" BEFORE JPAUtils is mocked,
 * because JPAUtils' static initializer calls ConfigureAPI.isInitialized() and throws
 * otherwise.
 */
public class TestStatus
{
    private static final Integer PROGRAM_PK = 42;

    // ---------------------------------------------------------------------
    // Status resource: response contract and EntityManager lifecycle
    // ---------------------------------------------------------------------

    @Test
    public void getProgramStatus_returnsDtoBuiltFromDaoCounts()
    {
        // Arrange
        EntityManager em = mock(EntityManager.class);
        ProcedureStatusCounts procedureCounts = new ProcedureStatusCounts(3, 1, 0, 2);
        RunStatusCounts runCounts = new RunStatusCounts(2, 0, 0, 1, 0, 0);

        try (MockedStatic<ConfigureAPI> mockApi = Mockito.mockStatic(ConfigureAPI.class))
        {
            mockApi.when(ConfigureAPI::isInitialized).thenReturn(true);

            try (MockedStatic<JPAUtils> mockJpa = Mockito.mockStatic(JPAUtils.class);
                    MockedStatic<ProcedureDAO> mockPdDao = Mockito.mockStatic(ProcedureDAO.class);
                    MockedStatic<RunDAO> mockRunDao = Mockito.mockStatic(RunDAO.class))
            {
                mockJpa.when(JPAUtils::getEntityManager).thenReturn(em);
                mockPdDao.when(() -> ProcedureDAO.getProcedureStatusCounts(em, PROGRAM_PK)).thenReturn(procedureCounts);
                mockRunDao.when(() -> RunDAO.getRunStatusCounts(em, PROGRAM_PK)).thenReturn(runCounts);

                // Act
                Response response = new Status().getProgramStatus(PROGRAM_PK);

                // Assert
                assertEquals(200, response.getStatus());
                ProgramStatusDTO dto = (ProgramStatusDTO) response.getEntity();
                assertEquals(PROGRAM_PK, dto.getProgramPk());
                assertSame(procedureCounts, dto.getProcedureStatusCounts());
                assertSame(runCounts, dto.getRunStatusCounts());
                mockJpa.verify(() -> JPAUtils.closeEntityManager(em));
            }
        }
    }

    @Test
    public void getProgramStatus_returnsServerError_whenDaoFails()
    {
        // Arrange
        EntityManager em = mock(EntityManager.class);

        try (MockedStatic<ConfigureAPI> mockApi = Mockito.mockStatic(ConfigureAPI.class))
        {
            mockApi.when(ConfigureAPI::isInitialized).thenReturn(true);

            try (MockedStatic<JPAUtils> mockJpa = Mockito.mockStatic(JPAUtils.class);
                    MockedStatic<ProcedureDAO> mockPdDao = Mockito.mockStatic(ProcedureDAO.class);
                    MockedStatic<RunDAO> mockRunDao = Mockito.mockStatic(RunDAO.class))
            {
                mockJpa.when(JPAUtils::getEntityManager).thenReturn(em);
                mockPdDao.when(() -> ProcedureDAO.getProcedureStatusCounts(em, PROGRAM_PK))
                        .thenThrow(new RuntimeException("db down"));

                // Act
                Response response = new Status().getProgramStatus(PROGRAM_PK);

                // Assert: TODO (EPIC-2306 follow-up, R3) - the error contract should
                // distinguish bad input (400) from server errors and return the
                // standard Error body, not a hand-rolled JSON string.
                assertEquals(500, response.getStatus());
                mockJpa.verify(() -> JPAUtils.closeEntityManager(em));
            }
        }
    }

    @Test
    public void getProceduresByStatus_returnsListFromDao()
    {
        // Arrange
        EntityManager em = mock(EntityManager.class);
        List<ProcedureListDTO> procedures = Collections.emptyList();

        try (MockedStatic<ConfigureAPI> mockApi = Mockito.mockStatic(ConfigureAPI.class))
        {
            mockApi.when(ConfigureAPI::isInitialized).thenReturn(true);

            try (MockedStatic<JPAUtils> mockJpa = Mockito.mockStatic(JPAUtils.class);
                    MockedStatic<ProcedureDAO> mockPdDao = Mockito.mockStatic(ProcedureDAO.class))
            {
                mockJpa.when(JPAUtils::getEntityManager).thenReturn(em);
                mockPdDao.when(() -> ProcedureDAO.getProceduresByStatus(em, PROGRAM_PK, "DRAFT")).thenReturn(procedures);

                // Act
                Response response = new Status().getProceduresByStatus(PROGRAM_PK, "DRAFT");

                // Assert
                assertEquals(200, response.getStatus());
                assertSame(procedures, response.getEntity());
                mockJpa.verify(() -> JPAUtils.closeEntityManager(em));
            }
        }
    }

    @Test
    public void getProceduresByStatus_returnsServerError_whenDaoFails()
    {
        // Arrange
        EntityManager em = mock(EntityManager.class);

        try (MockedStatic<ConfigureAPI> mockApi = Mockito.mockStatic(ConfigureAPI.class))
        {
            mockApi.when(ConfigureAPI::isInitialized).thenReturn(true);

            try (MockedStatic<JPAUtils> mockJpa = Mockito.mockStatic(JPAUtils.class);
                    MockedStatic<ProcedureDAO> mockPdDao = Mockito.mockStatic(ProcedureDAO.class))
            {
                mockJpa.when(JPAUtils::getEntityManager).thenReturn(em);
                mockPdDao.when(() -> ProcedureDAO.getProceduresByStatus(em, PROGRAM_PK, "DRAFT"))
                        .thenThrow(new RuntimeException("db down"));

                // Act
                Response response = new Status().getProceduresByStatus(PROGRAM_PK, "DRAFT");

                // Assert: TODO (EPIC-2306 follow-up, R3) - see getProgramStatus_returnsServerError_whenDaoFails
                assertEquals(500, response.getStatus());
                mockJpa.verify(() -> JPAUtils.closeEntityManager(em));
            }
        }
    }

    @Test
    public void getRunsByStatus_returnsListFromDao()
    {
        // Arrange
        EntityManager em = mock(EntityManager.class);
        List<RunListDTO> runs = Collections.emptyList();

        try (MockedStatic<ConfigureAPI> mockApi = Mockito.mockStatic(ConfigureAPI.class))
        {
            mockApi.when(ConfigureAPI::isInitialized).thenReturn(true);

            try (MockedStatic<JPAUtils> mockJpa = Mockito.mockStatic(JPAUtils.class);
                    MockedStatic<RunDAO> mockRunDao = Mockito.mockStatic(RunDAO.class))
            {
                mockJpa.when(JPAUtils::getEntityManager).thenReturn(em);
                mockRunDao.when(() -> RunDAO.getRunsByStatus(em, PROGRAM_PK, "RUNNING")).thenReturn(runs);

                // Act
                Response response = new Status().getRunsByStatus(PROGRAM_PK, "RUNNING");

                // Assert
                assertEquals(200, response.getStatus());
                assertSame(runs, response.getEntity());
                mockJpa.verify(() -> JPAUtils.closeEntityManager(em));
            }
        }
    }

    @Test
    public void getRunsByStatus_returnsServerError_whenDaoFails()
    {
        // Arrange
        EntityManager em = mock(EntityManager.class);

        try (MockedStatic<ConfigureAPI> mockApi = Mockito.mockStatic(ConfigureAPI.class))
        {
            mockApi.when(ConfigureAPI::isInitialized).thenReturn(true);

            try (MockedStatic<JPAUtils> mockJpa = Mockito.mockStatic(JPAUtils.class);
                    MockedStatic<RunDAO> mockRunDao = Mockito.mockStatic(RunDAO.class))
            {
                mockJpa.when(JPAUtils::getEntityManager).thenReturn(em);
                mockRunDao.when(() -> RunDAO.getRunsByStatus(em, PROGRAM_PK, "RUNNING"))
                        .thenThrow(new RuntimeException("db down"));

                // Act
                Response response = new Status().getRunsByStatus(PROGRAM_PK, "RUNNING");

                // Assert: TODO (EPIC-2306 follow-up, R3) - see getProgramStatus_returnsServerError_whenDaoFails
                assertEquals(500, response.getStatus());
                mockJpa.verify(() -> JPAUtils.closeEntityManager(em));
            }
        }
    }

    // ---------------------------------------------------------------------
    // DAO status counts: grouped-row mapping and failure behavior
    // ---------------------------------------------------------------------

    @Test
    public void getProcedureStatusCounts_mapsGroupedRowsToStatusFields()
    {
        // Arrange
        EntityManager em = mock(EntityManager.class);
        TypedQuery<Object[]> query = mock(TypedQuery.class);
        when(em.createQuery(Mockito.anyString(), Mockito.eq(Object[].class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(
                new Object[] {ProcedureStatus.DRAFT, 3L},
                new Object[] {ProcedureStatus.WAITING, 1L},
                new Object[] {ProcedureStatus.APPROVED, 2L},
                new Object[] {ProcedureStatus.READY, 5L}));

        // Act
        ProcedureStatusCounts counts = ProcedureDAO.getProcedureStatusCounts(em, PROGRAM_PK);

        // Assert
        assertEquals(3, counts.getDRAFT());
        assertEquals(1, counts.getWAITING());
        assertEquals(2, counts.getAPPROVED());
        assertEquals(5, counts.getREADY());
    }

    @Test
    public void getRunStatusCounts_mapsGroupedRowsToStatusFields()
    {
        // Arrange
        EntityManager em = mock(EntityManager.class);
        TypedQuery<Object[]> query = mock(TypedQuery.class);
        when(em.createQuery(Mockito.anyString(), Mockito.eq(Object[].class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(
                new Object[] {RunStatus.RUNNING, 4L},
                new Object[] {RunStatus.REVIEWING, 1L},
                new Object[] {RunStatus.CORRECTING, 2L},
                new Object[] {RunStatus.COMPLETED, 6L},
                new Object[] {RunStatus.APPROVED, 3L},
                new Object[] {RunStatus.ABANDONED, 0L}));

        // Act
        RunStatusCounts counts = RunDAO.getRunStatusCounts(em, PROGRAM_PK);

        // Assert
        assertEquals(4, counts.getRUNNING());
        assertEquals(1, counts.getREVIEWING());
        assertEquals(2, counts.getCORRECTING());
        assertEquals(6, counts.getCOMPLETED());
        assertEquals(3, counts.getAPPROVED());
        assertEquals(0, counts.getABANDONED());
    }

    @Test
    public void getProcedureStatusCounts_returnsZeroCounts_whenQueryFails()
    {
        // Arrange
        EntityManager em = mock(EntityManager.class);
        TypedQuery<Object[]> query = mock(TypedQuery.class);
        when(em.createQuery(Mockito.anyString(), Mockito.eq(Object[].class))).thenReturn(query);
        when(query.getResultList()).thenThrow(new RuntimeException("db down"));

        // Act
        ProcedureStatusCounts counts = ProcedureDAO.getProcedureStatusCounts(em, PROGRAM_PK);

        // Assert: current behavior - the failure is swallowed and the caller sees
        // zeros instead of an error. TODO (EPIC-2306 follow-up, R3): flip this to
        // expect the exception to propagate.
        assertEquals(0, counts.getDRAFT());
        assertEquals(0, counts.getWAITING());
        assertEquals(0, counts.getAPPROVED());
        assertEquals(0, counts.getREADY());
    }

    @Test
    public void getRunStatusCounts_returnsZeroCounts_whenQueryFails()
    {
        // Arrange
        EntityManager em = mock(EntityManager.class);
        TypedQuery<Object[]> query = mock(TypedQuery.class);
        when(em.createQuery(Mockito.anyString(), Mockito.eq(Object[].class))).thenReturn(query);
        when(query.getResultList()).thenThrow(new RuntimeException("db down"));

        // Act
        RunStatusCounts counts = RunDAO.getRunStatusCounts(em, PROGRAM_PK);

        // Assert: current behavior - the failure is swallowed and the caller sees
        // zeros instead of an error. TODO (EPIC-2306 follow-up, R3): flip this to
        // expect the exception to propagate.
        assertEquals(0, counts.getRUNNING());
        assertEquals(0, counts.getREVIEWING());
        assertEquals(0, counts.getCORRECTING());
        assertEquals(0, counts.getCOMPLETED());
        assertEquals(0, counts.getAPPROVED());
        assertEquals(0, counts.getABANDONED());
    }

    // ---------------------------------------------------------------------
    // DAO by-status: status validation
    // ---------------------------------------------------------------------

    @Test
    public void getProceduresByStatus_invalidStatus_throwsWebApplicationException()
    {
        // Arrange: the enum conversion happens before the query is executed,
        // so the mock query is never actually used
        EntityManager em = mock(EntityManager.class);
        when(em.createQuery(Mockito.anyString(), Mockito.eq(ProcedureListDTO.class))).thenReturn(mock(TypedQuery.class));

        // Act / Assert
        assertThrows(WebApplicationException.class,
                () -> ProcedureDAO.getProceduresByStatus(em, PROGRAM_PK, "NOT_A_STATUS"));
    }

    @Test
    public void getRunsByStatus_invalidStatus_throwsWebApplicationException()
    {
        // Arrange: the enum conversion happens before the query is executed,
        // so the mock query is never actually used
        EntityManager em = mock(EntityManager.class);
        when(em.createQuery(Mockito.anyString(), Mockito.eq(RunListDTO.class))).thenReturn(mock(TypedQuery.class));

        // Act / Assert
        assertThrows(WebApplicationException.class,
                () -> RunDAO.getRunsByStatus(em, PROGRAM_PK, "NOT_A_STATUS"));
    }
}
