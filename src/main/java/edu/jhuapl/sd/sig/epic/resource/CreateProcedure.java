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
import edu.jhuapl.sd.sig.epic.data.UsersDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.*;
import edu.jhuapl.sd.sig.epic.model.display.CreateProcedureSelections;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import edu.jhuapl.sd.sig.epic.resource.model.NewProcData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Secured
@Path("/CreateProcedure")
public class CreateProcedure
{

    @Context
    SecurityContext sc;

    private static final Logger LOGGER = LogManager.getLogger();

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public CreateProcedureSelections getCreateProcSelections()
    {
        CreateProcedureSelections cps = new CreateProcedureSelections();

        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            cps.setPrograms(JPAUtils.getAllRecordsForTable(em, Program.class));
            cps.setSubsystems(JPAUtils.getAllRecordsForTable(em, Subsystem.class));
            cps.setTestingPhases(JPAUtils.getAllRecordsForTable(em, TestingPhase.class));
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }

        return cps;

    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response createNewProcedure(NewProcData npd)
    {
        EntityManager em = null;

        ProcedureDef pd;
        try
        {
            em = JPAUtils.getEntityManager();

            String username = sc.getUserPrincipal().getName();
            Users user = UsersDAO.getUserByUsername(em, username);

            if (!ProcedureDAO.isProcedureDefNameUnique(em, npd.getName()))
            {
                return Response.status(Response.Status.PRECONDITION_FAILED).entity("Procedure name must be unique").build();
            }
            else
            {
                pd = ProcedureDAO.createNewProcedure(em, npd.getName(), npd.getDescription(), npd.getProgram(), npd.getSubsystem(), npd.getEsd0(), npd.getHazardous(), npd.getHazardDescription(),
                        user.getUserId());
                return Response.status(Response.Status.CREATED).entity(pd).build();
            }
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            LOGGER.error("Unable to create new procedure", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to create new procedure. See logs for details.").build();

        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }
}
