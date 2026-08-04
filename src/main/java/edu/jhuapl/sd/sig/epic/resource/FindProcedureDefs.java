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
import edu.jhuapl.sd.sig.epic.model.display.dto.FindProcedureDTO;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Secured
@Path("/FindProcedureDefs")
public class FindProcedureDefs
{
    private static final Logger LOGGER = LogManager.getLogger();

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response findProcedureDefs(@QueryParam("searchText") String searchString,
            @QueryParam("program") String program,
            @QueryParam("subsystem") String subsystem,
            @QueryParam("editType") String editType,
            @QueryParam("procedureStatus") String procedureStatus,
            @QueryParam("phase") String testingPhase,
            @DefaultValue("0") @QueryParam("offSetResults") String offSetResults,
            @DefaultValue("20") @QueryParam("limitResults") String limitResults)
    {
        EntityManager em = null;
        FindProcedureDTO findProcedureDTO;
        try
        {
            int offSetResultsInt = Integer.parseInt(offSetResults);
            int limitResultsInt = Integer.parseInt(limitResults);
            em = JPAUtils.getEntityManager();
            findProcedureDTO = ProcedureDAO.findProceduresDefs(em, searchString, program, subsystem,
                    EditType.valueOf(editType),
                    procedureStatus, testingPhase, offSetResultsInt, limitResultsInt);

            return Response.status(Response.Status.OK).entity(findProcedureDTO).build();
        }
        catch (Exception e)
        {
            LOGGER.log(Level.ERROR, "Unable to search for procedure defs", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("error: Failed to retrieve search results").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }
}
