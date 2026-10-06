/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.procedures;

import edu.jhuapl.sd.sig.epic.model.ProcedureApproval;
import edu.jhuapl.sd.sig.epic.model.ProcedureApprovalType;
import edu.jhuapl.sd.sig.epic.utils.TestUtils;

public enum TestProcedureApprovalData
{

    PROCEDURE_APPROVAL_1
    {
        @Override
        public ProcedureApproval getTestData()
        {
            ProcedureApproval procedureApproval = new ProcedureApproval();
            procedureApproval.setIsApproved(true);
            procedureApproval.setApprovalType(ProcedureApprovalType.APPROVER);
            procedureApproval.setUsers(TestUtils.getTestUser());
            return procedureApproval;
        }
    },
    PROCEDURE_APPROVAL_2
    {
        @Override
        public ProcedureApproval getTestData()
        {
            ProcedureApproval procedureApproval = new ProcedureApproval();
            procedureApproval.setIsApproved(false);
            procedureApproval.setApprovalType(ProcedureApprovalType.REVIEWER);
            procedureApproval.setUsers(TestUtils.getTestUser());
            return procedureApproval;
        }
    },
    PROCEDURE_APPROVAL_3
    {
        @Override
        public ProcedureApproval getTestData()
        {
            ProcedureApproval procedureApproval = new ProcedureApproval();
            procedureApproval.setIsApproved(false);
            procedureApproval.setApprovalType(ProcedureApprovalType.APPROVER);
            procedureApproval.setUsers(TestUtils.getTestUser());
            return procedureApproval;
        }
    };

    public abstract ProcedureApproval getTestData();

}
