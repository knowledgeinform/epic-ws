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
import edu.jhuapl.sd.sig.epic.data.UsersDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.ProcedureDetails;
import edu.jhuapl.sd.sig.epic.model.Users;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Secured
@Path("/ProcedureDetails")
public class Procedure
{

    @Context
    SecurityContext sc;

    private static final Logger LOGGER = LogManager.getLogger();

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getProcedureDetailsByPk(@QueryParam("pk") Integer pk)
    {
        ProcedureDetails pd = null;
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            pd = JPAUtils.getRecordById(em, ProcedureDetails.class, pk);
            Hibernate.initialize(pd.getProcedureDetailRuns());

            return Response.status(Response.Status.OK).entity(pd).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Problem while retrieving a procedure details with pk: " + pk, e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: Failed to retrieve procedure " +
                "details with pk: " + pk).build();
    }

    @PUT
    @Path("/Hazard")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateProcedureDetailsHazard(@QueryParam("pk") Integer pk, @QueryParam("isHazardous") Boolean isHazardous,
            @QueryParam("hazardDescription") String hazardDescription)
    {
        edu.jhuapl.sd.sig.epic.model.ProcedureDetails procedureDetails = null;
        EntityManager em = null;

        try
        {
            em = JPAUtils.getEntityManager();
            procedureDetails = ProcedureDetailsDAO.updateProcedureDetailsHazard(em, pk, isHazardous, hazardDescription);
        }
        catch (Exception e)
        {
            LOGGER.error("Problem saving hazard information for procedure details", e);
            return Response.status(Response.Status.OK).entity("{\"error\": \"Could not save hazard information: " + e.getMessage() + "\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return Response.status(Response.Status.OK).entity(procedureDetails).build();
    }

    @PUT
    @Path("Esd0")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateProcedureDetailsEsd0(
            @QueryParam("pk") Integer pk,
            @QueryParam("val") boolean val)
    {
        edu.jhuapl.sd.sig.epic.model.ProcedureDetails procedureDetails = null;
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            procedureDetails = ProcedureDetailsDAO.updateProcedureDetailsEsd0(em, pk, val);
        }
        catch (Exception e)
        {
            LOGGER.error("Problem saving esd0 information for procedure details", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Could not save ESD Class 0 information").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return Response.status(Response.Status.OK).entity(procedureDetails).build();
    }

    @POST
    @Path("CreateRevision")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response createRevision(
            @QueryParam("id") String id)
    {
        edu.jhuapl.sd.sig.epic.model.ProcedureDetails pd = null;
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            Users user = UsersDAO.getUserByUsername(em, sc.getUserPrincipal().getName());

            edu.jhuapl.sd.sig.epic.model.ProcedureDetails latestRedlinedProc = ProcedureDetailsDAO.getLatestVersionForProcedure(em, id);

            pd = ProcedureDetailsDAO.createRevision(em, latestRedlinedProc.getId(), user);
        }
        catch (Exception e)
        {
            LOGGER.error("Problem creating a procedure details revision:", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Could not create procedure details revision.").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return Response.status(Response.Status.OK).entity(pd).build();
    }
}
