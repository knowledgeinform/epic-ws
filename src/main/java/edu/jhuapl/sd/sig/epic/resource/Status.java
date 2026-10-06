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
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.display.dto.ProcedureListDTO;
import edu.jhuapl.sd.sig.epic.model.display.dto.RunListDTO;
import edu.jhuapl.sd.sig.epic.model.Status.ProgramStatusDTO;
import edu.jhuapl.sd.sig.epic.model.Status.ProcedureStatusCounts;
import edu.jhuapl.sd.sig.epic.model.Status.RunStatusCounts;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;

@Secured
@Path("/Status")
public class Status
{

    @Context
    SecurityContext sc;

    private static final Logger LOGGER = LogManager.getLogger(Status.class);

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/Program/{programPk}")
    public Response getProgramStatus(@PathParam("programPk") Integer programPk)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            ProcedureStatusCounts procedureStatusCounts = ProcedureDAO.getProcedureStatusCounts(em, programPk);
            RunStatusCounts runStatusCounts = RunDAO.getRunStatusCounts(em, programPk);

            ProgramStatusDTO programStatusDTO = new ProgramStatusDTO();
            programStatusDTO.setProgramPk(programPk);
            programStatusDTO.setProcedureStatusCounts(procedureStatusCounts);
            programStatusDTO.setRunStatusCounts(runStatusCounts);

            return Response.ok().entity(programStatusDTO).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Problem retrieving program status for programPk: " + programPk, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to retrieve program status\"}")
                    .build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/ProceduresByStatus")
    public Response getProceduresByStatus(@QueryParam("programPk") Integer programPk,
            @QueryParam("status") String status)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            List<ProcedureListDTO> procedures = ProcedureDAO.getProceduresByStatus(em, programPk, status);

            return Response.ok().entity(procedures).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Problem retrieving procedures by status", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to retrieve procedures by status\"}")
                    .build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/RunsByStatus")
    public Response getRunsByStatus(@QueryParam("programPk") Integer programPk,
            @QueryParam("status") String status)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            List<RunListDTO> runs = RunDAO.getRunsByStatus(em, programPk, status);

            return Response.ok().entity(runs).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Problem retrieving runs by status", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to retrieve runs by status\"}")
                    .build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }
}
