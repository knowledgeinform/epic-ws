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

import edu.jhuapl.sd.sig.epic.data.TestingPhaseDAO;
import edu.jhuapl.sd.sig.epic.data.UsersDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.TestingPhase;
import edu.jhuapl.sd.sig.epic.model.Users;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

@Secured
@Path("testingPhases")
public class TestingPhases
{

    @Context
    SecurityContext sc;

    private static final Logger LOGGER = LogManager.getLogger();

    @POST
    @Path("/create")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public TestingPhase create(TestingPhase tp)
    {

        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();

            String username = sc.getUserPrincipal().getName();
            Users creatingUser = UsersDAO.getUserByUsername(em, username);

            if (creatingUser.getIsAdmin())
            {
                TestingPhaseDAO.createTestingPhase(em, tp);
                em.close();
                return tp;
            }
            else
            {
                em.close();
                String msg = "Problem creating Testing Phase: request not submitted by an Admin. \nRequesting user: " + creatingUser + "\nTesting Phase: " + tp;
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
