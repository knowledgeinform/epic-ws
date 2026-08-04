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

import edu.jhuapl.sd.sig.epic.data.ProcedureDetailsDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.ProcedureDetails;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;

import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Secured
@Path("/AuthorProcedure")
public class AuthorProcedure
{
    private static final Logger LOGGER = LogManager.getLogger();

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public ProcedureDetails getProcedureDetailsByUniqueCode(@QueryParam("id") String procedureId)
    {
        ProcedureDetails pdv = null;
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            pdv = ProcedureDetailsDAO.getProcedureDetailsByUniqueCode(em, procedureId);
            Hibernate.initialize(pdv.getStepGroupDefs());
            Hibernate.initialize(pdv.getProcedureInstructions());
            Hibernate.initialize(pdv.getProcedureApprovals());
            Hibernate.initialize(pdv.getHistories());
        }
        catch (Exception e)
        {
            LOGGER.error("Unable to retrieve procedure def version by unique code: ", e);

        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }

        return pdv;

    }
}
