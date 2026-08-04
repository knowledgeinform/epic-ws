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

import edu.jhuapl.sd.sig.epic.data.*;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.*;
import edu.jhuapl.sd.sig.epic.model.ProcedureDetails;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import edu.jhuapl.sd.sig.epic.resource.model.RedlineData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.*;

@Secured
@Path("/Redline")
public class Redlines
{

    @Context
    SecurityContext sc;

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * This method checks if a given run is allowed to be redlined, based on whether or not other existing runs for this
     * revision have been previously redlined or not. Only one run can be redlined at a time in current implementation.
     *
     * @param runId
     * @return
     */
    @GET
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("/Allowed")
    public Response checkIfRedliningAllowed(@QueryParam("id") String runId)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            Run run = RunDAO.getRunByUniqueCode(em, runId);
            ProcedureDetails pd = run.getProcedureDetails();
            Boolean allowed = !pd.getEditType().equals(EditType.LOCKED_RUN) &&
                    (pd.getOriginalProcedureDetails().getRedlinedVersion() == null || pd.getOriginalProcedureDetails().getRedlinedVersion().equalsIgnoreCase(runId));
            if (!allowed)
            {
                em.getTransaction().begin();
                pd.setEditType(EditType.LOCKED_RUN);
                em.merge(pd);
                em.getTransaction().commit();
            }
            return Response.status(Response.Status.OK).entity(allowed).build();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            LOGGER.error("Problem checking locked status for run with id of " + runId, e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error: Failed to find locked status for run with id " +
                "of " + runId + "\"}").build();
    }

    /**
     * This method accepts red line data for an instruction. It saves the changes to the instruction, including the change
     * in EditType, and saves the accompanying red line comment.
     * Note that the EditType should have been set on the front end.
     *
     * @param redlineData
     * @return
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/Instruction")
    public Response saveRedLineForInstruction(RedlineData redlineData)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            // first confirm that allowed to redline
            ProcedureDetails run = JPAUtils.getRecordById(em, ProcedureDetails.class, redlineData.getProcedureDetailsPk());
            Response response;
            if ((response = checkThatAllowedToRedline(em, run)) != null)
            {
                return response;
            }

            // if here, we are good to make the red line changes.
            ProcedureInstruction redlinedInstruction = redlineData.getProcedureInstruction();

            ProcedureInstruction updatedInstruction = RedlineDAO.saveSingleProcedureInstructionRedLine(em, redlinedInstruction,
                    processRedLineComment(em, redlineData), run);

            return Response.status(Response.Status.OK).entity(updatedInstruction).build();

        }
        catch (Exception e)
        {
            LOGGER.error("Problem while saving red line for instruction with pk of " + redlineData.getProcedureInstruction().getPk(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/StepGroup")
    public Response saveRedLineForStepGroup(RedlineData redlineData)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            // first confirm that allowed to redline
            ProcedureDetails run = JPAUtils.getRecordById(em, ProcedureDetails.class, redlineData.getProcedureDetailsPk());
            Response response;
            if ((response = checkThatAllowedToRedline(em, run)) != null)
            {
                return response;
            }

            // if here, we are good to make the red line changes.
            redlineData.setRedLineComment(processRedLineComment(em, redlineData));

            List<RedlineData> redlineDataList = new ArrayList<RedlineData>();
            redlineDataList.add(redlineData);
            List<StepGroupDef> updatedStepGroups = RedlineDAO.saveStepGroupRedLine(em, redlineDataList, run);

            return Response.status(Response.Status.OK).entity(updatedStepGroups.get(0)).build();

        }
        catch (Exception e)
        {
            LOGGER.error("Problem while saving red line for step group with pk of " + redlineData.getStepGroupDef().getPk(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/StepGroupArray")
    public Response saveRedLinesForStepGroupArray(List<RedlineData> redlineData)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            if (redlineData.size() == 0)
            {
                return Response.status(Response.Status.NO_CONTENT).entity("{\"error\": \"No data received\"}").build();
            }

            // first confirm that allowed to redline
            ProcedureDetails run = JPAUtils.getRecordById(em, ProcedureDetails.class, redlineData.get(0).getProcedureDetailsPk());
            Response response;
            if ((response = checkThatAllowedToRedline(em, run)) != null)
            {
                return response;
            }

            for (RedlineData redline : redlineData)
            {
                redline.setRedLineComment(processRedLineComment(em, redline));
            }

            // we are going to save the whole list as one transaction
            List<StepGroupDef> updatedStepGroups = RedlineDAO.saveStepGroupRedLine(em, redlineData, run);

            return Response.status(Response.Status.OK).entity(updatedStepGroups).build();

        }
        catch (Exception e)
        {
            LOGGER.error("Problem while saving red line for step groups", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/StepArray")
    public Response saveRedLineForStep(List<RedlineData> redlineData)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            if (redlineData.size() == 0)
            {
                return Response.status(Response.Status.NO_CONTENT).entity("{\"error\": \"No data received\"}").build();
            }

            // first confirm that allowed to redline
            ProcedureDetails run = JPAUtils.getRecordById(em, ProcedureDetails.class, redlineData.get(0).getProcedureDetailsPk());
            Response response;
            if ((response = checkThatAllowedToRedline(em, run)) != null)
            {
                return response;
            }

            for (RedlineData redline : redlineData)
            {
                redline.setRedLineComment(processRedLineComment(em, redline));
            }

            List<StepDef> updatedSteps = RedlineDAO.saveRedLinesToSteps(em, redlineData, run);

            return Response.ok().entity(updatedSteps).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Problem while saving red lines for steps", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/CopyStep")
    public Response saveRedLineForCopiedStep(RedlineData data)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            // first confirm that allowed to redline
            ProcedureDetails run = JPAUtils.getRecordById(em, ProcedureDetails.class, data.getProcedureDetailsPk());
            Response response;
            if ((response = checkThatAllowedToRedline(em, run)) != null)
            {
                return response;
            }

            // we need to mark this run as the red lined run
            run = RedlineDAO.markThisRunAsRedlined(em, run);

            // if copying a step through this endpoint, must be in redline mode, so editType should always be REDLINE_ADD
            StepDef newStep = StepDAO.copyStepToGroup(em, data.getStepDef().getPk(), data.getStepGroupDef().getPk(), EditType.REDLINE_ADD,
                    data.getProcedureDetailsPk(), processRedLineComment(em, data));

            return Response.status(Response.Status.CREATED).entity(newStep).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Problem while saving red lines for copying step with pk " + data.getStepDef().getPk(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/CopyStepGroup")
    public Response saveRedLineForCopiedStepGroup(@QueryParam("parentGroupPk") Integer parentGroupPk, RedlineData data)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            // first confirm that allowed to redline
            ProcedureDetails run = JPAUtils.getRecordById(em, ProcedureDetails.class, data.getProcedureDetailsPk());
            Response response;
            if ((response = checkThatAllowedToRedline(em, run)) != null)
            {
                return response;
            }

            // we need to mark this run as the red lined run
            run = RedlineDAO.markThisRunAsRedlined(em, run);

            StepGroupDef newGroup = StepGroupDAO.copyGroupToGroup(em, data.getStepGroupDef().getPk(), parentGroupPk,
                    data.getProcedureDetailsPk(), EditType.REDLINE_ADD, processRedLineComment(em, data));

            return Response.status(Response.Status.CREATED).entity(newGroup).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Problem while saving red lines for copying step group with pk " + data.getStepGroupDef().getPk(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/CopyMultipleSteps")
    public Response saveRedLinesWhenCloningMultipleSteps(RedlineData data)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            // first confirm that allowed to redline
            ProcedureDetails run = JPAUtils.getRecordById(em, ProcedureDetails.class, data.getProcedureDetailsPk());
            Response response;
            if ((response = checkThatAllowedToRedline(em, run)) != null)
            {
                return response;
            }

            // we need to mark this run as the red lined run
            run = RedlineDAO.markThisRunAsRedlined(em, run);

            List<StepGroupDef> newGroups = StepGroupDAO.copyStepsHierarchyToGroup(em, data.getStepDefList(), data.getStepGroupDefList(), data.getStepGroupDef().getPk(),
                    data.getProcedureDetailsPk(), EditType.REDLINE_ADD, processRedLineComment(em, data));

            return Response.status(Response.Status.CREATED).entity(newGroups).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Problem while saving red lines for copying multiple steps to step group with pk " + data.getStepGroupDef().getPk(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/HazardUpdate")
    public Response updateHazardInfoAsRedLine(@QueryParam("isHazardous") Boolean isHazardous,
            @QueryParam("hazardDescription") String hazardDescription,
            @QueryParam("isEsd0") Boolean isEsd0, RedlineData redlineData)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            // first confirm that allowed to redline
            ProcedureDetails run = JPAUtils.getRecordById(em, ProcedureDetails.class, redlineData.getProcedureDetailsPk());
            Response response;
            if ((response = checkThatAllowedToRedline(em, run)) != null)
            {
                return response;
            }

            run = RedlineDAO.updateProcedureDetailsHazardRedLine(em, run, processRedLineComment(em, redlineData), isHazardous, isEsd0, hazardDescription);
            Hibernate.initialize(run.getRedLineComments());
            Hibernate.initialize(run.getStepGroupDefs());
            Hibernate.initialize(run.getProcedureInstructions());
            Hibernate.initialize(run.getBlackLineComments());
            Hibernate.initialize(run.getOriginalProcedureDetails().getProcedureApprovals());

            return Response.ok().entity(run).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Could not save hazard information as a red line for procedure details with pk: " + redlineData.getProcedureDetailsPk(), e);
            return Response.ok().entity("{\"error\": \"Could not save hazard information as a red line, error of: " + e.getMessage() + "\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }

    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/CopyInstruction")
    public Response copyInstructionAsRedLine(RedlineData redlineData)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            ProcedureDetails run = JPAUtils.getRecordById(em, ProcedureDetails.class, redlineData.getProcedureDetailsPk());

            Response response;
            if ((response = checkThatAllowedToRedline(em, run)) != null)
            {
                return response;
            }

            // we need to mark this run as the red lined run
            run = RedlineDAO.markThisRunAsRedlined(em, run);

            ProcedureInstruction pi = InstructionDAO.copyInstruction(em, redlineData.getProcedureInstruction().getPk(), EditType.REDLINE_ADD, processRedLineComment(em, redlineData));
            return Response.status(Response.Status.OK).entity(pi).build();
        }
        catch (Exception e)
        {
            String msg = "Failed to copy procedure instruction";
            LOGGER.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"" + msg + "\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/CloneInstructions")
    public Response cloneInstructionsToNewProcedureAsRedlines(RedlineData redlineData)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            ProcedureDetails run = JPAUtils.getRecordById(em, ProcedureDetails.class, redlineData.getProcedureDetailsPk());

            Response response;
            if ((response = checkThatAllowedToRedline(em, run)) != null)
            {
                return response;
            }

            // we need to mark this run as the red lined run
            run = RedlineDAO.markThisRunAsRedlined(em, run);

            SortedSet<ProcedureInstruction> sourcePIs = new TreeSet<>();
            for (Integer sourcePk : redlineData.getInstructionPkList())
            {
                sourcePIs.add(JPAUtils.getRecordById(em, ProcedureInstruction.class, sourcePk));
            }

            SortedSet<ProcedureInstruction> newPIs = InstructionDAO.copyInstructionsToProcedure(em, sourcePIs, run, EditType.REDLINE_ADD, processRedLineComment(em, redlineData));
            return Response.status(Response.Status.OK).entity(newPIs).build();
        }
        catch (Exception e)
        {
            String msg = "Failed to copy procedure instruction";
            LOGGER.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"" + msg + "\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    private RedLineComment processRedLineComment(EntityManager em, RedlineData data)
    {
        // get the current user
        String username = sc.getUserPrincipal().getName();
        Users user = UsersDAO.getUserByUsername(em, username);
        RedLineComment comment = data.getRedLineComment();
        comment.setUsers(user);
        comment.setCommentTimestamp(new Date());
        return comment;
    }

    private Response checkThatAllowedToRedline(EntityManager em, ProcedureDetails run)
    {
        // first confirm that allowed to redline
        if (run.getEditType().equals(EditType.LOCKED_RUN))
        {
            // if here, this is a locked run and we can't make red lines
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\": \"This run is locked because red lines have already been made on run " +
                    run.getOriginalProcedureDetails().getRedlinedVersion() + "\"}").build();
        }
        return null;
    }
}
