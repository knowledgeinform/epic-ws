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
import edu.jhuapl.sd.sig.epic.model.EditType;
import edu.jhuapl.sd.sig.epic.model.History;
import edu.jhuapl.sd.sig.epic.model.ProcedureDef;
import edu.jhuapl.sd.sig.epic.model.ProcedureDetails;
import edu.jhuapl.sd.sig.epic.model.ProcedureHeader;
import edu.jhuapl.sd.sig.epic.model.ProcedureInstruction;
import edu.jhuapl.sd.sig.epic.model.ProcedureStatus;
import edu.jhuapl.sd.sig.epic.model.Program;
import edu.jhuapl.sd.sig.epic.model.Subsystem;
import edu.jhuapl.sd.sig.epic.model.Users;
import edu.jhuapl.sd.sig.epic.model.display.dto.FindProcedureDTO;
import edu.jhuapl.sd.sig.epic.model.display.dto.ProcedureListDTO;
import edu.jhuapl.sd.sig.epic.resource.model.NewProcData;
import edu.jhuapl.sd.sig.epic.resource.model.ProcedureDefAttributes;
import edu.jhuapl.sd.sig.epic.model.Status.ProcedureStatusCounts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.WebApplicationException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProcedureDAO
{
    private static final Logger LOGGER = LogManager.getLogger();

    public static List<edu.jhuapl.sd.sig.epic.model.display.dto.ProcedureListDTO> getFavoriteProcedureList(EntityManager em, Integer userid, EditType editType)
    {
        List<edu.jhuapl.sd.sig.epic.model.display.dto.ProcedureListDTO> procedures = null;
        String qString = "SELECT new edu.jhuapl.sd.sig.epic.model.display.dto.ProcedureListDTO(pdef, det) FROM ProcedureDetails det JOIN det.procedureDef pdef JOIN det.favoriteUsers u WHERE u.userId = :user_id AND det.editType = :editType";

        TypedQuery<edu.jhuapl.sd.sig.epic.model.display.dto.ProcedureListDTO> q = em.createQuery(qString, edu.jhuapl.sd.sig.epic.model.display.dto.ProcedureListDTO.class);
        q.setParameter("user_id", userid);
        q.setParameter("editType", editType);

        procedures = q.getResultList();

        return procedures;
    }

    public static boolean isProcedureDefNameUnique(EntityManager em, String name)
    {
        String q = "SELECT p FROM ProcedureDef p WHERE LOWER(p.name) = :name";
        TypedQuery<ProcedureDef> tq = em.createQuery(q, ProcedureDef.class);
        tq.setParameter("name", name.toLowerCase());
        List<ProcedureDef> pd = tq.getResultList();
        if (pd.size() == 0)
        {
            return true;
        }
        return false;
    }

    public static ProcedureDef createNewProcedure(EntityManager em, String name, String description, Integer programPk, Integer subsystemPk, Boolean esd0, Boolean hazardous, String hazardText,
            Integer userId)
    {
        ProcedureDef pd = null;
        em.getTransaction().begin();

        Program program = em.find(Program.class, programPk);
        Subsystem subsystem = em.find(Subsystem.class, subsystemPk);

        pd = new ProcedureDef();
        pd.setDescription(description);
        pd.setName(name);
        pd.setProgram(program);
        pd.setSubsystem(subsystem);

        ProcedureDetails pdv = new ProcedureDetails();
        pdv.setProcedureDef(pd);
        pdv.setProcedureDefVersion(1);
        pdv.setStatus(ProcedureStatus.DRAFT);
        pdv.setEditType(EditType.ORIGINAL);
        pdv.setEsd0(esd0);
        pdv.setHazardous(hazardous);
        pdv.setHazardDescription(hazardText);

        Users user = em.find(Users.class, userId);

        Set<Users> favoriteUsers = new HashSet<>();
        favoriteUsers.add(user);
        pdv.setFavoriteUsers(favoriteUsers);
        user.getProcedureDetails().add(pdv);

        ProcedureHeader ph = new ProcedureHeader();
        ph.setProcedureDetails(pdv);
        ph.setUser(user);
        ph.setCreationDate(new Date());

        pdv.setProcedureHeader(ph);

        HashSet<ProcedureDetails> pdvs = new HashSet<>();
        pdvs.add(pdv);

        pd.setProcedureDetails(pdvs);

        em.persist(pd);
        em.flush();

        pdv.setId(program.getCode() + "-" + subsystem.getCode() + "-" + pd.getPk() + "-" + pdv.getProcedureDefVersion());
        pdv.getHistories().add(new History(null, pdv, user));

        em.getTransaction().commit();
        return pd;
    }

    public static ProcedureDef cloneProcedure(EntityManager em, NewProcData npd, int procedureDetailsPk, Users user)
    {
        LOGGER.debug("Cloning procedure with new name {} from procedure details pk {}", npd.getName(), procedureDetailsPk);
        ProcedureDef pd = null;
        try
        {
            em.getTransaction().begin();

            Program program = JPAUtils.getRecordById(em, Program.class, npd.getProgram());
            Subsystem subsystem = JPAUtils.getRecordById(em, Subsystem.class, npd.getSubsystem());
            ProcedureDetails originalProcedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDetailsPk);

            // set the procedure def using npd
            pd = new ProcedureDef();
            pd.setDescription(npd.getDescription());
            pd.setName(npd.getName());
            pd.setProgram(program);
            pd.setSubsystem(subsystem);

            //			// now need to set pdv using procD
            LOGGER.debug("Copying procedure details.");
            ProcedureDetails pdv = ProcedureDetailsDAO.copyProcedureDetails(em, originalProcedureDetails, 1, npd.getEsd0(), npd.getHazardous(), npd.getHazardDescription(), EditType.ORIGINAL, user,
                    false);
            pdv.setProcedureDef(pd);

            HashSet<ProcedureDetails> pdvs = new HashSet<>();
            pdvs.add(pdv);

            pd.setProcedureDetails(pdvs);

            em.persist(pd);

            // Also need to persist all steps within a group
            StepGroupDAO.persistStepGroupArray(em, pdv.getStepGroupDefs());

            if (pdv.getProcedureInstructions() != null)
            {
                for (ProcedureInstruction pi : pdv.getProcedureInstructions())
                {
                    em.persist(pi);
                }
            }

            em.flush();

            pdv.setId(program.getCode() + "-" + subsystem.getCode() + "-" + pd.getPk() + "-" + pdv.getProcedureDefVersion());
            History cloneHistory = new History(null, pdv, user);
            cloneHistory.setDescription("Cloned from procedure " + originalProcedureDetails.getId()
                    + " (" + originalProcedureDetails.getProcedureDef().getName() + ")");
            pdv.getHistories().add(cloneHistory);

            LOGGER.debug("Commiting cloned procedure to database.");

            em.getTransaction().commit();

        }
        catch (Exception e)
        {
            String msg = "Error cloning procedure";
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
        return pd;
    }

    public static ProcedureDef updateProcedureDefAttributes(EntityManager em, ProcedureDefAttributes procedureDefAttributes)
    {
        ProcedureDef pd;

        try
        {
            em.getTransaction().begin();

            pd = JPAUtils.getRecordById(em, ProcedureDef.class, procedureDefAttributes.getId());
            pd.setName(procedureDefAttributes.getName() != null && !procedureDefAttributes.getName().isEmpty() ? procedureDefAttributes.getName().trim() : pd.getName());
            pd.setDescription(
                    procedureDefAttributes.getDescription() != null && !procedureDefAttributes.getDescription().isEmpty() ? procedureDefAttributes.getDescription().trim() : pd.getDescription());

            em.merge(pd);
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String errorMessage = "Error updating procedure name and/or description in the database";
            LOGGER.error(errorMessage, e);
            throw new WebApplicationException(errorMessage, e);
        }
        return pd;
    }

    public static List<ProcedureListDTO> getListOfAllProceduresForReport(EntityManager em,
            Integer programPk,
            Integer subsystemPk)
    {
        List<ProcedureListDTO> allProceduresForReport = null;
        try
        {
            String query = "SELECT new edu.jhuapl.sd.sig.epic.model.display.dto.ProcedureListDTO(pDet, pDef.name, pDef.program, pDef.subsystem, ph.creationDate, ph.user) " +
                    "from ProcedureDetails pDet JOIN pDet.procedureDef pDef JOIN pDet.procedureHeader ph " +
                    "WHERE pDet.editType = :editType";

            if (programPk != null)
            {
                query += " AND pDef.program.pk = :programPk";
            }
            if (subsystemPk != null)
            {
                query += " AND pDef.subsystem.pk = :subsystemPk";
            }
            TypedQuery<ProcedureListDTO> q = em.createQuery(query, ProcedureListDTO.class);
            q.setParameter("editType", EditType.ORIGINAL);
            if (programPk != null)
            {
                q.setParameter("programPk", programPk);
            }
            if (subsystemPk != null)
            {
                q.setParameter("subsystemPk", subsystemPk);
            }

            allProceduresForReport = q.getResultList();
            return allProceduresForReport;
        }
        catch (Exception e)
        {
            String message = "Could not retrieve list of all procedures";
            LOGGER.error(message, e);
            throw new WebApplicationException(message, e);
        }
    }

    /**
     * Find a list of procedure details that match the search criteria.
     * 
     * @param em entity manager
     * @param searchString search query string
     * @param programId program ID
     * @param subsystemId subsystem ID
     * @param editType Edit Type of the procedure details
     * @param procedureStatus Procedure status
     * @param testingPhaseId Testing phase ID
     * @return list of procedure details that match search criteria.
     */
    public static FindProcedureDTO findProceduresDefs(EntityManager em, String searchString, String programId,
            String subsystemId, EditType editType, String procedureStatus,
            String testingPhaseId, Integer offSetResults, Integer limitResults)
    {
        FindProcedureDTO findProcedureDTO = new FindProcedureDTO();
        SearchResult<ProcedureDef> result;
        try
        {
            SearchSession searchSession = Search.session(em);

            result = searchSession.search(ProcedureDef.class)
                    .where(f -> f.bool(
                            b ->
                            {
                                // Determine if searching for runs or procedures
                                if (editType != EditType.ORIGINAL)
                                {
                                    // Return only runs
                                    b.mustNot(f.match()
                                            .field("procedureDetails.editType")
                                            .matching(EditType.ORIGINAL));
                                    // Search against either procedure or run names and descriptions
                                    if (!searchString.isEmpty())
                                    {
                                        b.must(f.simpleQueryString()
                                                .fields("name", "description", "procedureDetails.id",
                                                        "procedureDetails.run.name", "procedureDetails.run.description")
                                                .matching(searchString)
                                                .defaultOperator(BooleanOperator.AND));
                                    }

                                    if (!testingPhaseId.isEmpty())
                                    {
                                        int testingIdInt = Integer.parseInt(testingPhaseId);
                                        b.must(f.match()
                                                .field("procedureDetails.run.testingPhase.pk")
                                                .matching(testingIdInt));
                                    }
                                }
                                else
                                {
                                    // Return only procedures
                                    b.must(f.match()
                                            .fields("procedureDetails.editType")
                                            .matching(editType));
                                    // Only search against procedure names and descriptions
                                    if (!searchString.isEmpty())
                                    {
                                        b.must(f.simpleQueryString()
                                                .fields("name", "description", "procedureDetails.id")
                                                .matching(searchString)
                                                .defaultOperator(BooleanOperator.AND));
                                    }
                                }
                                // Regardless of whether run or procedure, search against program and subsystem if
                                // present
                                if (!programId.isEmpty())
                                {
                                    int programIdInt = Integer.parseInt(programId);
                                    b.must(f.match()
                                            .fields("program.pk")
                                            .matching(programIdInt));
                                }
                                if (!subsystemId.isEmpty())
                                {
                                    int subsystemIdInt = Integer.parseInt(subsystemId);
                                    b.must(f.match()
                                            .fields("subsystem.pk")
                                            .matching(subsystemIdInt));
                                }
                                if (!procedureStatus.isEmpty())
                                {
                                    // Map string to ProcedureStatus object
                                    ProcedureStatus status = null;
                                    for (ProcedureStatus ps : ProcedureStatus.values())
                                    {
                                        if (procedureStatus.equals(ps.name()))
                                            status = ps;
                                    }

                                    if (status != null)
                                    {
                                        b.must(f.match()
                                                .fields("procedureDetails.status")
                                                .matching(status));
                                    }
                                    else
                                    {
                                        LOGGER.error("Search provided procedure status of " + procedureStatus + ", " +
                                                "which does not match list of possible statuses.");
                                    }
                                }
                            }))
                    .fetch(offSetResults, limitResults);

            // Initialize procedure detail runs for procedures that are ORIGINAL and READY since runs are lazy loaded
            for (ProcedureDef pDef : result.hits())
            {
                for (ProcedureDetails pd : pDef.getProcedureDetails())
                {
                    if (pd.getStatus().equals(ProcedureStatus.READY) &&
                            pd.getEditType().equals(EditType.ORIGINAL))
                    {
                        Hibernate.initialize(pd.getProcedureDetailRuns());
                    }
                }
            }

            findProcedureDTO.setProcedureDefs(result.hits());
            findProcedureDTO.setNumResults(result.total().hitCount());
        }
        catch (Exception e)
        {
            throw new WebApplicationException("Error retrieving search results", e);
        }
        return findProcedureDTO;
    }

    public static ProcedureStatusCounts getProcedureStatusCounts(EntityManager em, Integer programPk)
    {
        ProcedureStatusCounts counts = new ProcedureStatusCounts();

        try
        {
            String query = "SELECT pd.status, COUNT(pd) FROM ProcedureDetails pd " +
                    "JOIN pd.procedureDef pDef " +
                    "WHERE pd.editType = :editType";

            if (programPk != null)
            {
                query += " AND pDef.program.pk = :programPk";
            }

            query += " GROUP BY pd.status";

            TypedQuery<Object[]> q = em.createQuery(query, Object[].class);
            q.setParameter("editType", EditType.ORIGINAL);
            if (programPk != null)
            {
                q.setParameter("programPk", programPk);
            }

            List<Object[]> results = q.getResultList();

            for (Object[] result : results)
            {
                ProcedureStatus status = (ProcedureStatus) result[0];
                Long count = (Long) result[1];

                switch (status)
                {
                    case DRAFT:
                        counts.setDRAFT(count.intValue());
                        break;
                    case WAITING:
                        counts.setWAITING(count.intValue());
                        break;
                    case APPROVED:
                        counts.setAPPROVED(count.intValue());
                        break;
                    case READY:
                        counts.setREADY(count.intValue());
                        break;
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Error retrieving procedure status counts", e);
        }

        return counts;
    }

    public static List<ProcedureListDTO> getProceduresByStatus(EntityManager em, Integer programPk, String status)
    {
        try
        {
            String query = "SELECT new edu.jhuapl.sd.sig.epic.model.display.dto.ProcedureListDTO(pd, pDef.name, pDef.program, pDef.subsystem, ph.creationDate, ph.user) " +
                    "FROM ProcedureDetails pd JOIN pd.procedureDef pDef JOIN pd.procedureHeader ph " +
                    "WHERE pd.editType = :editType AND pd.status = :status";

            if (programPk != null)
            {
                query += " AND pDef.program.pk = :programPk";
            }

            TypedQuery<ProcedureListDTO> q = em.createQuery(query, ProcedureListDTO.class);
            q.setParameter("editType", EditType.ORIGINAL);
            q.setParameter("status", ProcedureStatus.valueOf(status));
            if (programPk != null)
            {
                q.setParameter("programPk", programPk);
            }

            return q.getResultList();
        }
        catch (Exception e)
        {
            LOGGER.error("Error retrieving procedures by status", e);
            throw new WebApplicationException("Failed to retrieve procedures by status", e);
        }
    }
}
