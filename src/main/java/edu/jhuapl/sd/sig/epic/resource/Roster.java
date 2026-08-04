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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.jhuapl.sd.sig.epic.data.RosterDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.Program;
import edu.jhuapl.sd.sig.epic.model.ProgramRole;
import edu.jhuapl.sd.sig.epic.model.RosterEntry;
import edu.jhuapl.sd.sig.epic.model.UserRolesPair;
import edu.jhuapl.sd.sig.epic.model.Users;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;

@Secured
@Path("/Roster/{programPk}")
public class Roster
{

    private static final Logger LOGGER = LogManager.getLogger(Runs.class.getName());

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    public List<UserRolesPair> addMember(
            @PathParam("programPk") int programPk,
            @QueryParam("userId") int userId,
            String role)
    {
        EntityManager em = JPAUtils.getEntityManager();

        try
        {

            TypedQuery<ProgramRole> prQuery = em.createQuery("SELECT pr FROM ProgramRole pr WHERE pr.name = :role AND pr.program.pk = :programPk", ProgramRole.class);
            prQuery.setParameter("role", role);
            prQuery.setParameter("programPk", programPk);
            ProgramRole pr = prQuery.getSingleResult();

            RosterEntry re = new RosterEntry(
                    JPAUtils.getRecordById(em, Users.class, userId),
                    JPAUtils.getRecordById(em, Program.class, programPk),
                    pr);

            RosterDAO.addMemberToRoster(em, re);

            return RosterDAO.getRosterForProgram(em, programPk);

        }
        catch (Exception e)
        {
            String msg = "Could not add member to roster.";
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @DELETE
    @Produces
    public List<UserRolesPair> removeRole(
            @PathParam("programPk") int programPk,
            @QueryParam("userId") int userId,
            @QueryParam("role") String role)
    {
        EntityManager em = JPAUtils.getEntityManager();
        try
        {
            RosterDAO.removeRoleFromRoster(em, programPk, userId, role);
            return RosterDAO.getRosterForProgram(em, programPk);
        }
        catch (Exception e)
        {
            String msg = "Could not remove role from roster.";
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @DELETE
    @Path("removeMember/{userId}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<UserRolesPair> removeMember(
            @PathParam("programPk") int programPk,
            @PathParam("userId") int userId)
    {
        EntityManager em = JPAUtils.getEntityManager();
        try
        {
            RosterDAO.removeMemberFromRoster(em, programPk, userId);
            return RosterDAO.getRosterForProgram(em, programPk);
        }
        catch (Exception e)
        {
            String msg = "Could not remove member from roster.";
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public List<UserRolesPair> getRosterForProgram(
            @PathParam("programPk") int programPk)
    {
        EntityManager em = JPAUtils.getEntityManager();
        try
        {
            return RosterDAO.getRosterForProgram(em, programPk);
        }
        catch (Exception e)
        {
            String msg = "Could not get roster.";
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

}
