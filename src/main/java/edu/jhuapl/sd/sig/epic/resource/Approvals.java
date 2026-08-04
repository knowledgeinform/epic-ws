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

import edu.jhuapl.sd.sig.epic.data.*;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.ProcedureApprovalType;
import edu.jhuapl.sd.sig.epic.model.Run;
import edu.jhuapl.sd.sig.epic.model.RunApproval;
import edu.jhuapl.sd.sig.epic.model.Users;
import edu.jhuapl.sd.sig.epic.model.display.AllApprovals;
import edu.jhuapl.sd.sig.epic.model.display.dto.ProcApprovalDashboardDTO;
import edu.jhuapl.sd.sig.epic.model.display.dto.RunApprovalDashboardDTO;
import edu.jhuapl.sd.sig.epic.model.ProcedureApproval;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import edu.jhuapl.sd.sig.epic.resource.util.GenericExceptionMapper;
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
import java.util.Set;
import java.util.SortedSet;

@Secured
@Path("/MyApprovals")
public class Approvals
{

    @Context
    SecurityContext sc;

    private static final Logger LOGGER = LogManager.getLogger();
    private GenericExceptionMapper gem = new GenericExceptionMapper();

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public AllApprovals getMyApprovals()
    {

        EntityManager em = null;

        AllApprovals allApprovals = new AllApprovals();
        try
        {
            em = JPAUtils.getEntityManager();

            String username = sc.getUserPrincipal().getName();
            Users user = UsersDAO.getUserByUsername(em, username);

            List<ProcApprovalDashboardDTO> procedureApprovals = ApprovalsDAO.getProcedureApprovalList(em, user.getUserId());
            List<RunApprovalDashboardDTO> closeoutApprovals = ApprovalsDAO.getRunApprovalList(em, user.getUserId());

            allApprovals.setProcedureApprovals(procedureApprovals);
            //			allApprovals.setWitnessApprovals(null);
            allApprovals.setCloseoutApprovals(closeoutApprovals);
            return allApprovals;
        }
        catch (Exception e)
        {
            // add logger stuff here
            LOGGER.log(Level.ERROR, "Unable to query for my approvals", e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }

        return allApprovals;
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/AddRunApproval")
    public Response addRunApproval(@QueryParam("userId") Integer userId,
            @QueryParam("approvalType") ProcedureApprovalType approvalType,
            @QueryParam("runPk") Integer runPk,
            @QueryParam("approverOrder") Integer approverOrder)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            RunApproval approval = ApprovalsDAO.saveNewRunApproval(em, userId, approvalType, runPk, approverOrder);

            return Response.status(Response.Status.CREATED).entity(approval).build();
        }
        catch (Exception e)
        {
            String message = "Problem saving run approver";
            LOGGER.error(message, e);
            return Response.status(Response.Status.CREATED).entity("{\"error\": \"" + message + "\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @DELETE
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/DeleteRunApproval")
    public Response deleteRunApproval(@QueryParam("runApprovalPk") Integer runApprovalPk)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            RunApproval runApproval = JPAUtils.getRecordById(em, RunApproval.class, runApprovalPk);

            SortedSet<RunApproval> runApprovals = ApprovalsDAO.deleteRunApproval(em, runApproval);

            return Response.ok().entity(runApprovals).build();
        }
        catch (Exception e)
        {
            String message = "Problem deleting run approver";
            LOGGER.error(message, e);
            return Response.ok().entity("{\"error\": \"" + message + "\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/UpdateRunApprovals")
    public Response updateRunApprovals(@QueryParam("runPk") Integer runPk, Set<RunApproval> runApprovals)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            runApprovals = ApprovalsDAO.updateRunApprovals(em, runApprovals);

            return Response.ok().entity(runApprovals).build();
        }
        catch (Exception e)
        {
            String message = "Problem updating run approver";
            LOGGER.error(message, e);
            return Response.ok().entity("{\"error\": \"" + message + "\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/RecordRunApprovalDecision")
    public Response recordRunApprovalDecision(RunApproval runApproval)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            Run run = ApprovalsDAO.handleRunApprovalDecision(em, runApproval);

            return Response.accepted().entity(run).build();
        }
        catch (Exception e)
        {
            String message = "Problem with updating run approval to " + runApproval.getIsApproved().toString();
            LOGGER.error(message, e);
            return Response.ok().entity("{\"error\": \"" + message + "\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @Path("/SetApproverDisabled")
    @POST
    @Produces({MediaType.APPLICATION_JSON})
    public Response approverDisabled(@QueryParam("pk") Integer procedureApprovalId,
            @QueryParam("approverDisabled") String approverDisabledString)
    {
        Response response;
        EntityManager em = null;
        try
        {
            Boolean approverDisabled = null;
            if (!approverDisabledString.equalsIgnoreCase("null"))
            {
                approverDisabled = Boolean.parseBoolean(approverDisabledString);
            }
            em = JPAUtils.getEntityManager();
            String username = sc.getUserPrincipal().getName();
            Users user = UsersDAO.getUserByUsername(em, username);
            ProcedureApproval pa = ProcedureApproverDAO.setDisabledFlag(em, procedureApprovalId, approverDisabled, user);
            response = Response.ok().entity(pa).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Error when setting approver disabled status", e);
            response = gem.toResponse(e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return response;
    }
}
