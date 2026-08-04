/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic;

import edu.jhuapl.sd.sig.epic.email.TestEmailEngine;
import edu.jhuapl.sd.sig.epic.export.TestCleanupEngine;
import edu.jhuapl.sd.sig.epic.export.TestExport;
import edu.jhuapl.sd.sig.epic.logging.TestClientSideLogging;
import edu.jhuapl.sd.sig.epic.procedures.TestProcedureHeaderOperations;
import edu.jhuapl.sd.sig.epic.procedures.TestProcedureWorkflowStateChangeOperations;
import edu.jhuapl.sd.sig.epic.procedures.TestStepGroupOperations;
import edu.jhuapl.sd.sig.epic.runs.*;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({TestStepGroupOperations.class, TestProcedureHeaderOperations.class, TestExport.class,
        TestCleanupEngine.class, TestEmailEngine.class, TestSecondSignatureDAO.class, TestRunOperations.class,
        TestClientSideLogging.class, TestProcedureWorkflowStateChangeOperations.class,
        TestEquipmentListOperations.class, TestRunStepOperations.class, TestRunStateChangeOperations.class})
public class EpicTestSuite
{}
