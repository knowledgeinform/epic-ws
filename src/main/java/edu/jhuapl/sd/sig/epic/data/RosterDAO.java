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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.ProgramRole;
import edu.jhuapl.sd.sig.epic.model.RosterEntry;
import edu.jhuapl.sd.sig.epic.model.UserRolesPair;
import edu.jhuapl.sd.sig.epic.model.Users;

public class RosterDAO
{
    public static List<UserRolesPair> getRosterForProgram(EntityManager em, int programPk)
    {
        TypedQuery<RosterEntry> tq = em.createQuery("SELECT re FROM RosterEntry re WHERE re.program.pk = :programPk", RosterEntry.class);
        tq.setParameter("programPk", programPk);
        List<RosterEntry> entries = tq.getResultList();
        Map<Users, List<ProgramRole>> grouped = entries.stream()
                .collect(Collectors.groupingBy(
                        RosterEntry::getUser,
                        Collectors.mapping(RosterEntry::getRole, Collectors.toList())));
        List<UserRolesPair> list = grouped.keySet().stream()
                .map(key -> new UserRolesPair(key, grouped.get(key)))
                .collect(Collectors.toList());
        return list;
    }

    public static void removeRoleFromRoster(EntityManager em, int programPk, int userId, String role)
    {
        Query q = em.createNativeQuery("DELETE r FROM roster r JOIN program_roles pr ON role_pk=pr.pk WHERE program_pk = ?1 AND user_id = ?2 AND pr.name = ?3");
        q.setParameter(1, programPk);
        q.setParameter(2, userId);
        q.setParameter(3, role);
        JPAUtils.doInTransaction(em, () ->
        {
            q.executeUpdate();
        });
    }

    public static void removeMemberFromRoster(EntityManager em, int programPk, int userId)
    {
        Query q = em.createNativeQuery("DELETE FROM roster WHERE program_pk = ?1 AND user_id = ?2");
        q.setParameter(1, programPk);
        q.setParameter(2, userId);
        JPAUtils.doInTransaction(em, () ->
        {
            q.executeUpdate();
        });
    }

    public static void addMemberToRoster(EntityManager em, RosterEntry re)
    {
        JPAUtils.doInTransaction(em, () ->
        {
            em.persist(re);
        });
    }

}
