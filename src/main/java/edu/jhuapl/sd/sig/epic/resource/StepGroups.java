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
import edu.jhuapl.sd.sig.epic.model.ProcedureDetails;
import edu.jhuapl.sd.sig.epic.model.StepGroupDef;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import edu.jhuapl.sd.sig.epic.resource.util.PatchUtils;
import edu.jhuapl.sd.sig.epic.resource.util.GenericExceptionMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;

import javax.json.*;
import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Secured
@Path("/StepGroups")
public class StepGroups
{
    private static final Logger LOGGER = LogManager.getLogger();
    private GenericExceptionMapper gem = new GenericExceptionMapper();

    @GET
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response getStepGroupsForProcedureDetail(@QueryParam("procedureDetailsPk") Integer procedureDetailsPk)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            ProcedureDetails pd = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDetailsPk);
            if (pd != null)
            {
                Set<StepGroupDef> sgList = pd.getStepGroupDefs();
                Hibernate.initialize(sgList);
                return Response.ok().entity(sgList).build();
            }
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return Response.status(Response.Status.OK).entity("{\"error\": \"Could not get list of step groups, " +
                "see logs for details\"}").build();
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response saveStepGroup(@QueryParam("procedureDetailsPk") Integer procedureDetailsPk, List<StepGroupDef> data)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            List<StepGroupDef> sgd = StepGroupDAO.insertNewStepGroupDefTransaction(em, data, procedureDetailsPk);
            return Response.ok().entity(sgd).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Problem saving step group", e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return Response.status(Response.Status.OK).entity("{\"error\": \"Could not save step group, " +
                "see logs for details\"}").build();
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateStepGroupData(List<StepGroupDef> data)
    {
        EntityManager em = null;

        try
        {
            em = JPAUtils.getEntityManager();

            if (data.size() == 0)
            {
                return Response.status(Response.Status.NO_CONTENT).entity("{\"error\": \"No data received\"}").build();
            }

            List<StepGroupDef> stepGroupDefList = StepGroupDAO.updateStepGroupsTransaction(em, data);

            return Response.ok().entity(stepGroupDefList).build();
        }
        catch (WebApplicationException e)
        {
            LOGGER.error("Web application Exception in updating procedure step groups", e);
        }
        catch (Exception e)
        {
            LOGGER.error("Problem saving procedure step groups", e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return Response.status(Response.Status.OK).entity("{\"error\": \"Error updating step groups, " +
                "see logs for details\"}").build();
    }

    @DELETE
    @Path("{id}")
    @Consumes({MediaType.TEXT_PLAIN})
    public Response deleteStepGroup(@PathParam("id") int id)
    {
        EntityManager em = JPAUtils.getEntityManager();
        try
        {
            boolean success = StepGroupDAO.deleteStepGroupTransaction(em, id);
            return Response.status(Response.Status.OK).entity("{\"success\": " + success + "}").build();
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
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return Response.status(Response.Status.OK).entity("{\"error\": \"Failed to delete step group\"}").build();
    }

    /**
     * This method allows the user to pass in a json patch generated on the front end for an array of step groups and
     * apply that patch to that array to facilitate bulk add/update/deletions on that array. The patch is applied via
     * the Patchutils class, and then the resulting patched array is filtered for only those groups that have been updated
     * or deleted. Note in the future we may want to expand to use for adding groups in bulk as well, but that is outside
     * the scope of what's needed in the application at the moment. The groups to be updated and deleted are passed to the
     * DAO for database operations, then the patched array is returned to the front end.
     * 
     * @param parentPk
     * @param isTopLevel
     * @param operationsJson - this is the patch generated by the client
     * @return
     */
    @PATCH
    @Consumes({MediaType.APPLICATION_JSON_PATCH_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response applyPatchUpdatesToStepGroups(@QueryParam("parentPk") int parentPk, @QueryParam("isTopLevel") boolean isTopLevel,
            InputStream operationsJson)
    {
        Response response;
        EntityManager em = JPAUtils.getEntityManager();

        // Json reader reads the input stream of the patch and creates a JsonPatch object
        JsonReader jsonReader = Json.createReader(operationsJson);
        JsonPatch patch = Json.createPatch(jsonReader.readArray());
        jsonReader.close();

        try
        {
            ProcedureDetails pd = null;
            SortedSet<StepGroupDef> stepGroupsToUpdate;

            // get the array of step groups to be patched.
            if (isTopLevel)
            {
                pd = JPAUtils.getRecordById(em, ProcedureDetails.class, parentPk);
                stepGroupsToUpdate = pd.getStepGroupDefs();
            }
            else
            {
                StepGroupDef parentGroup = JPAUtils.getRecordById(em, StepGroupDef.class, parentPk);
                stepGroupsToUpdate = parentGroup.getStepGroupDefsChildren();
            }

            // apply the patch to the array via the patch utils class
            List<StepGroupDef> allGroups = PatchUtils.updateArrayUsingJsonPatch(patch, stepGroupsToUpdate, StepGroupDef.class);
            SortedSet<StepGroupDef> finalGroupsToReturn = new TreeSet<>();
            List<StepGroupDef> updateTheseGroups = new ArrayList<>();

            // separate out the groups that have been updated vs those that haven't; we do not need to send the unchanged
            // groups to the DAO for unnecessary updating.
            allGroups.stream().forEach(group ->
            {
                boolean noMatch = true;
                for (StepGroupDef sg : stepGroupsToUpdate)
                {
                    if (group.equals(sg))
                    {
                        noMatch = false;
                        break;
                    }
                }
                if (noMatch)
                {
                    updateTheseGroups.add(group);
                }
                else
                {
                    finalGroupsToReturn.add(group);
                }
            });

            // if the original array has groups that are not in the patched array, those groups should be deleted.
            List<StepGroupDef> deleteTheseGroups = stepGroupsToUpdate.stream().filter(g ->
            {
                for (StepGroupDef sg : allGroups)
                {
                    if (g.getPk().equals(sg.getPk()))
                        return false;
                }
                return true;
            }).collect(Collectors.toList());
            Integer pdPk = pd != null ? pd.getPk() : null;

            // TODO: Can add functionality here to check if the patched array has groups that are not in the original
            // array - if so, these are groups to be added to the database.

            finalGroupsToReturn.addAll(StepGroupDAO.bulkAddUpdateDeleteTransaction(em, new ArrayList<StepGroupDef>(), updateTheseGroups, deleteTheseGroups, pdPk));
            response = Response.ok().entity(finalGroupsToReturn).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Problem updating step groups", e);
            response = gem.toResponse(e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return response;
    }
}
