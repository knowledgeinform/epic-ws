/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.data;

import javax.persistence.EntityManager;
import javax.ws.rs.WebApplicationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.TestingPhase;

public class TestingPhaseDAO extends JPAUtils
{

    private static final Logger LOGGER = LogManager.getLogger();

    public static TestingPhase createTestingPhase(EntityManager em, TestingPhase tp)
    {
        try
        {
            em.getTransaction().begin();
            em.persist(tp);
            em.getTransaction().commit();
            return tp;
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            String msg = "Problem while saving new Testing Phase " + tp;
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
    }

}
