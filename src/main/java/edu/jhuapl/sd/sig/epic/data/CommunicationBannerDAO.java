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

import edu.jhuapl.sd.sig.epic.model.CommunicationBanner;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

public class CommunicationBannerDAO
{

    public static void postCommunicationBanner(EntityManager em, CommunicationBanner communicationBanner)
    {
        // since we are allowing only one banner at a time, remove anything that's currently in communication banner table first
        removeCommunicationBanner(em);

        // commit new banner to database
        em.getTransaction().begin();
        em.persist(communicationBanner);
        em.getTransaction().commit();
    }

    public static void removeCommunicationBanner(EntityManager em)
    {
        CommunicationBanner communicationBanner = getCommunicationBanner(em);
        if (communicationBanner != null)
        {
            em.getTransaction().begin();
            em.remove(communicationBanner);
            em.getTransaction().commit();
        }
    }

    public static CommunicationBanner getCommunicationBanner(EntityManager em)
    {
        String queryString = "SELECT cb FROM CommunicationBanner cb";
        TypedQuery<CommunicationBanner> query = em.createQuery(queryString, CommunicationBanner.class);

        try
        {
            return query.getSingleResult();
        }
        catch (NoResultException e)
        {
            return null;
        }
    }
}
