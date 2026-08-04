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

import edu.jhuapl.sd.sig.epic.data.EquipmentListDAO;
import edu.jhuapl.sd.sig.epic.data.ProcedureDAO;
import edu.jhuapl.sd.sig.epic.data.RunDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.EquipmentList;
import edu.jhuapl.sd.sig.epic.model.display.dto.ProcedureListDTO;
import edu.jhuapl.sd.sig.epic.model.display.dto.RunListDTO;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import edu.jhuapl.sd.sig.epic.resource.util.GenericExceptionMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.Set;

@Secured
@Path("/Reports")
public class Reports
{
    @Context
    SecurityContext sc;

    private static final Logger LOGGER = LogManager.getLogger();
    private GenericExceptionMapper gem = new GenericExceptionMapper();

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/AllProcedures")
    public Response getAllProceduresForReport(@QueryParam("programPk") Integer programPk,
            @QueryParam("subsystemPk") Integer subsystemPk)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            List<ProcedureListDTO> procedureList = ProcedureDAO.getListOfAllProceduresForReport(em, programPk, subsystemPk);
            return Response.ok().entity(procedureList).build();
        }
        catch (Exception e)
        {
            String message = "Could not retrieve list of all procedures due to error: " + e.getMessage();
            LOGGER.error(message, e);
            return Response.ok().entity("{\"error\": \"" + message + "\"}").build();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/AllRuns")
    public Response getAllRunsForReport(@QueryParam("programPk") Integer programPk,
            @QueryParam("subsystemPk") Integer subsystemPk,
            @QueryParam("testingPhasePk") Integer testingPhasePk,
            @QueryParam("getNonConformance") Boolean getNonConformance)
    {
        EntityManager em = null;
        Response response;

        try
        {
            em = JPAUtils.getEntityManager();
            List<RunListDTO> runList = RunDAO.getListOfAllRunsForReport(em, programPk, subsystemPk, testingPhasePk, getNonConformance);
            response = Response.ok().entity(runList).build();
        }
        catch (Exception e)
        {
            String message = "Could not retrieve list of runs due to error";
            LOGGER.error(message, e);
            response = gem.toResponse(e);

        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return response;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/AllEquipment")
    public Response getAllEquipmentForReport(@QueryParam("programPk") Integer programPk,
            @QueryParam("subsystemPk") Integer subsystemPk,
            @QueryParam("testingPhasePk") Integer testingPhasePk)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            List<EquipmentList> equipmentList = EquipmentListDAO.getAllEquipmentForReport(em, programPk, subsystemPk, testingPhasePk);
            return Response.ok().entity(equipmentList).build();
        }
        catch (Exception e)
        {
            String message = "Could not retrieve list of equipment due to error";
            LOGGER.error(message, e);
            return Response.ok().entity("{\"error\": \"" + message + "\"}").build();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/RunsForEquipment")
    public Response getAllRunsForEquipmentForReport(@QueryParam("equipmentPropertyNumber") String equipmentPropertyNumber,
            @QueryParam("equipmentSerialNumber") String equipmentSerialNumber)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            Set<RunListDTO> runsForEquipment = EquipmentListDAO.getAllRunsAssociatedWithEquipment(em, equipmentPropertyNumber, equipmentSerialNumber);
            return Response.ok().entity(runsForEquipment).build();
        }
        catch (Exception e)
        {
            String message = "Could not retrieve list of runs for equipment due to error";
            LOGGER.error(message, e);
            return Response.ok().entity("{\"error\": \"" + message + "\"}").build();
        }
    }
}
