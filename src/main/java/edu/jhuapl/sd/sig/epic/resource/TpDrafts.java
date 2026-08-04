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
import edu.jhuapl.sd.sig.epic.model.EditType;
import edu.jhuapl.sd.sig.epic.model.Users;
import edu.jhuapl.sd.sig.epic.model.display.dto.ProcedureListDTO;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.util.List;

@Secured
@Path("/MyDrafts")
public class TpDrafts
{

    private static final Logger LOGGER = LogManager.getLogger();

    @Context
    SecurityContext sc;

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public List<ProcedureListDTO> getMyDrafts()
    {

        EntityManager em = null;

        List<ProcedureListDTO> procedures;
        try
        {
            em = JPAUtils.getEntityManager();

            String username = sc.getUserPrincipal().getName();
            Users user = UsersDAO.getUserByUsername(em, username);

            procedures = ProcedureDAO.getFavoriteProcedureList(em, user.getUserId(), EditType.ORIGINAL);
            return procedures;
        }
        catch (Exception e)
        {
            LOGGER.error("Error Getting Procedures For UserData", e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return null;
    }
}
