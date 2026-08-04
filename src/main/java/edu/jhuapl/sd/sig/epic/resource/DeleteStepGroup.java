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

import edu.jhuapl.sd.sig.epic.data.StepGroupDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Secured
@Path("/DeleteStepGroup")
public class DeleteStepGroup
{
    private static final Logger LOGGER = LogManager.getLogger();

    @DELETE
    @Path("{id}")
    @Consumes({MediaType.TEXT_PLAIN})
    public Response deleteStepGroup(@PathParam("id") int id)
    {
        try
        {
            boolean success = StepGroupDAO.deleteStepGroupTransaction(JPAUtils.getEntityManager(), id);
            return Response.status(Response.Status.OK).entity(
                    success).build();
        }
        catch (WebApplicationException e)
        {
            LOGGER.error("Web application Exception in deleting step group with pk of " + id, e);
            throw e;
        }
        catch (Exception e)
        {
            LOGGER.error("Problem deleting step group", e);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to delete step group").build();
    }
}
