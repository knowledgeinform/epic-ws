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
import edu.jhuapl.sd.sig.epic.data.UsersDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.MessageType;
import edu.jhuapl.sd.sig.epic.model.ProcedureApproval;
import edu.jhuapl.sd.sig.epic.model.ProcedureStatus;
import edu.jhuapl.sd.sig.epic.model.Users;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import edu.jhuapl.sd.sig.epic.resource.model.ProcedureApprovalData;
import edu.jhuapl.sd.sig.epic.resource.util.AuthorizationUtils;
import edu.jhuapl.sd.sig.epic.startup.EmailEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Secured
@Path("/SaveProcedureHeaderUserData")
public class SaveProcedureHeaderUserData
{

    @Context
    private SecurityContext sc;

    private static final Logger LOGGER = LogManager.getLogger();

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response saveProcedureHeaderUserData(ProcedureApprovalData data)
    {

        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            AuthorizationUtils.checkIsProcedureOwnerOrAdmin(sc, data.getProcedureDetailsPk());

            ProcedureApproval approval = ProcedureApproverDAO.insertNewProcedureApproval(
                    em,
                    data.getUserId(),
                    data.getApprovalType(),
                    data.getProcedureDetailsPk());

            if (approval.getProcedureDetails().getStatus() == ProcedureStatus.WAITING)
                EmailEngine.getInstance().sendMessage(MessageType.NOTIFICATION_PROCEDURE_WAITING, approval);

            return Response.status(Response.Status.CREATED).entity(approval).build();

        }
        catch (Exception e)
        {
            LOGGER.error("Problem saving procedure header approvers and reviewers", e);
            return Response.status(Response.Status.CREATED).entity("Problem saving procedure header approvers and reviewers").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }

    }

    @POST
    @Path("/Author")
    @Produces({MediaType.APPLICATION_JSON})
    public Response changeProcedureAuthor(@QueryParam("userId") int userId, @QueryParam("procedureHeaderPk") int procedureHeaderPk)
    {

        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            Users newAuthor = UsersDAO.changeAuthorOnProcedureHeader(em, procedureHeaderPk, userId);
            return Response.status(Response.Status.CREATED).entity(newAuthor).build();

        }
        catch (Exception e)
        {
            LOGGER.error("Problem changing procedure author.", e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unable to change procedure author.").build();

    }
}
