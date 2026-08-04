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

import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.ConfigKey;
import edu.jhuapl.sd.sig.epic.model.ProcedureDetails;
import edu.jhuapl.sd.sig.epic.model.ProcedureHeader;
import edu.jhuapl.sd.sig.epic.model.Users;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.WebApplicationException;
import java.util.List;

public class UsersDAO extends JPAUtils
{
    private static final Logger LOGGER = LogManager.getLogger();

    public static List<Users> getAllUsers(EntityManager em)
    {
        List<Users> users = null;
        String qString = "SELECT u FROM Users u";
        TypedQuery<Users> q = em.createQuery(qString, Users.class);
        users = q.getResultList();
        return users;

    }

    public static Users getUserByUsername(EntityManager em, String username)
    {
        Users u = null;
        String query = "SELECT u FROM Users u WHERE u.username = :username";
        TypedQuery<Users> q = em.createQuery(query, Users.class);
        q.setParameter("username", username);
        List<Users> users = q.getResultList();
        if (users != null && users.size() > 0)
        {
            u = users.get(0);
        }
        return u;
    }

    public static List<Users> getMatchingUsers(EntityManager em, String searchTerm)
    {
        String query = "SELECT u FROM Users u WHERE u.username LIKE :query OR u.displayName LIKE :query";
        TypedQuery<Users> q = em.createQuery(query, Users.class);
        q.setParameter("query", "%" + searchTerm + "%");
        return q.getResultList();
    }

    public static Users updateUserInformation(EntityManager em, Users userData)
    {
        try
        {
            em.getTransaction().begin();
            Users user = JPAUtils.getRecordById(em, Users.class, userData.getUserId());

            // if user returns null, try searching by username.
            if (user == null)
            {
                user = getUserByUsername(em, userData.getUsername());
            }

            user.setDisplayName(userData.getDisplayName());
            user.setPin(userData.getPin());
            user.setEmail(userData.getEmail());
            user.setIsAdmin(userData.getIsAdmin());
            if (!user.getUsername().equals(userData.getUsername()))
            {
                // log that the user name is being changed.
                LOGGER.info("Changing username from " + user.getUsername() + " to " + userData.getUsername() + " for userId "
                        + user.getUserId());
                user.setUsername(userData.getUsername());
            }
            // TODO: Need to update last login? Come back to when that is implemented.

            user = em.merge(user);
            em.getTransaction().commit();
            return user;
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
                LOGGER.error("Could not update user information", e);
                throw new WebApplicationException("Could not update user information", e);
            }
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return null;
    }

    /**
     * Adds a user to the database. Automatically sets PIN to default value.
     */
    public static Users addUser(EntityManager em, Users user)
    {
        try
        {
            em.getTransaction().begin();

            if (getUserByUsername(em, user.getUsername()) != null)
            {
                throw new WebApplicationException("Username already exists.");
            }
            ;

            if (user.getIsAdmin() == null)
                user.setIsAdmin(false);

            user.setPin(AppConfigurationDAO.getConfigForKey(ConfigKey.SYSTEM_DEFAULT_PIN));

            em.persist(user);
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
                throw new WebApplicationException("Could not add user.", e);
            }
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return user;
    }

    public static Users toggleFavorite(EntityManager em, Integer procedureDetailPk, String username, Boolean isSave)
    {
        Users returnUser = null;
        try
        {
            em.getTransaction().begin();
            ProcedureDetails pd = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDetailPk);
            Users user = UsersDAO.getUserByUsername(em, username);
            if (isSave)
            {
                user.getProcedureDetails().add(pd);
            }
            else
            {
                user.getProcedureDetails().removeIf(p -> p.getPk() == pd.getPk());
            }
            returnUser = em.merge(user);
            em.getTransaction().commit();

        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
                throw new WebApplicationException("Could not toggle favorite", e);
            }
        }

        return returnUser;
    }

    public static Users changeAuthorOnProcedureHeader(EntityManager em, Integer procedureHeaderPk, Integer userId)
    {
        Users newAuthor = null;

        try
        {
            em.getTransaction().begin();
            ProcedureHeader ph = JPAUtils.getRecordById(em, ProcedureHeader.class, procedureHeaderPk);
            Users setToUser = JPAUtils.getRecordById(em, Users.class, userId);

            ph.setUser(setToUser);

            ph = em.merge(ph);
            em.getTransaction().commit();
            newAuthor = ph.getUser();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
                throw new WebApplicationException("Unable to change procedure author");
            }
        }
        return newAuthor;

    }
}
