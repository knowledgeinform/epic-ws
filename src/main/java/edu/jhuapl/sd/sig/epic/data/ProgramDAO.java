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

import edu.jhuapl.sd.sig.epic.model.ConfigKey;
import edu.jhuapl.sd.sig.epic.model.ProcedureChangeType;
import edu.jhuapl.sd.sig.epic.model.Program;
import edu.jhuapl.sd.sig.epic.model.ProgramRole;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.WebApplicationException;
import java.util.*;

public class ProgramDAO
{

    private static final Logger LOGGER = LogManager.getLogger();

    public static Program getProgram(EntityManager em, Integer program_id)
    {
        String qString = "SELECT p FROM Program p WHERE p.pk = :program_id";
        TypedQuery<Program> q = em.createQuery(qString, Program.class);
        q.setParameter("program_id", program_id);
        Program program = q.getSingleResult();

        return program;
    }

    public static Program createProgram(EntityManager em, Program prog)
    {
        try
        {
            em.getTransaction().begin();

            // save the program
            em.persist(prog);

            // By default programs are populated with copies of the default global procedureChangeTypes and programRoles
            String pctQuery = "SELECT pct FROM ProcedureChangeType pct WHERE pct.program = NULL AND pct.name != :legacyChangeType";
            TypedQuery<ProcedureChangeType> q = em.createQuery(pctQuery, ProcedureChangeType.class);
            q.setParameter("legacyChangeType", AppConfigurationDAO.getConfigForKey(ConfigKey.LEGACY_PROCEDURE_CHANGE_TYPE_NAME));
            List<ProcedureChangeType> globalProcedureChangeTypes = q.getResultList();

            String rolesQuery = "SELECT r FROM ProgramRole r WHERE r.program = NULL";
            TypedQuery<ProgramRole> query = em.createQuery(rolesQuery, ProgramRole.class);
            List<ProgramRole> globalProgramRoles = query.getResultList();

            // for each global procedure change type, make a copy, set the copy to have this program, and save the change type
            Map<ProcedureChangeType, Set<ProgramRole>> newPctToGlobalProgramRolesMap = new HashMap<>();
            for (ProcedureChangeType pct : globalProcedureChangeTypes)
            {
                ProcedureChangeType newPct = ProcedureChangeType.builder()
                        .isEnabled(pct.getIsEnabled())
                        .description(pct.getDescription())
                        .name(pct.getName())
                        .acceptsAllSignatures(pct.getAcceptsAllSignatures())
                        .program(prog)
                        .build();
                //				newPct.setIsEnabled(pct.getIsEnabled());
                //				newPct.setDescription(pct.getDescription());
                //				newPct.setName(pct.getName());
                //				newPct.setAcceptsAllSignatures(pct.getAcceptsAllSignatures());
                //				newPct.setProgram(prog);

                em.persist(newPct);
                newPctToGlobalProgramRolesMap.put(newPct, pct.getRequiredRoleApprovals());
            }

            // for each global program role make a copy, set the copy to have this program, and save it.
            Map<Integer, ProgramRole> globalRolePkToNewRoleMap = new HashMap<>();
            for (ProgramRole role : globalProgramRoles)
            {
                ProgramRole newRole = ProgramRole.builder()
                        .name(role.getName())
                        .program(prog)
                        .build();
                //				newRole.setName(role.getName());
                //				newRole.setProgram(prog);
                em.persist(newRole);
                globalRolePkToNewRoleMap.put(role.getPk(), newRole);
                if (prog.getProgramRolesForProgram() == null)
                {
                    prog.setProgramRolesForProgram(new HashSet<>());
                }
                prog.getProgramRolesForProgram().add(newRole);
            }

            // for each new procedure change type, set its required program roles
            for (ProcedureChangeType newChangeType : newPctToGlobalProgramRolesMap.keySet())
            {
                for (ProgramRole role : newPctToGlobalProgramRolesMap.get(newChangeType))
                {
                    if (newChangeType.getRequiredRoleApprovals() == null)
                    {
                        newChangeType.setRequiredRoleApprovals(new HashSet<>());
                    }
                    newChangeType.getRequiredRoleApprovals().add(globalRolePkToNewRoleMap.get(role.getPk()));
                }
                newChangeType = em.merge(newChangeType);
                if (prog.getProcedureChangeTypes() == null)
                {
                    prog.setProcedureChangeTypes(new HashSet<>());
                }
                prog.getProcedureChangeTypes().add(newChangeType);
            }

            em.getTransaction().commit();
            return prog;
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            String msg = "Problem while saving new program " + prog;
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
    }
}
