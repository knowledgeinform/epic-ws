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

import edu.jhuapl.sd.sig.epic.data.RunDAO;
import edu.jhuapl.sd.sig.epic.data.UsersDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.*;
import edu.jhuapl.sd.sig.epic.model.ProcedureDetails;
import edu.jhuapl.sd.sig.epic.model.display.dto.RunListDTO;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import edu.jhuapl.sd.sig.epic.resource.util.GenericExceptionMapper;
import edu.jhuapl.sd.sig.epic.startup.EmailEngine;
import org.apache.logging.log4j.Level;
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
@Path("/Runs")
public class Runs
{
    @Context
    SecurityContext sc;

    private static final Logger LOGGER = LogManager.getLogger(Runs.class.getName());
    private GenericExceptionMapper gem = new GenericExceptionMapper();

    @GET
    @Path("Run/{runId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Run getRun(@PathParam("runId") String runId)
    {
        Run run = null;
        EntityManager em = null;

        try
        {
            em = JPAUtils.getEntityManager();
            run = RunDAO.getRunByUniqueCode(em, runId);
        }
        catch (Exception e)
        {
            LOGGER.error("Unable to retrieve run: ", e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }

        return run;
    }

    @GET
    @Path("Run/pk/{runPk}")
    @Produces({MediaType.APPLICATION_JSON})
    public Run getRunByPk(@PathParam("runPk") Integer runPk)
    {
        Run run = null;
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            run = RunDAO.getRunByPk(em, runPk);
        }
        catch (Exception e)
        {
            LOGGER.error("Unable to retrieve run: ", e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return run;
    }

    @GET
    @Path("/UniqueName")
    @Produces({MediaType.APPLICATION_JSON})
    public Response checkForUniqueRunName(@QueryParam("runName") String runName)
    {
        Response response;
        boolean isUnique;
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            isUnique = RunDAO.isRunNameUnique(em, runName);
            response = Response.status(Response.Status.OK).entity(isUnique).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Error when checking for unique run name", e);
            response = gem.toResponse(e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return response;
    }

    /**
     * This method is for getting all the runs associated with this user (dashboard display)
     *
     * @return runs
     */
    @GET
    @Path("/MyRuns")
    @Produces({MediaType.APPLICATION_JSON})
    public List<RunListDTO> getMyRuns()
    {

        List<RunListDTO> runs;
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            String username = sc.getUserPrincipal().getName();
            Users user = UsersDAO.getUserByUsername(em, username);

            runs = RunDAO.getFavoriteRunList(em, user.getUserId());
            return runs;
        }
        catch (Exception e)
        {
            LOGGER.log(Level.ERROR, "Unable to query for my runs", e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }

        return null;
    }

    /**
     * This method creates a new run.
     * 
     * @param pdvPk
     * @param runInfo
     * @return
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response createNewRun(@QueryParam("pdvPk") String pdvPk, @QueryParam("runNumber") Integer runNumber, Run runInfo)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            String username = sc.getUserPrincipal().getName();
            Users user = UsersDAO.getUserByUsername(em, username);

            ProcedureDetails originalPdv = JPAUtils.getRecordById(em, ProcedureDetails.class, Integer.parseInt(pdvPk));

            ProcedureDetails pdv = RunDAO.createNewRun(em, originalPdv, runNumber, runInfo, user);

            return Response.status(Response.Status.OK).entity(pdv).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Problem while creating new run", e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: Failed to create new run from " +
                "procedure def version with pk: " + pdvPk).build();
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/TransitionRunToReviewing")
    public Response transitionRunToReviewing(@QueryParam("runPk") Integer runPk)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            String username = sc.getUserPrincipal().getName();
            Users user = UsersDAO.getUserByUsername(em, username);

            Run transitionedRun = RunDAO.transitionRunToReviewing(em, runPk, user);

            //check if db update worked, if so email approvers
            if (transitionedRun.getStatus().equals(RunStatus.REVIEWING))
            {
                EmailEngine.getInstance().sendMessage(MessageType.NOTIFICATION_RUN_CLOSEOUT_WAITING, transitionedRun);
            }

            return Response.ok().entity(transitionedRun).build();

        }
        catch (Exception e)
        {
            LOGGER.error("Problem while transitioning run to REVIEWING status", e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return Response.accepted().entity("{\"error\": \"A problem occurred while transitioning the run to REVIEWING status.\"}")
                .build();
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/TransitionRunToCorrecting")
    public Response transitionRunToCorrecting(@QueryParam("runPk") Integer runPk,
            @QueryParam("onlyAdminCanTransition") Boolean onlyAdminCanTransition)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            Users user = UsersDAO.getUserByUsername(em, sc.getUserPrincipal().getName());
            Run run = JPAUtils.getRecordById(em, Run.class, runPk);

            // check if only admin can take this action or if the run is in completed status
            if (onlyAdminCanTransition || run.getStatus().equals(RunStatus.COMPLETED))
            {
                // if here, find out if the user is an admin
                if (!user.getIsAdmin())
                {
                    // if here, the user is not an admin, and this transition should not be completed.
                    LOGGER.error("A non-admin user, " + user.getDisplayName() + ", attempted to transition the run to correcting status when this action was not allowed.");
                    return Response.ok().entity(
                            "{\"error\": \"Problem while transitioning run to CORRECTING status - this action cannot be completed by a non-admin user. The unauthorized attempt has been logged.\"}")
                            .build();
                }
            }
            else
            {
                // otherwise, only an admin or the closeout submitter should be transitioning this run
                if (!user.getIsAdmin() && user.getUserId() != run.getCloseoutSubmissionUser().getUserId())
                {
                    // if here, the person attempting to put the run in correcting status is not an admin or the closeout submitter
                    LOGGER.error("A non-authorized user, " + user.getDisplayName() + ", attempted to transition the run to correcting status.");
                    return Response.ok()
                            .entity("{\"error\": \"Problem while transitioning run to CORRECTING status - this action is not allowed for this user. The unauthorized attempt has been logged.\"}")
                            .build();
                }
            }

            Run transitionedRun = RunDAO.transitionRunToCorrectingStatus(em, run, user);

            //check if db update worked, if so email approvers
            if (transitionedRun.getStatus().equals(RunStatus.CORRECTING))
            {
                EmailEngine.getInstance().sendMessage(MessageType.NOTIFICATION_RUN_CLOSEOUT_RETRACTED, transitionedRun);
            }

            return Response.ok().entity(transitionedRun).build();

        }
        catch (Exception e)
        {
            LOGGER.error("Problem while transitioning run to CORRECTING status", e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return Response.accepted().entity("{\"error\": \"A problem occurred while transitioning the run to CORRECTING status.\"}")
                .build();
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/TransitionRunToCompleted")
    public Response transitionRunToCompleted(@QueryParam("runPk") Integer runPk)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            Users user = UsersDAO.getUserByUsername(em, sc.getUserPrincipal().getName());

            Run transitionedRun = RunDAO.transitionRunToCompletedStatus(em, runPk, user);

            return Response.ok().entity(transitionedRun).build();

        }
        catch (Exception e)
        {
            LOGGER.error("Problem while transitioning run to COMPLETED status", e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return Response.accepted().entity("{\"error\": \"A problem occurred while transitioning the run to COMPLETED status.\"}")
                .build();
    }

    /*
     Use this method for TESTING purposes only
     */
    public void setSc(SecurityContext sc)
    {
        this.sc = sc;
    }
}
