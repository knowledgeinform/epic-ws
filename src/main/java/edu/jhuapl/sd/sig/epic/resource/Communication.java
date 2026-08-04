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

import edu.jhuapl.sd.sig.epic.data.CommunicationBannerDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.CommunicationBanner;
import edu.jhuapl.sd.sig.epic.model.util.DateTimeUtils;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 * Communication REST controller.
 * Contains REST endpoints related to communication banner.
 */
@Secured
@Path("/Communication")
public class Communication
{

    @Context
    SecurityContext sc;

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Save communication banner
     * 
     * @param communicationBanner communication banner details
     * @return HTTP status
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("/PostBanner")
    public Response postBanner(CommunicationBanner communicationBanner)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            CommunicationBannerDAO.postCommunicationBanner(em, communicationBanner);
            return Response.status(Response.Status.OK).build();

        }
        catch (Exception e)
        {
            LOGGER.error("Failed to save communication banner", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Failed to save communication banner. See logs for details.\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    /**
     * Delete communication banner.
     *
     * @return HTTP status code
     */
    @DELETE
    @Path("/RemoveBanner")
    public Response removeBanner()
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            CommunicationBannerDAO.removeCommunicationBanner(em);
            return Response.status(Response.Status.OK).build();

        }
        catch (Exception e)
        {
            LOGGER.error("Failed to remove communication banner", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Failed to remove communication banner. See logs for details.\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    /**
     * Get communication banner.
     *
     * @return communication banner details
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/GetBanner")
    public Response getBanner()
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            CommunicationBanner communicationBanner = CommunicationBannerDAO.getCommunicationBanner(em);

            if (communicationBanner != null && communicationBanner.getExpiry() != null)
            {
                if (DateTimeUtils.isCommunicationBannerExpired(communicationBanner.getExpiry()))
                {
                    CommunicationBannerDAO.removeCommunicationBanner(em);
                    communicationBanner = null;
                }
            }

            return Response.status(Response.Status.OK)
                    .entity(communicationBanner)
                    .build();
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to get communication banner", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Failed to get communication banner. See logs for details.\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }
}
