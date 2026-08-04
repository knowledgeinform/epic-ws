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

import edu.jhuapl.sd.sig.epic.data.ProgramDAO;
import edu.jhuapl.sd.sig.epic.data.UsersDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.Program;
import edu.jhuapl.sd.sig.epic.model.ProgramRole;
import edu.jhuapl.sd.sig.epic.model.Users;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.Set;

@Secured
@Path("/programs")
public class Programs
{

    @Context
    SecurityContext sc;

    private static final Logger LOGGER = LogManager.getLogger();

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public List<Program> getAllPrograms()
    {
        EntityManager em = null;
        List<Program> allPrograms;
        try
        {
            em = JPAUtils.getEntityManager();
            allPrograms = JPAUtils.getAllRecordsForTable(em, Program.class);

            return allPrograms;
        }
        catch (Exception e)
        {
            LOGGER.log(Level.ERROR, "Unable to query for programs", e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return null;
    }

    @POST
    @Path("/create")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Program createProgram(Program prog)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            String username = sc.getUserPrincipal().getName();
            Users user = UsersDAO.getUserByUsername(em, username);

            if (user.getIsAdmin())
            {
                ProgramDAO.createProgram(em, prog);
                em.close();
                return prog;
            }
            else
            {
                em.close();
                String msg = "Problem creating program: user is not an Admin. \nUser: " + user + "\nProgram: " + prog;
                LOGGER.error(msg);
                throw new WebApplicationException(msg);
            }
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }

    }

    @POST
    @Path("/{programPk}/updateRequiredRoles")
    public void updateRequiredRoles(
            @PathParam("programPk") Integer programPk,
            Set<ProgramRole> requiredProgramRoles)
    {
        JPAUtils.basicTransaction(em ->
        {
            Program program = JPAUtils.getRecordById(em, Program.class, programPk);
            program.setRequiredProgramRoles(requiredProgramRoles);
            em.merge(program);
        }, "Could not update program required roles.");
    }
}
