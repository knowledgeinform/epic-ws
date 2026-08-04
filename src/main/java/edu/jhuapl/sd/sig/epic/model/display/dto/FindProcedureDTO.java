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

import edu.jhuapl.sd.sig.epic.model.ProcedureDef;
import edu.jhuapl.sd.sig.epic.model.ProcedureDetails;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Creates an object containing the total number of procedure detail hits
 * and a subset of those hits
 */
@Getter
@Setter
public class FindProcedureDTO
{
    protected List<ProcedureDetails> procedureDetails;
    protected List<ProcedureDef> procedureDefs;
    protected Long numResults;
}
