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

@Data
public class ProgramRoleSelection
{
    private Integer pk;
    private String name;
    private Boolean bypassValidation;
    private Boolean deletable;
    private Integer programPk;

    public ProgramRoleSelection()
    {}

    public ProgramRoleSelection(
            Integer pk,
            String name,
            Boolean bypassValidation,
            Boolean deletable,
            Integer programPk)
    {
        this.pk = pk;
        this.name = name;
        this.bypassValidation = bypassValidation;
        this.deletable = deletable;
        this.programPk = programPk;
    }
}
