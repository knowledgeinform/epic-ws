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
import edu.jhuapl.sd.sig.epic.model.ProcedureApproval;
import edu.jhuapl.sd.sig.epic.model.ProcedureAttachment;
import edu.jhuapl.sd.sig.epic.model.ProcedureDetails;
import edu.jhuapl.sd.sig.epic.model.ProcedureHeader;
import edu.jhuapl.sd.sig.epic.model.ProcedureInstruction;
import edu.jhuapl.sd.sig.epic.model.ProcedureStatus;
import edu.jhuapl.sd.sig.epic.model.StepDef;
import edu.jhuapl.sd.sig.epic.model.StepGroupDef;
import edu.jhuapl.sd.sig.epic.model.Users;
import edu.jhuapl.sd.sig.epic.model.display.dto.FindProcedureDTO;
import edu.jhuapl.sd.sig.epic.model.util.AttachmentHandler;

import edu.jhuapl.sd.sig.epic.model.util.CopyUtils;
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
import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

public class ProcedureDetailsDAO
{
    private static final Logger LOGGER = LogManager.getLogger();

    public static ProcedureDetails transitionToWaiting(EntityManager em, Integer pk, Long dueDateMillis, Users user)
    {

        Date dueDate = new Date(dueDateMillis);

        ProcedureDetails pdv = null;
        try
        {
            em.getTransaction().begin();
            pdv = JPAUtils.getRecordById(em, ProcedureDetails.class, pk);

            pdv.getHistories().add(new History(new Date(), ProcedureStatus.WAITING.toString(), user, null, pdv));

            ProcedureHeader header = JPAUtils.getRecordById(em, ProcedureHeader.class, pdv.getProcedureHeader().getPk());
            header.setSubmittedForReviewDate(new Date());
            em.merge(header);

            pdv.setStatus(ProcedureStatus.WAITING);
            pdv.setProcedureApprovalDueDate(dueDate);

            em.merge(pdv);
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            LOGGER.error("Error while transitioning procedure to waiting status", e);
            throw new WebApplicationException("Error while transitioning procedure to waiting status", e);
        }

        return em.find(ProcedureDetails.class, pk);

    }

    public static ProcedureDetails transitionToReadyForRelease(EntityManager em, Integer pk, Users user)
    {
        ProcedureDetails pdv;

        em.getTransaction().begin();

        pdv = em.find(ProcedureDetails.class, pk);

        ProcedureHeader header = JPAUtils.getRecordById(em, ProcedureHeader.class, pdv.getProcedureHeader().getPk());
        header.setReleasedDate(new Date());
        em.merge(header);

        pdv.getHistories().add(new History(new Date(), ProcedureStatus.READY.toString(), user, null, pdv));
        pdv.setStatus(ProcedureStatus.READY);
        pdv = em.merge(pdv);

        em.getTransaction().commit();

        return pdv;

    }

    public static ProcedureDetails returnToDraft(EntityManager em, Integer pk, Users user)
    {
        ProcedureDetails pdv;

        em.getTransaction().begin();

        pdv = em.find(ProcedureDetails.class, pk);

        ProcedureHeader header = JPAUtils.getRecordById(em, ProcedureHeader.class, pdv.getProcedureHeader().getPk());
        header.setSubmittedForReviewDate(null);
        header.setApprovedDate(null);
        em.merge(header);

        for (ProcedureApproval pa : pdv.getProcedureApprovals())
        {
            pa.setIsApproved(null);
            em.merge(pa);
        }

        pdv.getHistories().add(new History(new Date(), ProcedureStatus.DRAFT.toString(), user, null, pdv));
        pdv.setStatus(ProcedureStatus.DRAFT);
        pdv.setProcedureApprovalDueDate(null);

        em.merge(pdv);
        em.getTransaction().commit();

        return em.find(ProcedureDetails.class, pk);

    }

