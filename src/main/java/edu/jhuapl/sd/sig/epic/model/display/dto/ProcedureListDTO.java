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
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ProcedureListDTO extends ProcedureDetailsDTO
{
    private int procedureDefPk;
    private ProcedureStatus status;
    private int numberOfRuns;
    private boolean isLatestReleasedRevision;

    // this constructor primarily used by Dashboard
    public ProcedureListDTO(ProcedureDef procedureDef, ProcedureDetails procedureDetails)
    {
        super(procedureDetails.getId(), procedureDetails.getProcedureDefVersion(), procedureDef.getName());
        this.procedureDefPk = procedureDef.getPk();
        this.procedureDetailsPk = procedureDetails.getPk();
        this.status = procedureDetails.getStatus();
        this.favoriteUsers = procedureDetails.getFavoriteUsers();
        List<ProcedureDetails> procedureDetailsList = procedureDef.getProcedureDetails().stream()
                .filter(pDet -> pDet.getRun() == null && pDet.getStatus().equals(ProcedureStatus.READY))
                .sorted((o1, o2) ->
                {
                    if (o1.getProcedureDefVersion() < o2.getProcedureDefVersion())
                        return -1;
                    else if (o1.getProcedureDefVersion() == o2.getProcedureDefVersion())
                        return 0;
                    else
                        return 1;
                }).collect(Collectors.toList());
        if (procedureDetailsList == null || procedureDetailsList.isEmpty())
        {
            this.isLatestReleasedRevision = false;
        }
        else
        {
            ProcedureDetails latestReleased = procedureDetailsList.get(procedureDetailsList.size() - 1);
            this.isLatestReleasedRevision = procedureDetails.getPk() == latestReleased.getPk();
        }
    }

    // this constructor primarily used by Reporting
    public ProcedureListDTO(ProcedureDetails pd, String procedureDefName, Program program, Subsystem subsystem,
            Date createdDate, Users author)
    {
        super(pd.getId(), pd.getProcedureDefVersion(), procedureDefName);
        this.program = program;
        this.subsystem = subsystem;
        this.author = author;
        this.createdDate = createdDate;
        this.url = pd.getProcedureUrl();
        this.status = pd.getStatus();
        this.numberOfRuns = pd.getProcedureDetailRuns().size();
    }
}
