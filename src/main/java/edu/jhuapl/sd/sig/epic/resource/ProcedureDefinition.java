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
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.EditType;
import edu.jhuapl.sd.sig.epic.model.ProcedureDef;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import edu.jhuapl.sd.sig.epic.resource.util.GenericExceptionMapper;
import edu.jhuapl.sd.sig.epic.resource.model.ProcedureDefAttributes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Secured
@Path("/ProcedureDefinition")
public class ProcedureDefinition
{

    @Context
    SecurityContext sc;

    private static final Logger LOGGER = LogManager.getLogger();
    private GenericExceptionMapper gem = new GenericExceptionMapper();

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getProcedureDefByPk(@QueryParam("pk") Integer pk)
    {
        ProcedureDef pd = null;
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            pd = JPAUtils.getRecordById(em, ProcedureDef.class, pk);

            for (edu.jhuapl.sd.sig.epic.model.ProcedureDetails pdv : pd.getProcedureDetails())
            {
                if (pdv.getEditType().equals(EditType.ORIGINAL))
                {
                    Hibernate.initialize(pdv.getProcedureDetailRuns());
                }
            }

            return Response.status(Response.Status.OK).entity(pd).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Problem while retrieving a procedure def with pk: " + pk, e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: Failed to retrieve procedure def " +
                "with pk: " + pk).build();
    }

    @POST
    @Path("/ChangeProcedureDefAttributes")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateProcedureDefAttributes(ProcedureDefAttributes procedureDefAttributes)
    {
        ProcedureDef pd = null;
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            pd = ProcedureDAO.updateProcedureDefAttributes(em, procedureDefAttributes);
        }
        catch (Exception e)
        {
            String errorMessage = "There was a problem saving procedure name and/or description";
            LOGGER.error(errorMessage, e);
            return gem.toResponse(e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return Response.status(Response.Status.OK).entity(pd).build();
    }
}
