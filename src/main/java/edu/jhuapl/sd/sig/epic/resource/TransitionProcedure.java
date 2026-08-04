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

import edu.jhuapl.sd.sig.epic.data.ProcedureApproverDAO;
import edu.jhuapl.sd.sig.epic.data.ProcedureDetailsDAO;
import edu.jhuapl.sd.sig.epic.data.UsersDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.*;
import edu.jhuapl.sd.sig.epic.model.ProcedureDetails;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import edu.jhuapl.sd.sig.epic.startup.EmailEngine;

import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Secured
@Path("/TransitionProcedure")
public class TransitionProcedure
{

    @Context
    SecurityContext sc;

    private static final Logger LOGGER = LogManager.getLogger();

    @Path("/DRAFT")
    @POST
    @Produces({MediaType.APPLICATION_JSON})
    public ProcedureDetails returnToDraft(@QueryParam("pk") Integer procedureDetailsId)
    {
        EntityManager em = null;

        ProcedureDetails pdv = null;

        try
        {
            em = JPAUtils.getEntityManager();
            Users currentUser = UsersDAO.getUserByUsername(em, sc.getUserPrincipal().getName());
            pdv = ProcedureDetailsDAO.returnToDraft(em, procedureDetailsId, currentUser);
            EmailEngine.getInstance().sendMessage(MessageType.NOTIFICATION_PROCEDURE_DRAFT, pdv);
        }
        catch (Exception e)
        {
            LOGGER.error("Unable to transition procedure to draft status", e);
            throw e;

        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return pdv;
    }

    @Path("/READY")
    @POST
    @Produces({MediaType.APPLICATION_JSON})
    public ProcedureDetails readyForRelease(@QueryParam("pk") Integer procedureDetailsPk)
    {
        EntityManager em = null;

        ProcedureDetails pdv = null;

        try
        {
            em = JPAUtils.getEntityManager();
            Users currentUser = UsersDAO.getUserByUsername(em, sc.getUserPrincipal().getName());

            pdv = ProcedureDetailsDAO.transitionToReadyForRelease(em, procedureDetailsPk, currentUser);
        }
        catch (Exception e)
        {
            LOGGER.error("Unable to transition procedure to released status", e);
            throw e;

        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return pdv;
    }

    @Path("/WAITING")
    @POST
    @Produces({MediaType.APPLICATION_JSON})
    public ProcedureDetails transitionToWaiting(@QueryParam("pk") Integer procedureDetailsPk, @QueryParam("dueDate") Long dueDate)
    {
        EntityManager em = null;

        ProcedureDetails pdv = null;

        try
        {
            em = JPAUtils.getEntityManager();
            Users currentUser = UsersDAO.getUserByUsername(em, sc.getUserPrincipal().getName());

            pdv = ProcedureDetailsDAO.transitionToWaiting(em, procedureDetailsPk, dueDate, currentUser);

            //check if db update worked, if so email approvers
            if (pdv.getStatus() == ProcedureStatus.WAITING)
                EmailEngine.getInstance().sendMessage(MessageType.NOTIFICATION_PROCEDURE_WAITING, pdv);

        }
        catch (Exception e)
        {
            LOGGER.error("Unable to transition procedure to Waiting for Approval", e);
            throw e;

        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return pdv;
    }

    @Path("/ApproveForRelease")
    @POST
    @Produces({MediaType.APPLICATION_JSON})
    public Response approveForRelease(@QueryParam("pk") Integer procedureApprovalId, @QueryParam("approved") String approvedString)
    {
        EntityManager em = null;

        ProcedureApproval pa = null;
        try
        {

            em = JPAUtils.getEntityManager();
            Boolean approved = null;
            if (!approvedString.equalsIgnoreCase("null"))
            {
                approved = Boolean.parseBoolean(approvedString);
            }
            pa = ProcedureApproverDAO.setApprovalFlag(em, procedureApprovalId, approved);
            ApprovalWithProcedureDetails approvalWithProcedureDetails = new ApprovalWithProcedureDetails();
            approvalWithProcedureDetails.setApproval(pa);
            ProcedureDetails procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, pa.getProcedureDetails().getPk());
            approvalWithProcedureDetails.setProcedureDetails(procedureDetails);
            return Response.ok().entity(approvalWithProcedureDetails).build();
        }
        catch (Exception e)
        {
            String message = "Unable to record approval decision";
            LOGGER.error(message, e);
            return Response.ok().entity("{\"error\": \"" + message + "\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @Setter
    @Getter
    class ApprovalWithProcedureDetails
    {
        private ProcedureApproval approval;
        private ProcedureDetails procedureDetails;
    }
}
