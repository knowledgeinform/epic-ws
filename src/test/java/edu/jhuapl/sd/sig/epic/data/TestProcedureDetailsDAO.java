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

import edu.jhuapl.sd.sig.epic.model.*;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import java.util.Iterator;

public class TestProcedureDetailsDAO
{
    /**
     * Updates the ProcedureDetails with some redline changes.
     * 
     * @param pd A ProcedureDetails with at least 3 instructions and at least 3 Step Groups, each with 3 at least children.
     * @param originalProcedureDetails The ProcedureDetails that this redline is based on.
     */
    public static void addRedLines(EntityManager em, ProcedureDetails pd, ProcedureDetails originalProcedureDetails)
    {

        pd.setOriginalProcedureDetails(originalProcedureDetails);
        RedlineDAO.markThisRunAsRedlined(em, pd);

        // Pick items to modify
        Iterator<ProcedureInstruction> instIter = pd.getProcedureInstructions().iterator();
        ProcedureInstruction instToDelete = instIter.next();
        ProcedureInstruction instToEdit = instIter.next();
        ProcedureInstruction instToAdd = instIter.next();
        Iterator<StepGroupDef> sgdIter = pd.getStepGroupDefs().iterator();
        StepGroupDef sgdToDelete = sgdIter.next();
        StepGroupDef sgdToEdit = sgdIter.next();
        StepGroupDef sgdToAdd = sgdIter.next();
        Iterator<StepDef> sdIter = sgdToAdd.getStepDefs().iterator();
        StepDef sdToDelete = sdIter.next();
        StepDef sdToEdit = sdIter.next();
        StepDef sdToAdd = sdIter.next();

        instToDelete.setEditType(EditType.REDLINE_DELETE);
        instToEdit.setEditType(EditType.REDLINE_EDIT);
        instToAdd.setEditType(EditType.REDLINE_ADD);

        sgdToDelete.setEditType(EditType.REDLINE_DELETE);
        sgdToEdit.setEditType(EditType.REDLINE_EDIT);
        sgdToAdd.setEditType(EditType.REDLINE_ADD);

        sdToDelete.setEditType(EditType.REDLINE_DELETE);
        sdToEdit.setEditType(EditType.REDLINE_EDIT);
        sdToAdd.setEditType(EditType.REDLINE_ADD);
    }

    /**
     * Creates a new run with all required fields filled in. The run is not saved to the database.
     * 
     * @param pd The ProcedureDetails to associate the run with.
     */
    public static Run generateTestRun(EntityManager em, ProcedureDetails pd)
    {

        // Count number of runs we already have so we can generate a unique name.
        CriteriaBuilder qb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = qb.createQuery(Long.class);
        cq.select(qb.count(cq.from(ProcedureDetails.class)));
        Long numRuns = em.createQuery(cq).getSingleResult();

        Run run = new Run();
        run.setProcedureDetails(pd);
        run.setName("Unit Test Run Creation " + numRuns); // Runs must have unique names. And we currently can't delete procedures due to bug (as noted earlier in code).
        run.setDescription("This is a unit test of a created run");
        run.setTestingPhase(TestProcedureDAO.getAllTestingPhases().get(0));
        return run;
    }
}
