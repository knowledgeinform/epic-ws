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
import edu.jhuapl.sd.sig.epic.model.ProcedureInstruction;
import edu.jhuapl.sd.sig.epic.model.ProcedureStatus;
import edu.jhuapl.sd.sig.epic.model.Run;
import edu.jhuapl.sd.sig.epic.model.RunApproval;
import edu.jhuapl.sd.sig.epic.model.RunCloseoutComment;
import edu.jhuapl.sd.sig.epic.model.RunCloseoutCommentReply;
import edu.jhuapl.sd.sig.epic.model.RunCloseoutStickyComment;
import edu.jhuapl.sd.sig.epic.model.RunStatus;
import edu.jhuapl.sd.sig.epic.model.RunStepComment;
import edu.jhuapl.sd.sig.epic.model.StepDef;
import edu.jhuapl.sd.sig.epic.model.StepGroupDef;
import edu.jhuapl.sd.sig.epic.model.TestingPhase;
import edu.jhuapl.sd.sig.epic.model.Users;
import edu.jhuapl.sd.sig.epic.model.display.dto.RunListDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.WebApplicationException;

import java.util.*;
import java.util.stream.Collectors;

public class RunDAO
{
    private static final Logger LOGGER = LogManager.getLogger(RunDAO.class.getName());

    public static Run getRunByUniqueCode(EntityManager em, String runId)
    {
        Run run;

        String qString = "SELECT r FROM Run r WHERE r.procedureDetails.id = :runId";
        TypedQuery<Run> q = em.createQuery(qString, Run.class);
        q.setParameter("runId", runId);
        run = q.getSingleResult();

        initializeRunProperties(run);
        return run;
    }

    public static Run getRunByPk(EntityManager em, Integer runPk)
    {

        Run run = null;

        String qString = "SELECT run FROM Run run WHERE run.pk = :runId";
        TypedQuery<Run> q = em.createQuery(qString, Run.class);
        q.setParameter("runId", runPk);
        run = q.getSingleResult();

        initializeRunProperties(run);
        return run;
    }

    private static void initializeRunProperties(Run run)
    {
        ProcedureDetails details = run.getProcedureDetails();

        Hibernate.initialize(details.getStepGroupDefs());
        Hibernate.initialize(details.getProcedureInstructions());
        Hibernate.initialize(details.getBlackLineComments());
        Hibernate.initialize(details.getRedLineComments());
        Hibernate.initialize(details.getRunCloseoutStickyComments());
        Hibernate.initialize(details.getHistories());

        details.getBlackLineComments().forEach(comment ->
        {
            Hibernate.initialize(comment.getProcedureChangeType());
            Hibernate.initialize(comment.getBlackRedLineSignatures());
        });

        details.getRedLineComments().forEach(comment ->
        {
            Hibernate.initialize(comment.getProcedureChangeType());
            Hibernate.initialize(comment.getBlackRedLineSignatures());
        });

        ProcedureDetails original = details.getOriginalProcedureDetails();
        if (original != null)
        {
            Hibernate.initialize(original.getProcedureApprovals());
            Hibernate.initialize(original.getHistories());
            Hibernate.initialize(original.getProcedureDetailRuns());
        }
    }

    public static List<RunListDTO> getFavoriteRunList(EntityManager em, Integer userid)
    {
        List<RunListDTO> runs = null;

        String qString = "SELECT new edu.jhuapl.sd.sig.epic.model.display.dto.RunListDTO(pd.pk, pd.id, pd.runNumber, pd.procedureDefVersion, r.name, r.status, pd) FROM Run r JOIN r.procedureDetails pd JOIN r.procedureDetails.favoriteUsers u WHERE u.userId = :user_id";

        TypedQuery<RunListDTO> q = em.createQuery(qString, RunListDTO.class);
        q.setParameter("user_id", userid);
        runs = q.getResultList();

        return runs;
    }

