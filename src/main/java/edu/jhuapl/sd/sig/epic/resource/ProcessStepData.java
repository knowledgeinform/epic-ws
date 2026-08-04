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
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.StepDef;
import edu.jhuapl.sd.sig.epic.model.StepTable;
import edu.jhuapl.sd.sig.epic.model.StepType;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.List;

@Secured
@Path("/ProcessStepData")
public class ProcessStepData
{
    @Context
    SecurityContext sc;

    private static final Logger LOGGER = LogManager.getLogger();

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/SINGLE_VALUE") // must be the same as StepType.SINGLE_VALUE
    public StepDef saveSingleValueStepData(StepDef data)
    {
        return saveStepDataForSingleValueOrCheckbox(data);
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/CHECKBOX") // must be the same as StepType.CHECKBOX
    public StepDef saveCheckboxStepData(StepDef data)
    {
        return saveStepDataForSingleValueOrCheckbox(data);
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/TABLE") // must be the same as StepType.TABLE
    public StepDef saveTableEntryStepData(StepTable data)
    {
        EntityManager em = null;
        StepTable stepDef = null;
        try
        {
            em = JPAUtils.getEntityManager();
            stepDef = StepDAO.createNewStepTable(em, data);
        }
        catch (Exception e)
        {
            LOGGER.error("Problem saving new step table entry", e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return stepDef;
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public List<StepDef> updateStepData(@QueryParam("updateTables") boolean updateTables, List<StepDef> stepDefsData)
    {
        List<StepDef> updatedSteps = new ArrayList<>();
        // FIXME: Should refactor this so that all steps are updated in the same transaction.
        for (StepDef stepDef : stepDefsData)
        {
            StepDef us = null;
            if (stepDef.getType().equals(StepType.CHECKBOX) || stepDef.getType().equals(StepType.SINGLE_VALUE))
            {
                us = updateStepDataForSingleValueOrCheckbox(stepDef);

            }
            else if (stepDef.getType().equals(StepType.TABLE))
            {
                us = updateStepDataForTableStep((StepTable) stepDef, updateTables);
            }
            if (us != null)
            {
                updatedSteps.add(us);
            }
            else
            {
                String msg = "Could not update step with primary key: " + stepDef.getPk();
                LOGGER.error(msg);
                throw new WebApplicationException(msg);
            }
        }
        return updatedSteps;
    }

    @DELETE
    @Path("{stepPk}")
    @Consumes({MediaType.TEXT_PLAIN})
    public Response deleteStep(@PathParam("stepPk") int stepPk)
    {
        try
        {
            boolean success = StepDAO.deleteStep(JPAUtils.getEntityManager(), stepPk);
            return Response.status(Response.Status.OK).entity(
                    success).build();
        }
        catch (WebApplicationException e)
        {
            LOGGER.error("Web application Exception in deleting step with pk of " + stepPk, e);
            throw e;
        }
        catch (Exception e)
        {
            LOGGER.error("Problem deleting step", e);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{error: Failed to delete step}").build();
    }

    /**
     * private helper function for saving new single value and checkbox steps
     *
     * @param data
     * @return
     */
    private StepDef saveStepDataForSingleValueOrCheckbox(StepDef data)
    {
        EntityManager em = null;
        StepDef stepDef = null;
        try
        {
            em = JPAUtils.getEntityManager();
            stepDef = StepDAO.createNewSingleValueOrCheckboxStep(em, data);
        }
        catch (Exception e)
        {
            LOGGER.error("Problem saving new step", e);
            throw e;
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return stepDef;
    }

    /**
     * helper function for updating a single value or checkbox step
     *
     * @param stepDef
     * @return
     */
    private StepDef updateStepDataForSingleValueOrCheckbox(StepDef stepDef)
    {
        EntityManager em = null;
        StepDef updatedStep = null;
        try
        {
            em = JPAUtils.getEntityManager();
            updatedStep = StepDAO.updateSingleValueOrCheckboxStep(em, stepDef);
        }
        catch (Exception e)
        {
            LOGGER.error("Problem updating step", e);
            throw e;
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return updatedStep;
    }

    /**
     * helper function to updating a table step
     *
     * @param stepTable
     * @return
     */
    private StepDef updateStepDataForTableStep(StepTable stepTable, boolean updateTables)
    {
        EntityManager em = null;
        StepDef updatedStep = null;
        try
        {
            em = JPAUtils.getEntityManager();
            updatedStep = StepDAO.updateTableStep(em, stepTable, updateTables);
        }
        catch (Exception e)
        {
            LOGGER.error("Problem updating step", e);
            throw e;
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return updatedStep;
    }
}
