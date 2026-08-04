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

import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.StepTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/CreateNewStepTable")
public class CreateNewStepTable
{
    private static final Logger LOGGER = LogManager.getLogger();

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public StepTable getNewStepTable(@QueryParam("stepGroupDefId") Integer step_def_group_id, @QueryParam("displayOrder") Double displayOrder)
    {
        EntityManager em = null;

        StepTable st = null;
        try
        {
            em = JPAUtils.getEntityManager();
            //			st = StepDAO.createNewStepTable(em, step_def_group_id, displayOrder);
            return st;
        }
        catch (Exception e)
        {
            LOGGER.error("Problem creating a new step table", e);

        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return null;
    }
}
