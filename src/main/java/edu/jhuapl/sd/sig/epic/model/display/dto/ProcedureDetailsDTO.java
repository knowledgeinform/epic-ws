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

import edu.jhuapl.sd.sig.epic.model.Program;
import edu.jhuapl.sd.sig.epic.model.Subsystem;
import edu.jhuapl.sd.sig.epic.model.Users;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Set;

@Getter
@Setter
public abstract class ProcedureDetailsDTO
{
    protected int procedureDetailsPk;
    protected String id;
    protected String procedureDefName;
    protected int procedureDefVersion;
    protected Program program;
    protected Subsystem subsystem;
    protected Users author;
    protected Date createdDate;
    protected String url;
    protected Set<Users> favoriteUsers;

    public ProcedureDetailsDTO(String id, int procedureDefVersion, String procedureDefName)
    {
        this.id = id;
        this.procedureDefVersion = procedureDefVersion;
        this.procedureDefName = procedureDefName;
    }
}
