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
import edu.jhuapl.sd.sig.epic.resource.util.AuthorizationUtils;
import edu.jhuapl.sd.sig.epic.resource.util.GenericExceptionMapper;
import edu.jhuapl.sd.sig.epic.startup.EmailEngine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Secured
@Path("/DeleteProcedureHeaderApproval")
public class DeleteProcedureHeaderApproval
{
    private static final Logger LOGGER = LogManager.getLogger();
    private GenericExceptionMapper gem = new GenericExceptionMapper();

    @Context
    private SecurityContext sc;

    @DELETE
    @Path("{id}")
    @Consumes({MediaType.TEXT_PLAIN})
    public Response deleteProcedureHeaderApprover(@PathParam("id") int id)
    {
        EntityManager em = null;
        Response response;

        try
        {
            ProcedureApproval pa;
            em = JPAUtils.getEntityManager();
            pa = JPAUtils.getRecordById(em, ProcedureApproval.class, id);
            AuthorizationUtils.checkIsProcedureOwnerOrAdmin(sc, pa.getProcedureDetails());

            String username = sc.getUserPrincipal().getName();
            Users user = UsersDAO.getUserByUsername(em, username);

            Boolean status = ProcedureApproverDAO.deleteProcedureApproval(em, id, user);

            if (pa.getProcedureDetails().getStatus() == ProcedureStatus.WAITING)
                EmailEngine.getInstance().sendMessage(MessageType.NOTIFICATION_PROCEDURE_REMOVED, pa);

            response = Response.ok().entity(status).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to delete procedure approval", e);
            response = gem.toResponse(e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return response;
    }
}
