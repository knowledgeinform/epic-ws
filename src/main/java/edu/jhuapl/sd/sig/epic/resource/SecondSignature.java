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

import edu.jhuapl.sd.sig.epic.data.SecondSignatureDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.*;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import edu.jhuapl.sd.sig.epic.resource.util.GenericExceptionMapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.List;

@Secured
@Path("/SecondSignature")
public class SecondSignature
{

    @Context
    SecurityContext sc;
    private GenericExceptionMapper gem = new GenericExceptionMapper();

    private static final Logger LOGGER = LogManager.getLogger();

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/RunStepSecondSignature")
    public Response saveRunStepSecondSignature(RunStepSecondSignature signatureData)
    {
        EntityManager em = null;

        try
        {
            em = JPAUtils.getEntityManager();
            StepDef stepDef = SecondSignatureDAO.saveRunStepSecondSignature(em, signatureData);

            return Response.status(Response.Status.OK).entity(stepDef).build();
        }
        catch (Exception e)
        {
            String error = "Could not save the signature for user " + signatureData.getUser().getUsername();
            LOGGER.error(error, e);
            return Response.status(Response.Status.OK).entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/BLACK_RED_LINE")
    public Response saveRedBlackLineSignature(List<BlackRedLineSignature> blackRedLineSignature)
    {
        Response response;
        EntityManager em = null;
        List<BlackRedLineSignature> savedSignatures = new ArrayList<>();

        try
        {
            em = JPAUtils.getEntityManager();
            savedSignatures = SecondSignatureDAO.saveBlackRedLineSignatureList(em, blackRedLineSignature);

            response = Response.ok().entity(savedSignatures).build();
        }

        catch (Exception e)
        {
            LOGGER.warn("Unable to save line edit signature: " + e.getMessage());
            response = gem.toResponse(e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return response;
    }
}
