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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import edu.jhuapl.sd.sig.epic.data.ProgramRolesDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.ProcedureChangeType;
import edu.jhuapl.sd.sig.epic.model.Program;
import edu.jhuapl.sd.sig.epic.model.ProgramRole;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import edu.jhuapl.sd.sig.epic.resource.model.ProcedureChangeTypeSelection;

@Secured
@Path("/ProcedureChangeTypes")
public class ProcedureChangeTypes
{

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public static List<ProcedureChangeType> getTypes(@QueryParam("programPk") Integer programPk)
    {
        List<ProcedureChangeType> list = new ArrayList<>();
        JPAUtils.basicTryCatch((em) ->
        {
            list.addAll(ProgramRolesDAO.getProcedureChangeTypes(em, programPk));
        }, "Could not get change types.");
        return list;
    }

    /**
     * A lightweight endpoint to return the change types for a selected program.
     * 
     * @param programPk program identifier
     * @return request containing the selected change types and required role approvals
     */
    @GET
    @Path("/ChangeTypes/{programPk}")
    @Produces({MediaType.APPLICATION_JSON})
    public static List<ProcedureChangeTypeSelection> getTypesForSelection(@PathParam("programPk") Integer programPk)
    {
        List<ProcedureChangeTypeSelection> list = new ArrayList<>();
        JPAUtils.basicTryCatch((em) ->
        {
            list.addAll(ProgramRolesDAO.getProcedureChangeTypesForSelection(em, programPk));
        }, "Could not get change types for program selection.");
        return list;
    }

    @POST
    public static ProcedureChangeType upsertType(ProcedureChangeType pct)
    {
        List<ProcedureChangeType> result = new ArrayList<>();
        JPAUtils.basicTransaction((em) ->
        {
            if (pct.getProgramPk() != null)
            {
                Program program = JPAUtils.getRecordById(em, Program.class, pct.getProgramPk());
                if (program != null)
                {
                    pct.setProgram(program);
                }
                else
                {
                    throw new WebApplicationException("Could not find program with pk: " + pct.getProgramPk());
                }

            }

            if (pct.getPk() == null)
            {
                em.persist(pct);
                result.add(pct);
            }
            else
            {
                result.add(em.merge(pct));
            }
        }, "Could not create/update change type.");
        return result.get(0);
    }

    @DELETE
    public static void deleteType(ProcedureChangeType pct)
    {
        JPAUtils.basicTransaction((em) ->
        {
            ProcedureChangeType apct = JPAUtils.getRecordById(em, ProcedureChangeType.class, pct.getPk());
            em.remove(apct);
        }, "Could not remove change type.");
    }

    @POST
    @Path("/{changeTypePk}")
    public static void updateRequiredRoleApprovals(
            @PathParam("changeTypePk") Integer changeTypePk,
            Set<ProgramRole> requiredRoleApprovals)
    {
        JPAUtils.basicTransaction((em) ->
        {
            ProcedureChangeType pct = JPAUtils.getRecordById(em, ProcedureChangeType.class, changeTypePk);
            pct.setRequiredRoleApprovals(requiredRoleApprovals);
            em.merge(pct);
        }, "Could not update required role approvals.");
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    public static void updateProcedureChangeType(ProcedureChangeType procedureChangeType)
    {
        JPAUtils.basicTransaction((em) -> em.merge(procedureChangeType),
                "Could not update the procedure change type with the admin's edits.");
    }
}
