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

import edu.jhuapl.sd.sig.epic.data.InstructionDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.EditType;
import edu.jhuapl.sd.sig.epic.model.ProcedureDetails;
import edu.jhuapl.sd.sig.epic.model.ProcedureInstruction;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Secured
@Path("/Instructions")
public class Instructions
{
    private static final Logger LOGGER = LogManager.getLogger();

    @GET
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response getInstructionSections(@QueryParam("procedureDetailsPk") Integer procedureDetailsPk)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            edu.jhuapl.sd.sig.epic.model.ProcedureDetails pd = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDetailsPk);
            if (pd != null)
            {
                Set<ProcedureInstruction> instList = pd.getProcedureInstructions();
                Hibernate.initialize(instList);
                return Response.ok().entity(instList).build();
            }
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Could not get instruction sections, " +
                "see logs for details\"}").build();
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/Copy")
    public Response copyInstructionSection(@QueryParam("instructionPk") Integer instructionPk)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            // if we are copying an instruction from this method, then the procedure is in draft mode and the edit type
            // should always be original. Also the redLineComment should always be null here.
            ProcedureInstruction pi = InstructionDAO.copyInstruction(em, instructionPk, EditType.ORIGINAL, null);
            return Response.status(Response.Status.OK).entity(pi).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to copy procedure instruction", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to copy procedure instruction").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/Clone")
    public Response cloneInstructionsToNewProcedure(@QueryParam("procedureDetailsPk") Integer procedureDetailsPk, Integer[] instPks)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            ProcedureDetails targetPd = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDetailsPk);
            SortedSet<ProcedureInstruction> sourcePIs = new TreeSet<>();
            for (Integer pk : instPks)
            {
                sourcePIs.add(JPAUtils.getRecordById(em, ProcedureInstruction.class, pk));
            }
            // if we are copying an instruction from this method, then the procedure is in draft mode and the edit type
            // should always be ORIGINAL
            // also redLineComment = null
            SortedSet<ProcedureInstruction> newPIs = InstructionDAO.copyInstructionsToProcedure(em, sourcePIs, targetPd, EditType.ORIGINAL, null);
            return Response.status(Response.Status.OK).entity(newPIs).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to copy procedure instruction", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to copy procedure instruction").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/AddSection")
    public SortedSet<ProcedureInstruction> saveInstructionInfoSectionData(ProcedureInstruction data)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            InstructionDAO.insertNewInstructionSection(em, data.getSectionName(), data.getDisplayOrder(), data.getProcedureDetails().getPk());
            return InstructionDAO.getInstructionSections(em, data.getProcedureDetails().getPk());
        }
        catch (Exception e)
        {
            LOGGER.error("Problem saving procedure instruction section", e);
            throw new WebApplicationException("Unable to save procedure instruction", e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/UpdateInstruction")
    public Response updateInstructionInfoData(List<ProcedureInstruction> data)
    {
        EntityManager em = null;

        List<ProcedureInstruction> piList = new ArrayList<>();
        try
        {
            em = JPAUtils.getEntityManager();
            for (int i = 0; i < data.size(); i++)
            {
                ProcedureInstruction pi = InstructionDAO.updateInstructionSection(em, data.get(i).getPk(), data.get(i).getSectionName(), data.get(i).getText(), data.get(i).getDisplayOrder());
                piList.add(pi);

            }
            return Response.status(Response.Status.OK).entity(piList).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to update procedure sections", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to update procedure sections").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @DELETE
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/DeleteInstruction")
    public ProcedureInstruction deleteInstructionSection(@QueryParam("sectionId") Integer sectionId)
    {
        ProcedureInstruction pi = null;
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            pi = InstructionDAO.deleteInstructionSection(em, sectionId);
        }
        catch (Exception e)
        {
            LOGGER.error("Unable to delete instruction section: ", e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }

        return pi;

    }
}
