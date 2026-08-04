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

import edu.jhuapl.sd.sig.epic.model.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class RunListDTO extends ProcedureDetailsDTO
{
    private int runNumber;
    private String runName;
    private RunStatus status;
    private Run run;
    protected int sumNonConformance;

    // primarily used by Dashboard
    public RunListDTO(int procedureDetailsPk, String id, int runNumber, int procedureDefVersion, String runName, RunStatus status, ProcedureDetails det)
    {
        super(id, procedureDefVersion, "");
        this.procedureDetailsPk = procedureDetailsPk;
        this.runNumber = runNumber;
        this.runName = runName;
        this.status = status;
        this.favoriteUsers = det.getFavoriteUsers();
    }

    // primarily used by reporting
    public RunListDTO(ProcedureDetails pd, String procedureDefName, Program program,
            Subsystem subsystem, Date createdDate, Users author)
    {
        super(pd.getId(), pd.getProcedureDefVersion(), procedureDefName);
        this.program = program;
        this.subsystem = subsystem;
        this.author = pd.getOriginalProcedureDetails().getProcedureHeader().getUser();
        this.createdDate = createdDate;
        this.url = pd.getRunUrl();
        this.runNumber = pd.getRunNumber();
        this.run = pd.getRun();
    }

    @Override
    public int hashCode()
    {
        // run and procedure ids are always unique
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        // run and procedure ids are always unique.
        return this.id.equalsIgnoreCase(((RunListDTO) obj).id);
    }
}
