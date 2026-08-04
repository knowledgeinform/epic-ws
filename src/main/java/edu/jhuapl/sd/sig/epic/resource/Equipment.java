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

import edu.jhuapl.sd.sig.epic.data.EquipmentListDAO;
import edu.jhuapl.sd.sig.epic.data.RunDAO;
import edu.jhuapl.sd.sig.epic.data.UsersDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.EquipmentList;
import edu.jhuapl.sd.sig.epic.model.Run;
import edu.jhuapl.sd.sig.epic.model.StepDef;
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
@Path("/Equipment")
public class Equipment
{
    @Context
    SecurityContext sc;

    private static final Logger LOGGER = LogManager.getLogger();

    @POST
    @Path("/UpdateItem/{runPk}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateEquipmentList(
            @PathParam("runPk") Integer runPk,
            @QueryParam("stepPk") Integer stepPk,
            EquipmentList equipment)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            String username = sc.getUserPrincipal().getName();
            Users user = UsersDAO.getUserByUsername(em, username);

            equipment = EquipmentListDAO.setEquipmentListItemForRun(em, runPk, stepPk, equipment, user);
            return Response.ok().entity(equipment).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Problem while updating Equipment Item", e);
            return Response.serverError().entity("Could not update equipment item").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }

    }

    @POST
    @Path("RemoveItem")
    @Produces({MediaType.APPLICATION_JSON})
    public Response removeItemFromStep(
            @QueryParam("equipmentPk") Integer equipmentPk,
            @QueryParam("stepPk") Integer stepPk)
    {
        EntityManager em = null;
        StepDef step = null;

        try
        {
            em = JPAUtils.getEntityManager();
            String username = sc.getUserPrincipal().getName();
            Users user = UsersDAO.getUserByUsername(em, username);

            step = EquipmentListDAO.removeItemFromStep(em, equipmentPk, stepPk, user);
            return Response.ok().entity(step).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Problem while removing a step from the Equipment Item", e);
            return Response.serverError().entity("Could not remove step from equipment item").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @DELETE
    @Path("DeleteItem/{pk}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteEquipmentListItem(
            @PathParam("pk") int pk)
    {
        EntityManager em = null;
        Run run;
        try
        {
            em = JPAUtils.getEntityManager();
            String username = sc.getUserPrincipal().getName();
            Users user = UsersDAO.getUserByUsername(em, username);
            run = EquipmentListDAO.deleteEquipmentListItem(em, pk, user);
            return Response.status(Response.Status.OK).entity(RunDAO.getRunByUniqueCode(em, run.getProcedureDetails().getId())).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to delete Equipment List Item", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to delete the equipment item").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }
}
