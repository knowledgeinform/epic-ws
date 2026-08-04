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
import edu.jhuapl.sd.sig.epic.model.*;
import edu.jhuapl.sd.sig.epic.resource.model.NewProcData;
import edu.jhuapl.sd.sig.epic.utils.DataGeneratorUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestProcedureDAO extends ProcedureDAO
{

    //    public static void saveProcedureDef(ProcedureDef pd) {
    //        EntityManager em = JPAUtils.getEntityManager();
    //
    //        try {
    //            em.getTransaction().begin();
    //            List<ProcedureDetails> pdvs = new ArrayList<>(pd.getProcedureDetails());
    //            Program program = pd.getProgram();
    //            Subsystem subsystem = pd.getSubsystem();
    //            em.persist(program);
    //            em.persist(subsystem);
    ////            pd.setProcedureDefVersions(null);
    //            pd.setProgram(program);
    //            pd.setSubsystem(subsystem);
    ////            em.persist(pd);
    //            pd.setProcedureDetails(null);
    //            for (ProcedureDetails pdv : pdvs) {
    ////                pdv.setProcedureDef(null);
    ////                em.persist(pd);
    //
    //                ProcedureDef procedureDef = pdv.getProcedureDef();
    //                em.persist(procedureDef);
    //                em.persist(pdv.getProcedureHeader().getUser());
    //                ProcedureHeader ph = pdv.getProcedureHeader();
    ////                ph.setProcedureDefVersion(null);
    ////                em.persist(ph);
    //                pdv.setProcedureHeader(null);
    ////                em.persist(pdv.getProcedureHeader());
    //                Set<ProcedureInstruction> pis = pdv.getProcedureInstructions();
    //                Set<StepGroupDef> groups = pdv.getStepGroupDefs();
    //                Set<ProcedureApproval> approvals = pdv.getProcedureApprovals();
    //
    //                pdv.setProcedureInstructions(null);
    //                pdv.setProcedureApprovals(null);
    //                pdv.setStepGroupDefs(null);
    //                em.persist(pdv);
    //                for (ProcedureInstruction pi: pis) {
    ////                    pi.setProcedureDefVersion(null);
    //
    //                    em.persist(pi);
    //                }
    //                for (ProcedureApproval pa : approvals) {
    ////                    pa.setProcedureDefVersion(null);
    //                    em.persist(pa);
    //                }
    //                for (StepGroupDef sg : groups) {
    ////                    sg.setProcedureDefVersion(null);
    //                    em.persist(sg);
    //                }
    ////                em.persist(pdv);
    //                em.persist(ph);
    //            }
    ////            em.persist(pd);////////
    //            em.flush();
    //            em.getTransaction().commit();
    //        }
    //        catch (Exception e) {
    //            em.getTransaction().rollback();
    //            e.printStackTrace();
    //        }
    //        finally {
    //            em.close();
    //        }
    //    }

    public static ProcedureDef createProcedure()
    {
        try
        {
            // get lists of programs, subsystems, and users
            List<Program> programs = TestProcedureDAO.getAllPrograms();
            List<Subsystem> subsystems = TestProcedureDAO.getAllSubsystems();
            List<Users> users = TestProcedureDAO.getAllUsers();

            // assert that the lists are not null
            assertNotNull(programs);
            assertNotNull(subsystems);
            assertNotNull(users);

            Program program = programs.get(0);
            Subsystem subsystem = subsystems.get(0);
            Users user = users.get(0);

            // test data
            String procedureName = "Unit Test Create New Procedure";
            String procedureDescription = "This is a test of the create procedure function";
            Boolean esd0 = false;
            Boolean hazardous = false;

            ProcedureDef newProcedure = TestProcedureDAO.createProcedureDefWrapper(procedureName, procedureDescription, program.getPk(), subsystem.getPk(), esd0, hazardous, "", user.getUserId());

            // assert has a primary key
            assertNotNull(newProcedure);
            assertNotNull(newProcedure.getPk());

            return newProcedure;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static void deleteProcedureDefAfterDate(Date date)
    {
        EntityManager em = JPAUtils.getEntityManager();
        try
        {
            em.getTransaction().begin();
            String query = "SELECT p FROM ProcedureHeader p WHERE p.creationDate >= :date";
            Query q = em.createQuery(query);
            q.setParameter("date", date, TemporalType.DATE);
            List<ProcedureHeader> phList = q.getResultList();
            Set<ProcedureDef> procedureDefs = new HashSet<>();
            for (ProcedureHeader procedureHeader : phList)
            {
                procedureDefs.add(procedureHeader.getProcedureDetails().getProcedureDef());
                em.remove(procedureHeader);
            }
            for (ProcedureDef procedureDef : procedureDefs)
            {
                for (ProcedureDetails pdv : procedureDef.getProcedureDetails())
                {
                    Set<ProcedureApproval> pas = pdv.getProcedureApprovals();
                    Set<ProcedureInstruction> pis = pdv.getProcedureInstructions();
                    Set<StepGroupDef> groups = pdv.getStepGroupDefs();

                    for (ProcedureApproval pa : pas)
                    {
                        pa.setProcedureDetails(null);
                        em.remove(pa);
                    }
                    for (ProcedureInstruction pi : pis)
                    {
                        pi.setProcedureDetails(null);
                        em.remove(pi);
                    }
                    for (StepGroupDef s : groups)
                    {
                        s.setProcedureDetails(null);
                        em.remove(s);
                    }
                    //                    pdv.setProcedureDef(null);
                    em.remove(pdv);
                }
                em.remove(em.find(ProcedureDef.class, procedureDef.getPk()));
            }
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            em.getTransaction().rollback();
            e.printStackTrace();
        }
        finally
        {
            em.close();
        }
    }

    public static void deleteProcedureDef(Integer procedureDefPk)
    {
        EntityManager em = JPAUtils.getEntityManager();
        try
        {
            em.getTransaction().begin();
            ProcedureDef procedureDef = JPAUtils.getRecordById(em, ProcedureDef.class, procedureDefPk);

            for (ProcedureDetails procedureDetail : procedureDef.getProcedureDetails())
            {
                // delete the procedure header
                ProcedureHeader procedureHeader = procedureDetail.getProcedureHeader();
                procedureHeader.setProcedureDetails(null);
                em.remove(procedureHeader);

                Set<ProcedureApproval> pas = procedureDetail.getProcedureApprovals();
                Set<ProcedureInstruction> pis = procedureDetail.getProcedureInstructions();
                Set<StepGroupDef> groups = procedureDetail.getStepGroupDefs();
                Run run = procedureDetail.getRun();
                List<BlackLineComment> blackLineComments = procedureDetail.getBlackLineComments();

                for (ProcedureApproval pa : pas)
                {
                    pa.setProcedureDetails(null);
                    em.remove(pa);
                }
                for (ProcedureInstruction pi : pis)
                {
                    pi.setProcedureDetails(null);
                    em.remove(pi);
                }
                for (BlackLineComment comment : blackLineComments)
                {
                    comment.setProcedureDetails(null);
                    em.remove(comment);
                }
                if (run != null)
                {
                    run.setProcedureDetails(null);
                    em.remove(run);
                }
                deleteStepGroupsAndSteps(em, groups);
                em.remove(procedureDetail);
            }
            em.remove(procedureDef);

            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            em.getTransaction().rollback();
            e.printStackTrace();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    public static Set<ProcedureDef> getProcedureDefAfterDate(Date date)
    {
        EntityManager em = JPAUtils.getEntityManager();
        List<ProcedureHeader> phList = null;
        Set<ProcedureDef> procedureDefs = new HashSet<>();
        try
        {
            em.getTransaction().begin();
            String query = "SELECT p FROM ProcedureHeader p WHERE p.creationDate >= :date";
            Query q = em.createQuery(query);
            q.setParameter("date", date, TemporalType.DATE);
            phList = q.getResultList();
            for (ProcedureHeader procedureHeader : phList)
            {
                procedureDefs.add(procedureHeader.getProcedureDetails().getProcedureDef());
            }
        }
        catch (Exception e)
        {
            em.getTransaction().rollback();
            e.printStackTrace();
        }
        finally
        {
            em.close();
            return procedureDefs;
        }
    }

    public static List<Program> getAllPrograms()
    {
        EntityManager em = null;
        List<Program> allPrograms;
        try
        {
            em = JPAUtils.getEntityManager();
            allPrograms = JPAUtils.getAllRecordsForTable(em, Program.class);

            return allPrograms;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return null;
    }

    public static List<Subsystem> getAllSubsystems()
    {
        EntityManager em = null;
        List<Subsystem> allSubsystems;
        try
        {
            em = JPAUtils.getEntityManager();
            allSubsystems = JPAUtils.getAllRecordsForTable(em, Subsystem.class);

            return allSubsystems;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return null;
    }

    public static List<TestingPhase> getAllTestingPhases()
    {
        EntityManager em = null;
        List<TestingPhase> allTestingPhases;
        try
        {
            em = JPAUtils.getEntityManager();
            allTestingPhases = JPAUtils.getAllRecordsForTable(em, TestingPhase.class);

            return allTestingPhases;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return null;
    }

    public static List<Users> getAllUsers()
    {
        EntityManager em = null;
        List<Users> allUsers;
        try
        {
            em = JPAUtils.getEntityManager();
            allUsers = JPAUtils.getAllRecordsForTable(em, Users.class);

            return allUsers;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return null;
    }

    public static ProcedureDef getProcedureDefByPk(Integer pk)
    {
        EntityManager em = null;
        ProcedureDef procedureDef;
        try
        {
            em = JPAUtils.getEntityManager();
            procedureDef = JPAUtils.getRecordById(em, ProcedureDef.class, pk);

            return procedureDef;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return null;
    }

    private static void deleteStepGroupsAndSteps(EntityManager em, Set<StepGroupDef> stepGroupDefs)
    {
        if (stepGroupDefs == null || stepGroupDefs.isEmpty())
        {
            return;
        }
        for (StepGroupDef group : stepGroupDefs)
        {
            for (StepDef step : group.getStepDefs())
            {
                step.setStepGroupDef(null);
                em.remove(step);
            }
            if (group.getStepGroupDefsChildren() != null && !group.getStepGroupDefsChildren().isEmpty())
            {
                deleteStepGroupsAndSteps(em, group.getStepGroupDefsChildren());
            }
            em.remove(group);
        }
    }

    public static ProcedureDef createProcedureDefWrapper(String name, String description, Integer programPk,
            Integer subsystemPk, Boolean esd0, Boolean hazardous, String hazardDescription, Integer userId)
    {
        EntityManager em = JPAUtils.getEntityManager();
        ProcedureDef procedureDef = ProcedureDAO.createNewProcedure(em, name, description, programPk, subsystemPk, esd0, hazardous, hazardDescription, userId);
        JPAUtils.closeEntityManager(em);
        return procedureDef;
    }

    public static ProcedureApproval saveProcedureApprovalWrapper(ProcedureApproval procedureApproval)
    {
        EntityManager em = JPAUtils.getEntityManager();
        ProcedureApproval pa = ProcedureApproverDAO.insertNewProcedureApproval(em, procedureApproval.getUsers().getUserId(), procedureApproval.getApprovalType(),
                procedureApproval.getProcedureDetails().getPk());
        JPAUtils.closeEntityManager(em);
        return pa;
    }

    public static void transitionToWaitingWrapper(ProcedureApproval procedureApproval, Long dueDate)
    {
        EntityManager em = JPAUtils.getEntityManager();
        Users randomUser = DataGeneratorUtils.getRandomUser();
        ProcedureDetails pd = ProcedureDetailsDAO.transitionToWaiting(em, procedureApproval.getProcedureDetails().getPk(), dueDate, randomUser);
        JPAUtils.closeEntityManager(em);
    }

    public static ProcedureApproval setApprovalFlagWrapper(ProcedureApproval procedureApproval, boolean isApproved)
    {
        EntityManager em = JPAUtils.getEntityManager();
        ProcedureApproval pa = ProcedureApproverDAO.setApprovalFlag(em, procedureApproval.getPk(), isApproved);
        JPAUtils.closeEntityManager(em);
        return pa;
    }

    public static ProcedureDetails transitionToReadyWrapper(ProcedureDetails procedureDetail)
    {
        EntityManager em = JPAUtils.getEntityManager();
        Users randomUser = DataGeneratorUtils.getRandomUser();
        ProcedureDetails details = ProcedureDetailsDAO.transitionToReadyForRelease(em, procedureDetail.getPk(), randomUser);
        JPAUtils.closeEntityManager(em);
        return details;
    }

    public static void blacklineCommentWrapper(BlackLineComment comment, Boolean manualStepValidation)
    {
        EntityManager em = JPAUtils.getEntityManager();
        BlackLineDAO.saveBlackLine(em, comment, manualStepValidation);
        JPAUtils.closeEntityManager(em);
    }

    public static void runStepCommentWrapper(RunStepComment comment)
    {
        EntityManager em = JPAUtils.getEntityManager();
        StepDAO.saveRunStepComment(em, comment);
        JPAUtils.closeEntityManager(em);
    }

    public static void saveRunValueForCheckboxStepWrapper(StepCheckbox step, Users user)
    {
        EntityManager em = JPAUtils.getEntityManager();
        StepDAO.saveRunValueForCheckboxStep(em, step, user);
        JPAUtils.closeEntityManager(em);
    }

    public static void saveRunValueForSingleValueStepWrapper(StepSingleValue step, Users user)
    {
        EntityManager em = JPAUtils.getEntityManager();
        StepDAO.saveRunValueForSingleValueStep(em, step, user);
        JPAUtils.closeEntityManager(em);
    }

    public static void saveRunValueForTableStepWrapper(StepTable step, StepTableCell cell, Users user)
    {
        EntityManager em = JPAUtils.getEntityManager();
        StepDAO.saveRunValueForTableStep(em, step, cell, user);
        JPAUtils.closeEntityManager(em);
    }

    public static void RunStepSecondSignatureWrapper(RunStepSecondSignature signature)
    {
        EntityManager em = JPAUtils.getEntityManager();
        SecondSignatureDAO.saveRunStepSecondSignature(em, signature);
        JPAUtils.closeEntityManager(em);
    }

    public static void addUserWrapper(Users user)
    {
        EntityManager em = JPAUtils.getEntityManager();
        UsersDAO.addUser(em, user);
        JPAUtils.closeEntityManager(em);
    }

    public static ProcedureInstruction saveProcedureInstructionSectionWrapper(ProcedureInstruction instruction)
    {
        EntityManager em = JPAUtils.getEntityManager();
        instruction = InstructionDAO.insertNewInstructionSection(em, instruction.getSectionName(), instruction.getDisplayOrder(),
                instruction.getProcedureDetails().getPk());
        JPAUtils.closeEntityManager(em);
        return instruction;
    }

    public static ProcedureInstruction updateInstructionSectionWrapper(ProcedureInstruction databaseInstruction, ProcedureInstruction testInstruction)
    {
        EntityManager em = JPAUtils.getEntityManager();
        ProcedureInstruction pi = InstructionDAO.updateInstructionSection(em, databaseInstruction.getPk(), testInstruction.getSectionName(),
                testInstruction.getText(), testInstruction.getDisplayOrder());
        JPAUtils.closeEntityManager(em);
        return pi;
    }

    public static StepGroupDef saveStepGroupDefWrapper(StepGroupDef group)
    {
        EntityManager em = JPAUtils.getEntityManager();
        Integer parentPk = -1;
        if (group.getStepGroupDefParent() != null)
        {
            parentPk = group.getStepGroupDefParent().getPk();
        }
        List<StepGroupDef> groups = new ArrayList<>();
        groups.add(group);
        List<StepGroupDef> savedGroup = StepGroupDAO.insertNewStepGroupDefTransaction(em, groups, group.getProcedureDetails().getPk());
        JPAUtils.closeEntityManager(em);
        return savedGroup.get(0);
    }

    public static StepDef saveStepDefWrapper(StepDef step)
    {
        EntityManager em = JPAUtils.getEntityManager();
        StepDef savedStep = null;
        if (step.getType().equals(StepType.CHECKBOX) || step.getType().equals(StepType.SINGLE_VALUE))
        {
            savedStep = StepDAO.createNewSingleValueOrCheckboxStep(em, step);
        }
        else if (step.getType().equals(StepType.TABLE))
        {
            savedStep = StepDAO.createNewStepTable(em, (StepTable) step);
        }
        JPAUtils.closeEntityManager(em);
        return savedStep;
    }

    public static ProcedureDef cloneProcedureWrapper(NewProcData newProcData, Integer procedureDetailsPk, Users user)
    {
        EntityManager em = JPAUtils.getEntityManager();
        ProcedureDef pd = ProcedureDAO.cloneProcedure(em, newProcData, procedureDetailsPk, user);
        JPAUtils.closeEntityManager(em);
        return pd;
    }

    public static ProcedureDetails createRunWrapper(ProcedureDetails procedureDetails, Run run, Integer runNumber, Users user)
    {
        EntityManager em = JPAUtils.getEntityManager();
        //user needs to be in the session
        user = em.find(Users.class, user.getUserId());
        procedureDetails = JPAUtils.getRecordById(em, ProcedureDetails.class, procedureDetails.getPk());
        ProcedureDetails newRun = RunDAO.createNewRun(em, procedureDetails, runNumber, run, user);
        JPAUtils.closeEntityManager(em);
        return newRun;
    }

}
