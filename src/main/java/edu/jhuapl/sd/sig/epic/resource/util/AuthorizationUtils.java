/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.resource.util;

import edu.jhuapl.sd.sig.epic.data.UsersDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.ProcedureDetails;
import edu.jhuapl.sd.sig.epic.model.Run;
import edu.jhuapl.sd.sig.epic.model.Users;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.SecurityContext;

public class AuthorizationUtils
{

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Returns false if the user from the SecurityContext is not an owner or admin.
     */
    public static boolean isProcedureOwnerOrAdmin(SecurityContext sc, ProcedureDetails pd)
    {
        if (pd == null)
            return false;
        if (pd.getProcedureHeader() == null)
            return false;

        EntityManager em = JPAUtils.getEntityManager();
        try
        {
            Users creatingUser = UsersDAO.getUserByUsername(em, sc.getUserPrincipal().getName());
            return creatingUser.getIsAdmin() || creatingUser.getUserId() == pd.getProcedureHeader().getUser().getUserId();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    /**
     * Throws an error if the user from the SecurityContext is not an owner or admin.
     */
    public static void checkIsProcedureOwnerOrAdmin(SecurityContext sc, ProcedureDetails pd) throws WebApplicationException
    {
        if (!AuthorizationUtils.isProcedureOwnerOrAdmin(sc, pd))
        {
            String msg = "Request not submitted by owner or Admin.";
            LOGGER.error(msg);
            throw new WebApplicationException(msg);
        }
    }

    /**
     * Throws an error if the user from the SecurityContext is not an owner or admin.
     */
    public static void checkIsProcedureOwnerOrAdmin(SecurityContext sc, Integer procedureDefPk) throws WebApplicationException
    {
        EntityManager em = JPAUtils.getEntityManager();
        try
        {
            ProcedureDetails pd = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDefPk);
            AuthorizationUtils.checkIsProcedureOwnerOrAdmin(sc, pd);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    public static void checkIsRunOwnerOrAdmin(SecurityContext sc, Integer runPk) throws WebApplicationException
    {
        EntityManager em = JPAUtils.getEntityManager();
        try
        {
            Run run = JPAUtils.getRecordById(em, Run.class, runPk);

            if (run == null || run.getProcedureDetails() == null)
            {
                String msg = "Run does not exist in database for primary key of " + runPk + ".";
                LOGGER.error(msg);
                throw new WebApplicationException(msg);
            }

            Users creatingUser = UsersDAO.getUserByUsername(em, sc.getUserPrincipal().getName());
            if (!creatingUser.getIsAdmin() && (creatingUser.getUserId() != run.getUser().getUserId()))
            {
                String msg = "Request not submitted by owner or Admin.";
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
