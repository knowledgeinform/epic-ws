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

import edu.jhuapl.sd.sig.epic.data.UsersDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.Users;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Secured
@Path("/ProcedureFavorites")
public class ProcedureFavorites
{
    private static final Logger LOGGER = LogManager.getLogger();

    @Context
    SecurityContext sc;

    @POST
    @Path("/Toggle")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response toggleFavorite(@QueryParam("procedureDetailPk") Integer procedureDetailPk, @QueryParam("isSave") Boolean isSave)
    {
        EntityManager em = null;
        Users user;
        try
        {
            em = JPAUtils.getEntityManager();

            String username = sc.getUserPrincipal().getName();

            user = UsersDAO.toggleFavorite(em, procedureDetailPk, username, isSave);

        }
        catch (Exception e)
        {
            LOGGER.error("Problem Toggling Favorite", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Could not save favorite information").build();

        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return Response.status(Response.Status.OK).entity(user).build();
    }

}
