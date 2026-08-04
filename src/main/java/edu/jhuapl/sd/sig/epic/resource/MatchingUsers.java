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
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Secured
@Path("/MatchingUsers")
public class MatchingUsers
{
    private static final Logger LOGGER = LogManager.getLogger();

    @GET
    @Consumes({MediaType.APPLICATION_JSON})
    //	@Produces({MediaType.APPLICATION_JSON})
    public List<Users> getMatchingUsers(@QueryParam("searchTerm") String searchTerm)
    {

        EntityManager em = null;

        List<Users> users = null;
        try
        {
            em = JPAUtils.getEntityManager();
            users = UsersDAO.getMatchingUsers(em, searchTerm);
        }
        catch (Exception e)
        {
            // add logger stuff here
            LOGGER.log(Level.ERROR, "Unable to query for users matching [" + searchTerm + "]", e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }

        return users;
    }
}
