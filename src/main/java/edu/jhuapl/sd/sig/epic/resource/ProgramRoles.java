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

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import edu.jhuapl.sd.sig.epic.data.ProgramRolesDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.Program;
import edu.jhuapl.sd.sig.epic.model.ProgramRole;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;

@Secured
@Path("/ProgramRoles")
public class ProgramRoles
{

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public static List<ProgramRole> getRoles(@QueryParam("programPk") Integer programPk)
    {
        List<ProgramRole> prList = new ArrayList<>();
        JPAUtils.basicTryCatch((em) ->
        {
            prList.addAll(ProgramRolesDAO.getProgramRoles(em, programPk));
        }, "Could not get program roles.");
        return prList;
    }

    @POST
    public static ProgramRole upsertRole(ProgramRole pr)
    {
        List<ProgramRole> result = new ArrayList<>();
        JPAUtils.basicTransaction((em) ->
        {
            if (pr.getProgramPk() != null)
            {
                Program program = JPAUtils.getRecordById(em, Program.class, pr.getProgramPk());
                if (program != null)
                {
                    pr.setProgram(program);
                }
                else
                {
                    throw new WebApplicationException("Could not find program with pk: " + pr.getProgramPk());
                }

            }

            if (pr.getPk() == null)
            {
                em.persist(pr);
                result.add(pr);
            }
            else
            {
                result.add(em.merge(pr));
            }
        }, "Could not add/update ProgramRole.");
        return result.get(0);
    }

    @DELETE
    public static void deleteRole(ProgramRole pr)
    {
        JPAUtils.basicTransaction((em) ->
        {
            ProgramRole apr = em.find(ProgramRole.class, pr.getPk());
            em.remove(apr);
        }, "Could not remove role.");
    }

}
