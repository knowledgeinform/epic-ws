/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.model;

public enum ProcedureStatus
{
    DRAFT("Procedure set to DRAFT status, all approvals reset."),
    WAITING("Procedure transitioned to IN REVIEW status."),
    APPROVED("Procedure transitioned to APPROVED status."),
    READY("Procedure transitioned to RELEASED status.");

    private final String statusMessage;

    ProcedureStatus(String s)
    {
        this.statusMessage = s;
    }

    public String toString()
    {
        return this.statusMessage;
    }
}
