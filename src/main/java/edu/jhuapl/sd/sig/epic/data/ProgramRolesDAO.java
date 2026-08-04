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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;

import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.ProcedureChangeType;
import edu.jhuapl.sd.sig.epic.model.ProgramRole;
import edu.jhuapl.sd.sig.epic.resource.model.ProgramRoleSelection;
import edu.jhuapl.sd.sig.epic.resource.model.ProcedureChangeTypeSelection;

public class ProgramRolesDAO
{
    public static List<ProgramRole> getProgramRoles(EntityManager em, Integer programPk)
    {
        String query;
        if (programPk == null)
        {
            query = "SELECT pr from ProgramRole pr WHERE pr.program = NULL";
        }
        else
        {
            query = "SELECT pr from ProgramRole pr WHERE pr.program.pk = :programPk";
        }
        TypedQuery<ProgramRole> q = em.createQuery(query, ProgramRole.class);
        if (programPk != null)
        {
            q.setParameter("programPk", programPk);
        }
        return q.getResultList();
    }

    public static List<ProcedureChangeType> getProcedureChangeTypes(EntityManager em, Integer programPk)
    {
        String query;
        if (programPk == null)
        {
            query = "SELECT pct from ProcedureChangeType pct WHERE pct.program = NULL";
        }
        else
        {
            query = "SELECT pct from ProcedureChangeType pct WHERE pct.program.pk = :programPk";
        }
        TypedQuery<ProcedureChangeType> q = em.createQuery(query, ProcedureChangeType.class);
        if (programPk != null)
        {
            q.setParameter("programPk", programPk);
        }
        List<ProcedureChangeType> results = q.getResultList();
        if (programPk != null)
        {
            for (ProcedureChangeType pct : results)
            {
                pct.setProgramPk(pct.getProgram().getPk());
            }
        }
        return results;
    }

    /**
     * Get change types based on a selected program id. This is a lightweight method that does not do
     * all the joins that getProcedureChangeTypes performs.
     * 
     * @param em
     * @param programPk
     * @return
     */
    public static List<ProcedureChangeTypeSelection> getProcedureChangeTypesForSelection(EntityManager em, Integer programPk)
    {
        String whereClause = (programPk == null) ? "pct.program IS NULL" : "pct.program.pk = :programPk";

        TypedQuery<ProcedureChangeTypeSelection> changeTypeQuery = em.createQuery(
                "SELECT new edu.jhuapl.sd.sig.epic.resource.model.ProcedureChangeTypeSelection(" +
                        "pct.pk, pct.name, pct.isEnabled, pct.acceptsAllSignatures, pct.description, pct.program.pk, " +
                        "CASE WHEN (" +
                        "EXISTS (SELECT bl.pk FROM BlackLineComment bl WHERE bl.procedureChangeType = pct) OR " +
                        "EXISTS (SELECT rl.pk FROM RedLineComment rl WHERE rl.procedureChangeType = pct)" +
                        ") THEN false ELSE true END" +
                        ") " +
                        "FROM ProcedureChangeType pct " +
                        "WHERE " + whereClause,
                ProcedureChangeTypeSelection.class);
        if (programPk != null)
        {
            changeTypeQuery.setParameter("programPk", programPk);
        }

        Map<Integer, ProcedureChangeTypeSelection> byPk = new LinkedHashMap<>();
        for (ProcedureChangeTypeSelection changeType : changeTypeQuery.getResultList())
        {
            byPk.put(changeType.getPk(), changeType);
        }

        TypedQuery<Tuple> roleQuery = em.createQuery(
                "SELECT pct.pk AS changeTypePk, " +
                        "pr.pk AS rolePk, " +
                        "pr.name AS roleName, " +
                        "pr.bypassValidation AS bypassValidation, " +
                        "pr.program.pk AS roleProgramPk, " +
                        "CASE WHEN EXISTS (SELECT sig.pk FROM BlackRedLineSignature sig WHERE sig.programRole = pr) " +
                        "THEN false ELSE true END AS roleDeletable " +
                        "FROM ProcedureChangeType pct " +
                        "LEFT JOIN pct.requiredRoleApprovals pr " +
                        "WHERE " + whereClause,
                Tuple.class);
        if (programPk != null)
        {
            roleQuery.setParameter("programPk", programPk);
        }

        for (Tuple row : roleQuery.getResultList())
        {
            Integer pctPk = row.get("changeTypePk", Integer.class);
            Integer rolePk = row.get("rolePk", Integer.class);
            if (rolePk == null)
            {
                continue;
            }
            ProcedureChangeTypeSelection changeType = byPk.get(pctPk);
            if (changeType == null)
            {
                continue;
            }
            ProgramRoleSelection role = new ProgramRoleSelection();
            role.setPk(rolePk);
            role.setName(row.get("roleName", String.class));
            role.setBypassValidation(row.get("bypassValidation", Boolean.class));
            role.setProgramPk(row.get("roleProgramPk", Integer.class));
            role.setDeletable(row.get("roleDeletable", Boolean.class));
            changeType.getRequiredRoleApprovals().add(role);
        }

        return new ArrayList<>(byPk.values());
    }

    public static ProcedureChangeType addChangeType(EntityManager em, String name)
    {
        ProcedureChangeType pct = new ProcedureChangeType();
        pct.setName(name);
        pct.setAcceptsAllSignatures(false);
        pct.setIsEnabled(true);

        JPAUtils.doInTransaction(em, () ->
        {
            em.persist(pct);
        });
        return pct;
    }

    public static void removeChangeType(EntityManager em, ProcedureChangeType changeType)
    {
        JPAUtils.doInTransaction(em, () ->
        {
            em.remove(changeType);
        });
    }

    public static ProgramRole addProgramRole(EntityManager em, String name)
    {
        ProgramRole pr = new ProgramRole();
        pr.setName(name);

        JPAUtils.doInTransaction(em, () ->
        {
            em.persist(pr);
        });

        return pr;
    }

    public static void removeProgramRole(EntityManager em, ProgramRole pr)
    {
        JPAUtils.doInTransaction(em, () ->
        {
            em.remove(pr);
        });
    }
}
