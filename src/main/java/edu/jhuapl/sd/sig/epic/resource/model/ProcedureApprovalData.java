/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.resource.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.jhuapl.sd.sig.epic.model.ProcedureApproval;

@JsonIgnoreProperties("procedureDetails")
public class ProcedureApprovalData extends ProcedureApproval
{

    int procedureDetailsPk;
    int userId;

    public ProcedureApprovalData()
    {}

    public int getProcedureDetailsPk()
    {
        return procedureDetailsPk;
    }

    public void setProcedureDetailsPk(int procedureDetailsPk)
    {
        this.procedureDetailsPk = procedureDetailsPk;
    }

    public int getUserId()
    {
        return userId;
    }

    public void setUserId(int userId)
    {
        this.userId = userId;
    }
}