    /**
     * This method accept the original Pdv and creates a new run based off of it.
     * 
     * @param em
     * @param originalPdvData
     * @return
     */
    public static ProcedureDetails createNewRun(EntityManager em, ProcedureDetails originalPdvData, Integer runNumber,
            Run runData, Users user)
    {

        LOGGER.debug("Creating new run");

        // Verify procedure is ready.
        if (!originalPdvData.getStatus().equals(ProcedureStatus.READY))
        {
            String msg = "Error creating a new run for procedure version with id of: " + originalPdvData.getId() + ": Procedure not in ready state.";
            LOGGER.error(msg);
            throw new WebApplicationException(msg);
        }

        // Verify procedure is latest revision.

        // sort the revisions in reverse order, so the latest is first.
        List<ProcedureDetails> readyRevisions = originalPdvData.getProcedureDef().getLatestRevisions().stream()
                .filter(p -> p.getStatus().equals(ProcedureStatus.READY))
                .sorted(Comparator.comparingInt(ProcedureDetails::getProcedureDefVersion).reversed())
                .collect(Collectors.toList());

        if (originalPdvData.getProcedureDefVersion() != readyRevisions.get(0).getProcedureDefVersion())
        {
            String msg = "Error creating a new run for procedure version with id of: " + originalPdvData.getId() + ": Procedure not latest revision.";
            LOGGER.error(msg);
            throw new WebApplicationException(msg);
        }

        ProcedureDetails runPdv = null;
        try
        {
            em.getTransaction().begin();

            ProcedureDetails pdvEntityForData;
            EditType editTypeForRun;

            // if the originalPdv has a redlinedVersion that is not null, we want to create the run from that
            // redlinedVersion and set the procedure to an edit type of LOCKED_RUN
            if (originalPdvData.getRedlinedVersion() != null)
            {
                // will make the run from the redlinedVersion
                pdvEntityForData = ProcedureDetailsDAO.getProcedureDetailsByUniqueCode(em, originalPdvData.getRedlinedVersion());
                editTypeForRun = EditType.LOCKED_RUN;
            }
            else
            {
                // make the run from the original procedure details
                pdvEntityForData = originalPdvData;
                editTypeForRun = EditType.RUN;
            }
            Hibernate.initialize(pdvEntityForData.getStepGroupDefs());
            Hibernate.initialize(pdvEntityForData.getProcedureInstructions());
            Hibernate.initialize(pdvEntityForData.getRedLineComments());
            ProcedureDef procedureDef = em.find(ProcedureDef.class, pdvEntityForData.getProcedureDef().getPk());

            // create a new Run from the data
            Run runInfo = new Run();

            runInfo.setDescription(runData.getDescription());
            runInfo.setName(runData.getName());
            runInfo.setTestingPhase(em.find(TestingPhase.class, runData.getTestingPhase().getPk()));
            runInfo.setUser(user);
            runInfo.setStatus(RunStatus.RUNNING);
            runInfo.setCreatedDate(new Date());

            // copy the pdv to a new procedureDetails object
            LOGGER.debug("Copying procedure details.");
            runPdv = ProcedureDetailsDAO.copyProcedureDetails(em, pdvEntityForData, pdvEntityForData.getProcedureDefVersion(), pdvEntityForData.getEsd0(),
                    pdvEntityForData.getHazardous(), pdvEntityForData.getHazardDescription(), editTypeForRun, user, false);
            runPdv.setRunNumber(runNumber);

            runInfo.setProcedureDetails(runPdv);
            runPdv.setRun(runInfo);
            runPdv.setProcedureDef(procedureDef);

            if (runPdv.getStepGroupDefs() != null && runPdv.getStepGroupDefs().size() != 0)
            {
                StepGroupDAO.persistStepGroupArray(em, runPdv.getStepGroupDefs());
            }

            em.persist(runPdv);

            if (runPdv.getProcedureInstructions() != null && runPdv.getProcedureInstructions().size() > 0)
            {
                for (ProcedureInstruction pi : runPdv.getProcedureInstructions())
                {
                    em.persist(pi);
                }
            }
            em.persist(runInfo);

            // finally set the id

            runPdv.setId(procedureDef.getProgram().getCode() + "-" + procedureDef.getSubsystem().getCode() + "-" +
                    procedureDef.getPk() + "-" + runPdv.getProcedureDefVersion() + "-" + runInfo.getTestingPhase().getCode() + "-" +
                    runPdv.getRunNumber());

            // create a status history
            runPdv.getHistories().add(new History(null, runPdv, user));

            LOGGER.debug("Commiting created new run to database.");

            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
                String msg = "Error creating a new run for procedure version with id of: " + originalPdvData.getId();
                LOGGER.error(msg, e);
                throw new WebApplicationException(msg, e);
            }
        }
        return runPdv;
    }

    /**
     * Find run by name to ascertain if the name is unique.
     * 
     * @param em
     * @param name
     * @return
     */
    public static boolean isRunNameUnique(EntityManager em, String name)
    {
        String q = "SELECT r FROM Run r WHERE LOWER(r.name) = :name";
        TypedQuery<Run> tq = em.createQuery(q, Run.class);
        tq.setParameter("name", name.toLowerCase());
        List<Run> runs = tq.getResultList();
        if (runs.size() == 0)
        {
            return true;
        }
        return false;
    }

    public static Run transitionRunToReviewing(EntityManager em, Integer runPk, Users user)
    {
        Run run = null;
        try
        {
            em.getTransaction().begin();

            // set Run Status to REVIEWING, or APPROVED if there are no approvers
            run = JPAUtils.getRecordById(em, Run.class, runPk);
            if (run.getProcedureDetails().getRunCloseoutStickyComments() != null && !run.getProcedureDetails().getRunCloseoutStickyComments().isEmpty())
            {
                throw new WebApplicationException("All run closeout stickies must be resolved before submitting the run for closeout");
            }

            if (run.getCloseoutSubmissionUser() == null)
            {
                run.setCloseoutSubmissionUser(user);
            }

            if (run.getCloseoutSubmittedDate() == null)
            {
                run.setCloseoutSubmittedDate(new Date());
            }

            if (run.getRunApprovals() == null || run.getRunApprovals().isEmpty())
            {
                run.setStatus(RunStatus.APPROVED);
                run.getProcedureDetails().getHistories().add(new History(new Date(), RunStatus.APPROVED.toString(), user, null, run.getProcedureDetails()));
            }
            else
            {
                run.getProcedureDetails().getHistories().add(new History(new Date(), RunStatus.REVIEWING.toString(), user, null, run.getProcedureDetails()));
                run.setStatus(RunStatus.REVIEWING);
            }

            boolean fullyApproved = true;
            for (RunApproval runApproval : run.getRunApprovals())
            {
                // Only check approvals with active approver
                boolean activeApprover = runApproval.getApproverDisabled() == null || !runApproval.getApproverDisabled();
                if (activeApprover)
                {
                    if (runApproval.getIsApproved() != null && !runApproval.getIsApproved())
                    {
                        runApproval.setIsApproved(null);
                        runApproval = em.merge(runApproval);
                        fullyApproved = false;
                    }
                    else if (runApproval.getIsApproved() == null)
                    {
                        fullyApproved = false;
                    }
                }
            }
            if (fullyApproved)
            {
                run.setStatus(RunStatus.APPROVED);
            }

            run = em.merge(run);

            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            LOGGER.error("Could not transition run to REVIEWING status", e);
            throw new WebApplicationException(e);
        }
        return run;
    }

    public static Set<Run> getRunsWhereCloseoutIsSubmittedAndIncomplete(EntityManager em)
    {
        String q = "SELECT r FROM Run r WHERE r.status = :reviewing";
        TypedQuery<Run> tq = em.createQuery(q, Run.class);
        tq.setParameter("reviewing", RunStatus.REVIEWING);
        return new HashSet<>(tq.getResultList());
    }

    public static Run transitionRunToCorrectingStatus(EntityManager em, Run run, Users user)
    {
        try
        {
            em.getTransaction().begin();
            run.getProcedureDetails().getHistories().add(new History(new Date(), RunStatus.CORRECTING.toString(), user, null, run.getProcedureDetails()));
            run.setStatus(RunStatus.CORRECTING);

            run = em.merge(run);
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            LOGGER.error("Could not transition run to CORRECTING status", e);
            throw new WebApplicationException(e);
        }
        return run;
    }

    public static Run transitionRunToCompletedStatus(EntityManager em, Integer runPk, Users user)
    {
        Run run = null;
        try
        {
            em.getTransaction().begin();
            run = JPAUtils.getRecordById(em, Run.class, runPk);
            run.getProcedureDetails().getHistories().add(new History(new Date(), RunStatus.COMPLETED.toString(), user, null, run.getProcedureDetails()));
            run.setStatus(RunStatus.COMPLETED);
            run.setCloseoutCompletedDate(new Date());

            // note that all stickies are linked to procedureDetails and optionally instruction/step group/step
            // per users, stickies should not be kept when run goes to COMPLETED status; they should be deleted.
            ProcedureDetails procedureDetails = run.getProcedureDetails();
            for (RunCloseoutStickyComment runCloseoutStickyComment : procedureDetails.getRunCloseoutStickyComments())
            {
                runCloseoutStickyComment.setStepDef(null);
                runCloseoutStickyComment.setStepGroupDef(null);
                runCloseoutStickyComment.setProcedureInstruction(null);
                runCloseoutStickyComment.setProcedureDetails(null);
                em.remove(runCloseoutStickyComment);
            }

            procedureDetails.setRunCloseoutStickyComments(null);
            procedureDetails = em.merge(procedureDetails);
            run.setProcedureDetails(procedureDetails);

            run = em.merge(run);
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            LOGGER.error("Could not transition run to COMPLETED status", e);
            throw new WebApplicationException(e);
        }
        return run;
    }

    public static RunCloseoutComment saveRunCloseoutComment(EntityManager em, RunCloseoutComment runCloseoutComment, Users user)
    {
        try
        {
            em.getTransaction().begin();

            runCloseoutComment.setUsers(user);
            runCloseoutComment.setCommentTimestamp(new Date());
            runCloseoutComment.setRunApproval(JPAUtils.getRecordById(em, RunApproval.class, runCloseoutComment.getRunApproval().getPk()));
            em.persist(runCloseoutComment);

            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String message = "Could not save run closeout comment for approval with pk = " + runCloseoutComment.getRunApproval().getPk();
            LOGGER.error(message, e);
            throw new WebApplicationException(message, e);
        }
        return runCloseoutComment;
    }

    public static RunCloseoutCommentReply saveRunCloseoutCommentReply(EntityManager em, RunCloseoutCommentReply runCloseoutCommentReply, Users user)
    {
        try
        {
            em.getTransaction().begin();

            runCloseoutCommentReply.setUsers(user);
            runCloseoutCommentReply.setCommentTimestamp(new Date());
            runCloseoutCommentReply.setRunCloseoutComment(JPAUtils.getRecordById(em, RunCloseoutComment.class,
                    runCloseoutCommentReply.getRunCloseoutComment().getPk()));
            em.persist(runCloseoutCommentReply);

            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String message = "Could not save a reply to run closeout comment with pk = " + runCloseoutCommentReply.getRunCloseoutComment().getPk();
            LOGGER.error(message, e);
            throw new WebApplicationException(message, e);
        }
        return runCloseoutCommentReply;
    }

    public static RunCloseoutStickyComment saveRunCloseoutStickyComment(EntityManager em, RunCloseoutStickyComment runCloseoutStickyComment, Users user)
    {
        try
        {
            em.getTransaction().begin();

            runCloseoutStickyComment.setUsers(user);
            runCloseoutStickyComment.setCommentTimestamp(new Date());
            runCloseoutStickyComment.setProcedureDetails(JPAUtils.getRecordById(em, ProcedureDetails.class,
                    runCloseoutStickyComment.getProcedureDetails().getPk()));
            if (runCloseoutStickyComment.getProcedureInstruction() != null)
            {
                runCloseoutStickyComment.setProcedureInstruction(JPAUtils.getRecordById(em, ProcedureInstruction.class,
                        runCloseoutStickyComment.getProcedureInstruction().getPk()));
            }
            else if (runCloseoutStickyComment.getStepGroupDef() != null)
            {
                runCloseoutStickyComment.setStepGroupDef(JPAUtils.getRecordById(em, StepGroupDef.class, runCloseoutStickyComment.getStepGroupDef().getPk()));
            }
            else if (runCloseoutStickyComment.getStepDef() != null)
            {
                runCloseoutStickyComment.setStepDef(JPAUtils.getRecordById(em, StepDef.class, runCloseoutStickyComment.getStepDef().getPk()));
            }

            em.persist(runCloseoutStickyComment);

            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String message = "Could not save a run closeout sticky for run with pk = " + runCloseoutStickyComment.getProcedureDetails().getPk();
            LOGGER.error(message, e);
            throw new WebApplicationException(message, e);
        }
        return runCloseoutStickyComment;
    }

    public static boolean markRunCloseoutStickyCommentComplete(EntityManager em, RunCloseoutStickyComment runCSC)
    {
        RunCloseoutStickyComment runCloseoutStickyComment = null;
        try
        {
            em.getTransaction().begin();

            runCloseoutStickyComment = JPAUtils.getRecordById(em, RunCloseoutStickyComment.class, runCSC.getPk());
            em.remove(runCloseoutStickyComment);

            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String message = "Could not update a run closeout sticky with pk = " + runCSC.getPk();
            LOGGER.error(message, e);
            throw new WebApplicationException(message, e);
        }
        return true;
    }

    public static List<RunListDTO> getListOfAllRunsForReport(EntityManager em,
            Integer programPk,
            Integer subsystemPk,
            Integer testingPhasePk, boolean getNonConformance)
    {
        List<RunListDTO> allRunsForReport = null;
        try
        {
            String query = "SELECT new edu.jhuapl.sd.sig.epic.model.display.dto.RunListDTO(pDet, pDef.name, pDef.program, pDef.subsystem, r.createdDate, r.user) " +
                    "from ProcedureDetails pDet JOIN pDet.procedureDef pDef JOIN pDet.run r " +
                    "WHERE pDet.editType != :editType";

            if (programPk != null)
            {
                query += " AND pDef.program.pk = :programPk";
            }
            if (subsystemPk != null)
            {
                query += " AND pDef.subsystem.pk = :subsystemPk";
            }
            if (testingPhasePk != null)
            {
                query += " AND r.testingPhase.pk = : testingPhasePk";
            }
            TypedQuery<RunListDTO> q = em.createQuery(query, RunListDTO.class);
            q.setParameter("editType", EditType.ORIGINAL);
            if (programPk != null)
            {
                q.setParameter("programPk", programPk);
            }
            if (subsystemPk != null)
            {
                q.setParameter("subsystemPk", subsystemPk);
            }
            if (testingPhasePk != null)
            {
                q.setParameter("testingPhasePk", testingPhasePk);
            }

            allRunsForReport = q.getResultList();

            if (getNonConformance)
            {
                for (int i = 0; i < allRunsForReport.size(); i++)
                {
                    ProcedureDetails pDet = allRunsForReport.get(i).getRun().getProcedureDetails();

                    List<RunStepComment> nonconformanceCommentsForThisRun = pDet.getAllSteps()
                            .stream()
                            .flatMap(step -> Optional.ofNullable(step.getRunStepComments())
                                    .orElseGet(Collections::emptySortedSet)
                                    .stream()
                                    .filter(comment -> comment.getIsNonconformance() != null && comment.getIsNonconformance()))
                            .collect(Collectors.toList());

                    allRunsForReport.get(i).setSumNonConformance(nonconformanceCommentsForThisRun.size());
                }
            }

            return allRunsForReport;
        }
        catch (Exception e)
        {
            String message = "Could not retrieve list of all runs";
            LOGGER.error(message, e);
            throw new WebApplicationException(message, e);
        }
    }
}
