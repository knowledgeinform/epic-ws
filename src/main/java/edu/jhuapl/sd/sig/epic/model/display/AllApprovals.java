/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.model.display;

import edu.jhuapl.sd.sig.epic.model.display.dto.ProcApprovalDashboardDTO;
import edu.jhuapl.sd.sig.epic.model.display.dto.RunApprovalDashboardDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AllApprovals
{
    List<ProcApprovalDashboardDTO> procedureApprovals;

    //	List<WitnessApproval> witnessApprovals;

    List<RunApprovalDashboardDTO> closeoutApprovals;

    public AllApprovals()
    {
        this.procedureApprovals = null;
        //		this.witnessApprovals = null;
        this.closeoutApprovals = null;
    }
}
