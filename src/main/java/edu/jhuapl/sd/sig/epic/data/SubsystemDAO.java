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

import edu.jhuapl.sd.sig.epic.model.Subsystem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.WebApplicationException;

public class SubsystemDAO
{

    private static final Logger LOGGER = LogManager.getLogger();

    public static Subsystem getSubsystem(EntityManager em, Integer subsystem_id)
    {
        Subsystem subsystem = null;
        String qString = "SELECT s FROM Subsystem s WHERE s.pk = :subsystem_id";
        TypedQuery<Subsystem> q = em.createQuery(qString, Subsystem.class);
        q.setParameter("subsystem_id", subsystem_id);
        subsystem = q.getSingleResult();

        return subsystem;
    }

    public static Subsystem createSubsystem(EntityManager em, Subsystem subsystem)
    {
        try
        {
            em.getTransaction().begin();
            em.persist(subsystem);
            em.getTransaction().commit();
            return subsystem;
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            String msg = "Problem while saving new subsystem " + subsystem;
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
    }

}
