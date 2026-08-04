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

public class ClonedProcedureData
{
    private NewProcData procedureDef;
    private int procedureDetailsPk;

    public ClonedProcedureData()
    {

    }

    public NewProcData getProcedureDef()
    {
        return procedureDef;
    }

    public void setProcedureDef(NewProcData procedureDef)
    {
        this.procedureDef = procedureDef;
    }

    public int getProcedureDetailsPk()
    {
        return procedureDetailsPk;
    }

    public void setProcedureDetailsPk(int procedureDetailsPk)
    {
        this.procedureDetailsPk = procedureDetailsPk;
    }
}
