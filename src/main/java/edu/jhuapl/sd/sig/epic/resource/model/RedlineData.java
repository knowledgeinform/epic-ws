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

import edu.jhuapl.sd.sig.epic.model.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RedlineData
{
    private RedLineComment redLineComment;
    private ProcedureInstruction procedureInstruction;
    private Integer procedureDetailsPk;
    private StepGroupDef stepGroupDef;
    private StepDef stepDef;
    private List<StepDef> stepDefList;
    private List<Integer> instructionPkList;
    private List<StepGroupDef> stepGroupDefList;
}
