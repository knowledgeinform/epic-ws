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

import edu.jhuapl.sd.sig.epic.data.UsersDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.Users;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.security.SecureRandom;
import java.util.List;

@Secured
@Path("/AllUsers")
public class AllUsers
{

    @Context
    SecurityContext sc;

    private static final Logger LOGGER = LogManager.getLogger();

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public List<Users> getAllUsers()
    {

        EntityManager em = null;

        List<Users> users = null;
        try
        {
            em = JPAUtils.getEntityManager();
            users = UsersDAO.getAllUsers(em);
        }
        catch (Exception e)
        {
            // add logger stuff here
            LOGGER.log(Level.ERROR, "Unable to query for all users", e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }

        return users;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.TEXT_PLAIN})
    @Path("{userName}")
    public Response getUserByUserName(@PathParam("userName") String userName)
    {
        EntityManager em = null;
        Users user = null;
        try
        {
            em = JPAUtils.getEntityManager();
            user = UsersDAO.getUserByUsername(em, userName);
            return Response.status(Response.Status.OK).entity(user).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Problem finding user with username " + userName, e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to retrieve a user for the given username").build();
    }

    @PUT
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("/UpdateUserInfo")
    public Response updateUserInformation(Users userData)
    {
        EntityManager em = null;
        Users user = null;

        try
        {
            em = JPAUtils.getEntityManager();
            user = UsersDAO.updateUserInformation(em, userData);
            return Response.status(Response.Status.OK).entity(user).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Error while updating user information for user id " + userData.getUserId() + ", username " +
                    userData.getUsername(), e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to update user information for " +
                "username " + userData.getUsername()).build();
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("/GenerateNewPin")
    public Response generateNewPinForUser(Users userData)
    {
        EntityManager em = null;
        Users user = null;

        try
        {
            em = JPAUtils.getEntityManager();

            SecureRandom random = new SecureRandom();
            String newPin = String.format("%06d", random.nextInt(1000000));
            userData = UsersDAO.getUserByUsername(em, userData.getUsername());
            userData.setPin(newPin);
            user = UsersDAO.updateUserInformation(em, userData);
            return Response.status(Response.Status.OK).entity(user).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Error while generating new pin for user id " + userData.getUserId() + ", username " +
                    userData.getUsername(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to generate new pin for " +
                    "username " + userData.getUsername()).build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }

    }

    @POST
    @Path("/create")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Users createUser(Users user)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            String username = sc.getUserPrincipal().getName();
            Users creatingUser = UsersDAO.getUserByUsername(em, username);

            if (creatingUser.getIsAdmin())
            {
                UsersDAO.addUser(em, user);
                return user;
            }
            else
            {
                String msg = "Problem creating user: request not submitted by an Admin. \nRequesting user: " + creatingUser + "\n New user: " + user;
                LOGGER.error(msg);
                throw new WebApplicationException(msg);
            }
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

}
