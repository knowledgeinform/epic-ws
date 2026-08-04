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

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcedureChangeTypeSelection
{
    private Integer pk;
    private String name;
    private Boolean isEnabled;
    private Boolean acceptsAllSignatures;
    private String description;
    private List<ProgramRoleSelection> requiredRoleApprovals = new ArrayList<>();
    private Boolean deletable;
    private Integer programPk;

    public ProcedureChangeTypeSelection()
    {}

    public ProcedureChangeTypeSelection(
            Integer pk,
            String name,
            Boolean isEnabled,
            Boolean acceptsAllSignatures,
            String description,
            Integer programPk,
            Boolean deletable)
    {
        this.pk = pk;
        this.name = name;
        this.isEnabled = isEnabled;
        this.acceptsAllSignatures = acceptsAllSignatures;
        this.description = description;
        this.programPk = programPk;
        this.deletable = deletable;
    }
}
