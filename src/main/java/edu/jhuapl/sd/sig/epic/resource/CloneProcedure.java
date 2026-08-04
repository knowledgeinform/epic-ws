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
import edu.jhuapl.sd.sig.epic.data.UsersDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.ProcedureDef;
import edu.jhuapl.sd.sig.epic.model.Users;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import edu.jhuapl.sd.sig.epic.resource.model.ClonedProcedureData;
import edu.jhuapl.sd.sig.epic.resource.model.NewProcData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

@Secured
@Path("/CloneProcedure")
public class CloneProcedure
{

    @Context
    SecurityContext sc;

    private static final Logger LOGGER = LogManager.getLogger();

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public ProcedureDef cloneNewProcedure(ClonedProcedureData cpd)
    {
        EntityManager em = null;

        ProcedureDef pd;
        try
        {
            em = JPAUtils.getEntityManager();

            String username = sc.getUserPrincipal().getName();
            Users user = UsersDAO.getUserByUsername(em, username);

            NewProcData npd = cpd.getProcedureDef();
            // create the new procedure and return it
            if (!ProcedureDAO.isProcedureDefNameUnique(em, npd.getName()))
            {
                throw new WebApplicationException("Name must be unique");
            }
            else
            {
                pd = ProcedureDAO.cloneProcedure(em, npd, cpd.getProcedureDetailsPk(), user);
                return pd;
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Unable to clone procedure", e);
            throw e;
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }
}