    public static edu.jhuapl.sd.sig.epic.model.ProcedureDetails getProcedureDetailsByUniqueCode(EntityManager em, String procedureId)
    {
        ProcedureDetails def;

        String qString = "SELECT v FROM ProcedureDetails v WHERE v.id = :procedureId";

        TypedQuery<ProcedureDetails> q = em.createQuery(qString, ProcedureDetails.class);
        q.setParameter("procedureId", procedureId);
        def = q.getSingleResult();

        return def;
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
    public static FindProcedureDTO findProcedures(EntityManager em, String searchString, String programId,
            String subsystemId, EditType editType, String procedureStatus,
            String testingPhaseId, Integer offSetResults, Integer limitResults)
    {
        FindProcedureDTO findProcedureDTO = new FindProcedureDTO();
        SearchResult<ProcedureDetails> result;
        try
        {
            SearchSession searchSession = Search.session(em);

            result = searchSession.search(ProcedureDetails.class)
                    .where(f -> f.bool(
                            b ->
                            {
                                // Determine if searching for runs or procedures
                                if (editType != EditType.ORIGINAL)
                                {
                                    // Return only runs
                                    b.mustNot(f.match()
                                            .field("editType")
                                            .matching(EditType.ORIGINAL));
                                    // Search against either procedure or run names and descriptions
                                    if (!searchString.isEmpty())
                                    {
                                        b.must(f.simpleQueryString()
                                                .fields("procedureDef.name", "procedureDef.description", "id",
                                                        "run.name", "run.description")
                                                .matching(searchString)
                                                .defaultOperator(BooleanOperator.AND));
                                    }

                                    if (!testingPhaseId.isEmpty())
                                    {
                                        int testingIdInt = Integer.parseInt(testingPhaseId);
                                        b.must(f.match()
                                                .field("run.testingPhase.pk")
                                                .matching(testingIdInt));
                                    }
                                }
                                else
                                {
                                    // Return only procedures
                                    b.must(f.match()
                                            .fields("editType")
                                            .matching(editType));
                                    // Only search against procedure names and descriptions
                                    if (!searchString.isEmpty())
                                    {
                                        b.must(f.simpleQueryString()
                                                .fields("procedureDef.name", "procedureDef.description", "id")
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
                                            .fields("procedureDef.program.pk")
                                            .matching(programIdInt));
                                }
                                if (!subsystemId.isEmpty())
                                {
                                    int subsystemIdInt = Integer.parseInt(subsystemId);
                                    b.must(f.match()
                                            .fields("procedureDef.subsystem.pk")
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
                                                .fields("status")
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
            for (ProcedureDetails procedureDetails : result.hits())
            {
                if (procedureDetails.getStatus().equals(ProcedureStatus.READY) &&
                        procedureDetails.getEditType().equals(EditType.ORIGINAL))
                {
                    Hibernate.initialize(procedureDetails.getProcedureDetailRuns());
                }
            }

            findProcedureDTO.setProcedureDetails(result.hits());
            findProcedureDTO.setNumResults(result.total().hitCount());
        }
        catch (Exception e)
        {
            throw new WebApplicationException("Error retrieving search results", e);
        }
        return findProcedureDTO;
    }

    /**
     * DAO method for updating the hazard information for a procedure. If the procedure contains steps that have
     * been flagged as hazardous, this method will not allow the procedure hazard flag to be set to false.
     * 
     * @param em
     * @param procedureDetailsPk
     * @param isHazard
     * @param hazardDescription
     * @return
     */
    public static ProcedureDetails updateProcedureDetailsHazard(EntityManager em, Integer procedureDetailsPk, Boolean isHazard, String hazardDescription)
    {
        ProcedureDetails procedureDetails = null;
        try
        {
            em.getTransaction().begin();
            procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDetailsPk);

            if (procedureDetails != null)
            {
                if (!isHazard && groupsHaveHazardousChild(procedureDetails.getStepGroupDefs()))
                {
                    throw new WebApplicationException("Cannot remove hazardous marking from procedure while there steps marked hazardous.");
                }
                procedureDetails.setHazardous(isHazard);
                procedureDetails.setHazardDescription(hazardDescription);
                em.merge(procedureDetails);
            }
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
                throw new WebApplicationException("Could not update hazard information for procedure details with pk: " + procedureDetailsPk, e);
            }
        }

        return procedureDetails;
    }

    public static ProcedureDetails updateProcedureDetailsEsd0(EntityManager em, Integer pk, boolean val)
    {
        ProcedureDetails procedureDetails = null;
        try
        {
            em.getTransaction().begin();
            procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, pk);

            // Fail disable when children have flag set.
            if (!val && groupsHaveEsd0Child(procedureDetails.getStepGroupDefs()))
            {
                throw new WebApplicationException("Cannot disable procedure ESD Class 0 flag while children steps have the flag.");
            }

            if (procedureDetails != null)
            {
                procedureDetails.setEsd0(val);
                em.merge(procedureDetails);
            }
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
                throw new WebApplicationException("Could not update Esd0 information for procedure details with pk: " + pk, e);
            }
        }

        return procedureDetails;
    }

    /**
     * This method accepts a given procedureDetails and returns a copy with all primary keys removed. Edit Types are set
     * to the given editType argument
     * 
     * @param pdvToCopy
     * @param version
     * @param hazardous
     * @param hazardDescription
     * @param editType
     * @param user A user to set as a favorite and the author of the procedure details.
     * @param omitRedlines If true, redlined Instructions, Steps, and Step Group Defs are not copied.
     * @return
     */
    public static ProcedureDetails copyProcedureDetails(EntityManager em, ProcedureDetails pdvToCopy, int version, Boolean esd0, Boolean hazardous, String hazardDescription,
            EditType editType, Users user, Boolean omitRedlines) throws Exception
    {
        // now need to set pdv
        ProcedureDetails pdv = new ProcedureDetails();
        pdv.setProcedureDefVersion(version);
        //if creating a run, keep the procedure status, otherwise this is draft of a new proc.
        pdv.setStatus((editType.equals(EditType.RUN) || editType.equals(EditType.LOCKED_RUN)) ? pdvToCopy.getStatus() : ProcedureStatus.DRAFT);
        pdv.setEditType(editType);
        pdv.setEsd0(esd0);
        pdv.setHazardous(hazardous);
        pdv.setHazardDescription(hazardDescription);

        //set as favorite
        Set<Users> favoriteUsers = new HashSet<>();
        favoriteUsers.add(user);
        pdv.setFavoriteUsers(favoriteUsers);
        user.getProcedureDetails().add(pdv);

        // set the procedure header
        ProcedureHeader ph = new ProcedureHeader();
        ph.setProcedureDetails(pdv);
        ph.setUser(user);

        if (editType == EditType.ORIGINAL)
        {
            ph.setCreationDate(new Date());
        }
        else
        {
            ph.setCreationDate(pdvToCopy.getProcedureHeader().getCreationDate());
        }

        ph.setText(pdvToCopy.getProcedureHeader().getText());
        pdv.setProcedureHeader(ph);

        //copy procedure header attachments
        if (pdvToCopy.getProcedureHeader().getAttachments() != null)
        {
            LOGGER.debug("Setting procedure attachments.");
            ph.setAttachments(new TreeSet<>());
            for (ProcedureAttachment originalAttachment : pdvToCopy.getProcedureHeader().getAttachments())
            {
                if (omitRedlines && originalAttachment.getEditType().equals(EditType.REDLINE_DELETE))
                    continue;

                ProcedureAttachment newPa = new ProcedureAttachment();
                newPa.setProcedureHeader(ph);
                newPa.setIsImage(originalAttachment.getIsImage());
                newPa.setOriginalFilename(originalAttachment.getOriginalFilename());
                newPa.setFilename(UUID.randomUUID().toString());
                newPa.setEditType(editType);
                // Copy the file
                LOGGER.debug("Saving attachment with Attachment Utils: source: {}, destination: {}", AttachmentHandler.getFullPathToFile(originalAttachment),
                        AttachmentHandler.UPLOAD_ROOT_DIR + File.separator + newPa.getFilename());
                AttachmentHandler.saveAttachment(AttachmentHandler.getFullPathToFile(originalAttachment),
                        AttachmentHandler.UPLOAD_ROOT_DIR + File.separator + newPa.getFilename());

                ph.getAttachments().add(newPa);
            }
        }

        // copy the procedure instructions
        pdv.setProcedureInstructions(CopyUtils.copyProcedureInstructions(pdvToCopy.getProcedureInstructions(), pdv, editType, true));

        // copy the step group defs
        // need to recurse and also copy steps
        pdv.setStepGroupDefs(CopyUtils.copyStepGroupDefs(pdvToCopy.getStepGroupDefs(), pdv, editType, null, null, null, true));

        // don't need to copy approvals

        // if editType is RUN or LOCKED_RUN, set the originalPdv
        if (editType.equals(EditType.RUN))
        {
            pdv.setOriginalProcedureDetails(pdvToCopy);
        }
        else if (editType.equals(EditType.LOCKED_RUN))
        {
            // if this is a locked run, then we are copying from a redlined run, so get the originalProcedureDetails of
            // the run being copied.
            pdv.setOriginalProcedureDetails(JPAUtils.getRecordById(em, ProcedureDetails.class, pdvToCopy.getOriginalProcedureDetails().getPk()));
        }
        else
        {
            pdv.setOriginalProcedureDetails(null);
        }

        return pdv;
    }

    /**
     * Returns the latest version of a given procedure. If redlines exist for that version, the redline is returned.
     */
    public static ProcedureDetails getLatestVersionForProcedure(EntityManager em, String procedureId)
    {
        ProcedureDetails pd = getProcedureDetailsByUniqueCode(em, procedureId);
        ProcedureDetails latestVersion = pd.getProcedureDef().getLatestRevisions().last();

        // Return the procedure if there are no redlines.
        if (latestVersion.getRedlinedVersion() == null)
        {
            return latestVersion;
        }
        else
        {
            return getProcedureDetailsByUniqueCode(em, latestVersion.getRedlinedVersion());
        }
    }

    /**
     * Takes a Run or Procedure ID and creates a new procedure revision in authoring mode.
     * Will retrieve the latest redline version for the procedure to base the revision off of.
     * 
     * @param user A user to set as a favorite and the author of the revision.
     */
    public static ProcedureDetails createRevision(EntityManager em, String procedureId, Users user)
    {
        ProcedureDetails pd = ProcedureDetailsDAO.getLatestVersionForProcedure(em, procedureId);
        ProcedureDetails newRev = null;

        try
        {
            newRev = copyProcedureDetails(em, pd, pd.getProcedureDefVersion() + 1, pd.getEsd0(), pd.getHazardous(), pd.getHazardDescription(), EditType.ORIGINAL, user, true);

            newRev.setStatus(ProcedureStatus.DRAFT);

            newRev.setProcedureDef(pd.getProcedureDef());
            newRev.getProcedureDef().getProcedureDetails().add(newRev);

            ProcedureDetailsDAO.persistProcedureDetails(em, newRev, true, user, pd);
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            String msg = "Unable to copy procedure details: ";
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }

        return newRev;
    }

    public static Boolean groupsHaveEsd0Child(SortedSet<StepGroupDef> sgds)
    {
        List<StepGroupDef> groupsWithEsd0 = sgds.stream().filter((sgd) ->
        {
            return stepHasEsd0Child(sgd);
        }).collect(Collectors.toList());
        return !groupsWithEsd0.isEmpty();
    }

    private static Boolean stepHasEsd0Child(StepGroupDef sgd)
    {
        return stepsHaveEsd0Child(sgd.getStepDefs()) || groupsHaveEsd0Child(sgd.getStepGroupDefsChildren());
    }

    private static Boolean stepsHaveEsd0Child(SortedSet<StepDef> sds)
    {
        return !sds.stream().filter((sd) -> sd.getEsd0()).collect(Collectors.toList()).isEmpty();
    }

    public static Boolean groupsHaveHazardousChild(SortedSet<StepGroupDef> groups)
    {
        return !groups.stream()
                .filter(group -> groupHasHazardousChild(group))
                .collect(Collectors.toList())
                .isEmpty();
    }

    private static Boolean groupHasHazardousChild(StepGroupDef group)
    {
        return stepsAreHazardous(group.getStepDefs()) || groupsHaveHazardousChild(group.getStepGroupDefsChildren());
    }

    private static Boolean stepsAreHazardous(SortedSet<StepDef> steps)
    {
        return !steps.stream()
                .filter(step -> step.getHazardous())
                .collect(Collectors.toList())
                .isEmpty();
    }

    /**
     * Persists the procedure and its children, including instructions, step groups, etc.
     * Note: EntityManager is not closed.
     */
    public static void persistProcedureDetails(EntityManager em, ProcedureDetails pdv, boolean createHistory, Users user, ProcedureDetails previousRevision)
    {
        try
        {

            em.getTransaction().begin();

            // Persist the base procedure
            em.persist(pdv);

            em.persist(pdv.getProcedureDef());

            // Persist all steps.
            StepGroupDAO.persistStepGroupArray(em, pdv.getStepGroupDefs());

            // Persist instructions.
            if (pdv.getProcedureInstructions() != null)
            {
                for (ProcedureInstruction pi : pdv.getProcedureInstructions())
                {
                    em.persist(pi);
                }
            }

            em.flush();

            // Update ID
            pdv.setId(pdv.getProcedureDef().getProgram().getCode() + "-" + pdv.getProcedureDef().getSubsystem().getCode() + "-" + pdv.getProcedureDef().getPk() + "-" + pdv.getProcedureDefVersion());

            if (createHistory)
            {
                History history = new History(null, pdv, user);
                em.persist(history);
                pdv.getHistories().add(history);
                previousRevision.getHistories().add(history);
            }

            em.getTransaction().commit();

        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            String msg = "Unable to persist procedure details: ";
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
    }
}
