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
import edu.jhuapl.sd.sig.epic.resource.model.util.PACommentReply;
import edu.jhuapl.sd.sig.epic.resource.util.GenericExceptionMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Date;
import java.util.List;

@Secured
@Path("/Comments")
public class Comments
{

    @Context
    SecurityContext sc;

    private static final Logger LOGGER = LogManager.getLogger();
    private GenericExceptionMapper gem = new GenericExceptionMapper();

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/Approval")
    public Response saveProcedureApprovalComment(PACommentReply pacr)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            String username = sc.getUserPrincipal().getName();
            Users user = UsersDAO.getUserByUsername(em, username);
            if (pacr.isReply())
            {
                ApprovalCommentReply acr = new ApprovalCommentReply();
                acr.setApprovalComment(JPAUtils.getRecordById(em, ApprovalComment.class, pacr.getParentPk()));
                acr.setCommentText(pacr.getComment());
                acr.setCommentTimestamp(new Date());
                acr.setUsers(user);
                acr.setCommentType(CommentType.APPROVAL_COMMENT_REPLY);

                acr = ProcedureApproverDAO.saveApprovalCommentReply(em, acr);
                return Response.status(Response.Status.CREATED).entity(acr).build();
            }
            else
            {
                ApprovalComment ac = new ApprovalComment();
                ac.setCommentText(pacr.getComment());
                ac.setProcedureApproval(JPAUtils.getRecordById(em, ProcedureApproval.class, pacr.getParentPk()));
                ac.setCommentTimestamp(new Date());
                ac.setUsers(user);
                ac.setCommentType(CommentType.APPROVAL_COMMENT);

                ac = ProcedureApproverDAO.saveApprovalComment(em, ac);
                return Response.status(Response.Status.CREATED).entity(ac).build();
            }

        }
        catch (Exception e)
        {
            LOGGER.error("Problem saving comment", e);
            throw new WebApplicationException("Unable to comment", e);

        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/RunStep")
    public Response saveRunStepComment(@QueryParam("stepPk") String stepPk, RunStepComment runStepComment)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            String username = sc.getUserPrincipal().getName();
            Users user = UsersDAO.getUserByUsername(em, username);

            runStepComment.setStepDef(em.find(StepDef.class, Integer.parseInt(stepPk)));
            runStepComment.setCommentTimestamp(new Date());
            runStepComment.setUsers(user);

            runStepComment = StepDAO.saveRunStepComment(em, runStepComment);

            return Response.status(Response.Status.CREATED).entity(runStepComment).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Problem saving run value comment", e);
            throw new WebApplicationException("Unable to save run value comment", e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/BlackLine")
    public Response saveBlackLineComment(List<BlackLineComment> blackLineComments, @QueryParam("entityType") String type,
            @QueryParam("isStepManualValidation") boolean stepManualValidation)
    {
        Response response;
        EntityManager em = null;

        for (BlackLineComment blackLineComment : blackLineComments)
        {
            if (blackLineComment.getProcedureChangeType() == null)
            {
                JPAUtils.logWebAppException("Could not save blackLineComment: no procedure change type.");
            }
            else if (blackLineComment.getCommentText() == null)
            {
                JPAUtils.logWebAppException("Could not save blackLineComment: no comment text.");
            }
            else if (blackLineComment.getCommentType() == null)
            {
                JPAUtils.logWebAppException("Could not save blackLineComment: no comment type.");
            }
        }

        try
        {
            em = JPAUtils.getEntityManager();

            String username = sc.getUserPrincipal().getName();
            Users user = UsersDAO.getUserByUsername(em, username);

            for (int i = 0; i < blackLineComments.size(); i++)
            {
                BlackLineComment blackLineComment = blackLineComments.get(i);
                // these cases should match the BlackLineEntityType enum on the front end (under app/interfaces/black-line.dto.ts)
                switch (type.toUpperCase())
                {
                    case "STEP":
                        blackLineComment.setStepDef(em.find(StepDef.class, blackLineComment.getStepDef().getPk()));
                        break;
                    case "STEP_GROUP":
                        blackLineComment.setStepGroupDef(em.find(StepGroupDef.class, blackLineComment.getStepGroupDef().getPk()));
                        break;
                    case "PROCEDURE_DETAILS":
                        blackLineComment.setProcedureDetails(em.find(ProcedureDetails.class, blackLineComment.getProcedureDetails().getPk()));
                        break;
                    case "PROCEDURE_INSTRUCTION":
                        blackLineComment.setProcedureInstruction(em.find(ProcedureInstruction.class, blackLineComment.getProcedureInstruction().getPk()));
                        break;
                    default:
                        LOGGER.error("Invalid entity type for black line comment");
                        throw new WebApplicationException("Invalid entity type for black line comment, unable to save black line");
                }
                blackLineComment.setCommentTimestamp(new Date());
                blackLineComment.setUsers(user);

                blackLineComment = BlackLineDAO.saveBlackLine(em, blackLineComment, stepManualValidation);
                blackLineComments.set(i, blackLineComment);
            }

            return Response.status(Response.Status.CREATED).entity(blackLineComments).build();
        }
        catch (Exception e)
        {
            if (blackLineComments.size() > 1)
            {
                LOGGER.error("Problem saving black lines", e);
                response = gem.toResponse(e);
            }
            else
            {
                LOGGER.error("Problem saving black line", e);
                response = gem.toResponse(e);
            }
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return response;
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/RunCloseout")
    public Response saveRunCloseoutComment(RunCloseoutComment runCloseoutComment)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            String username = sc.getUserPrincipal().getName();
            Users user = UsersDAO.getUserByUsername(em, username);

            runCloseoutComment = RunDAO.saveRunCloseoutComment(em, runCloseoutComment, user);

            return Response.ok().entity(runCloseoutComment).build();
        }
        catch (Exception e)
        {
            String msg = "Failed to save run closeout comment";
            LOGGER.error(msg, e);
            return Response.accepted().entity("{\"error\": \"" + msg + "\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/RunCloseoutReply")
    public Response saveRunCloseoutCommentReply(RunCloseoutCommentReply runCloseoutCommentReply)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            String username = sc.getUserPrincipal().getName();
            Users user = UsersDAO.getUserByUsername(em, username);

            runCloseoutCommentReply = RunDAO.saveRunCloseoutCommentReply(em, runCloseoutCommentReply, user);

            return Response.ok().entity(runCloseoutCommentReply).build();
        }
        catch (Exception e)
        {
            String msg = "Failed to save run closeout comment";
            LOGGER.error(msg, e);
            return Response.accepted().entity("{\"error\": \"" + msg + "\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/RunCloseoutStickyComment")
    public Response saveRunCloseoutStickyComment(RunCloseoutStickyComment runCloseoutStickyComment)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            String username = sc.getUserPrincipal().getName();
            Users user = UsersDAO.getUserByUsername(em, username);

            runCloseoutStickyComment = RunDAO.saveRunCloseoutStickyComment(em, runCloseoutStickyComment, user);

            return Response.ok().entity(runCloseoutStickyComment).build();
        }
        catch (Exception e)
        {
            String msg = "Failed to save run closeout sticky comment";
            LOGGER.error(msg, e);
            return Response.accepted().entity("{\"error\": \"" + msg + "\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/RunCloseoutStickyComment")
    public Response markRunCloseoutStickyCommentComplete(RunCloseoutStickyComment runCloseoutStickyComment)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            return Response.ok().entity(RunDAO.markRunCloseoutStickyCommentComplete(em, runCloseoutStickyComment)).build();
        }
        catch (Exception e)
        {
            String msg = "Failed to save run closeout sticky comment";
            LOGGER.error(msg, e);
            return Response.accepted().entity("{\"error\": \"" + msg + "\"}").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }
}
