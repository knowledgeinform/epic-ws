/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.model.display.dto;

import edu.jhuapl.sd.sig.epic.model.ProcedureApprovalType;
import edu.jhuapl.sd.sig.epic.model.RunStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RunApprovalDashboardDTO extends ApprovalDashboardDTO
{
    private String runName;
    private RunStatus runStatus;

    public RunApprovalDashboardDTO(String id, int pk, ProcedureApprovalType approvalType, String runName, RunStatus runStatus)
    {
        super(id, pk, approvalType);
        this.runName = runName;
        this.runStatus = runStatus;
    }

}
