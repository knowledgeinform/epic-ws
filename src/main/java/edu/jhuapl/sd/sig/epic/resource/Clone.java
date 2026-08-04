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
import edu.jhuapl.sd.sig.epic.data.StepGroupDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.EditType;
import edu.jhuapl.sd.sig.epic.model.StepDef;
import edu.jhuapl.sd.sig.epic.model.StepGroupDef;
import edu.jhuapl.sd.sig.epic.model.StepType;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import edu.jhuapl.sd.sig.epic.resource.model.StepsAndGroupsData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;

@Secured
@Path("/Clone")
public class Clone
{

    @Context
    SecurityContext sc;

    private static final Logger LOGGER = LogManager.getLogger();

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/StepToGroup")
    public Response cloneStepToGroup(@QueryParam("stepPk") Integer stepPk, @QueryParam("groupPk") Integer groupPk)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            // if copying a step through this endpoint, must be in authoring mode, so editType should always be original
            StepDef newStep = StepDAO.copyStepToGroup(em, stepPk, groupPk, EditType.ORIGINAL, null, null);

            return Response.status(Response.Status.CREATED).entity(newStep).build();

        }
        catch (Exception e)
        {
            LOGGER.error("Unable to copy step to group", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Failed to copy step. See logs for details.\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/ChangeStepType")
    public Response changeStepType(@QueryParam("stepPk") Integer stepPk, @QueryParam("stepType") StepType stepType)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            StepDef newStep = StepDAO.changeStepType(em, stepPk, stepType);

            return Response.status(Response.Status.CREATED).entity(newStep).build();

        }
        catch (Exception e)
        {
            LOGGER.error("Unable to copy step to group", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Failed to copy step. See logs for details.\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/GroupToGroup")
    public Response cloneGroupToGroup(@QueryParam("groupPk") Integer groupPk, @QueryParam("parentGroupPk") Integer parentGroupPk, @QueryParam("procedurePk") Integer procedurePk)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            // if copying a step group through this endpoint, must be in authoring mode, so editType should always be original
            StepGroupDef newGroup = StepGroupDAO.copyGroupToGroup(em, groupPk, parentGroupPk, procedurePk, EditType.ORIGINAL, null);

            return Response.status(Response.Status.CREATED).entity(newGroup).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Unable to copy group to group", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Failed to copy group. See logs for details.\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/StepsToNewProcedure")
    public Response cloneStepsToNewProcedure(@QueryParam("procedureDetailsPk") Integer procedureDetailsPk, @QueryParam("parentGroupPk") Integer parentGroupPk,
            StepsAndGroupsData data)
    {
        EntityManager em = null;
        try
        {

            em = JPAUtils.getEntityManager();

            // if copying steps through this endpoint, must be in authoring mode, so editType should always be original
            List<StepGroupDef> newGroups = StepGroupDAO.copyStepsHierarchyToGroup(em, data.getSteps(), data.getGroups(), parentGroupPk, procedureDetailsPk, EditType.ORIGINAL, null);

            return Response.status(Response.Status.CREATED).entity(newGroups).build();

        }
        catch (Exception e)
        {
            LOGGER.error("Unable to copy steps to new procedure", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Failed to copy steps to new procedure. See logs for details.\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

}
