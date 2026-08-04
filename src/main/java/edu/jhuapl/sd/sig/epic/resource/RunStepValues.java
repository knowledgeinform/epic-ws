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

import edu.jhuapl.sd.sig.epic.data.StepDAO;
import edu.jhuapl.sd.sig.epic.data.UsersDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.StepCheckbox;
import edu.jhuapl.sd.sig.epic.model.StepDef;
import edu.jhuapl.sd.sig.epic.model.StepSingleValue;
import edu.jhuapl.sd.sig.epic.model.StepTable;
import edu.jhuapl.sd.sig.epic.model.StepTableCell;
import edu.jhuapl.sd.sig.epic.model.Users;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import edu.jhuapl.sd.sig.epic.resource.util.GenericExceptionMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Secured
@Path("/RunValue")
public class RunStepValues
{

    @Context
    SecurityContext sc;

    private static final Logger LOGGER = LogManager.getLogger();
    private GenericExceptionMapper gem = new GenericExceptionMapper();

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/SINGLE_VALUE") // must be the same as StepType.SINGLE_VALUE
    public Response saveSingleValueRunStepValue(StepRunDataRequestBody request)
    {
        StepSingleValue runStepDef;
        EntityManager em = null;

        try
        {
            em = JPAUtils.getEntityManager();

            String username = sc.getUserPrincipal().getName();
            Users user = UsersDAO.getUserByUsername(em, username);

            runStepDef = StepDAO.saveRunValueForSingleValueStep(em, (StepSingleValue) request.data, user);
            return Response.status(Response.Status.OK).entity(runStepDef).build();
        }
        catch (WebApplicationException e)
        {
            LOGGER.error("Web application exception in saving single value step run value for step with pk of " + request.data.getPk(), e);
            throw e;
        }
        catch (Exception e)
        {
            LOGGER.error("Error attempting to save run value for single value step", e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("error: Failed to save run value").build();
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/CHECKBOX") // must be the same as StepType.CHECKBOX
    public Response saveCheckboxRunStepValue(StepRunDataRequestBody request)
    {
        StepCheckbox runStepDef;
        EntityManager em = null;

        try
        {
            em = JPAUtils.getEntityManager();

            String username = sc.getUserPrincipal().getName();
            Users user = UsersDAO.getUserByUsername(em, username);

            runStepDef = StepDAO.saveRunValueForCheckboxStep(em, (StepCheckbox) request.data, user);
            return Response.status(Response.Status.OK).entity(runStepDef).build();
        }
        catch (WebApplicationException e)
        {
            LOGGER.error("Web application exception in saving checkbox step run value for step with pk of " + request.data.getPk(), e);
            throw e;
        }
        catch (Exception e)
        {
            LOGGER.error("Error attempting to save run value for checkbox step", e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("error: Failed to save run value").build();
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/TABLE") // must be the same as StepType.TABLE
    public Response saveTableRunStepValue(StepRunDataRequestBody request)
    {
        StepTable runStepDef;
        EntityManager em = null;
        Response response;

        try
        {
            em = JPAUtils.getEntityManager();

            String username = sc.getUserPrincipal().getName();
            Users user = UsersDAO.getUserByUsername(em, username);

            runStepDef = StepDAO.saveRunValueForTableStep(em, (StepTable) request.data, request.dataCell, user);
            response = Response.status(Response.Status.OK).entity(runStepDef).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Error attempting to save run value for table step with pk of " + request.data.getPk(), e);
            response = gem.toResponse(e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return response;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class StepRunDataRequestBody
    {
        StepDef data;
        StepTableCell dataCell;

    }
}
