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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Setter
@Getter
@Entity
@Table(name = "run_closeout_sticky_comment")
@PrimaryKeyJoinColumn(name = "pk")
public class RunCloseoutStickyComment extends Comment
{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_pd_pk", referencedColumnName = "pk", foreignKey = @ForeignKey(name = "fk_runCloseoutStickyComment_procedureDetails"))
    @JsonIgnoreProperties({"blackLineComments", "originalProcedureDetails", "procedureDetailRuns", "redLineComments", "stepGroupDefs", "procedureApprovals", "procedureInstructions", "histories",
            "runCloseoutStickyComments"})
    private ProcedureDetails procedureDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_group_pk", referencedColumnName = "pk", foreignKey = @ForeignKey(name = "fk_runCloseoutStickyComment_stepGroup"))
    @JsonIgnoreProperties({"blackLineComments", "procedureDetails", "stepDefs", "stepGroupDefParent", "stepGroupDefsChildren", "redLineComments", "runCloseoutStickyComments"})
    private StepGroupDef stepGroupDef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_pk", referencedColumnName = "pk", foreignKey = @ForeignKey(name = "fk_runCloseoutStickyComment_step"))
    @JsonIgnoreProperties({"blackLineComments", "procedureDetails", "stepGroupDef", "runStepComments", "redLineComments", "histories", "equipmentList", "runCloseoutStickyComments"})
    private StepDef stepDef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instruction_pk", referencedColumnName = "pk", foreignKey = @ForeignKey(name = "fk_runCloseoutStickyComment_procedureInstruction"))
    @JsonIgnoreProperties({"blackLineComments", "procedureDetails", "redLineComments", "runCloseoutStickyComments"})
    private ProcedureInstruction procedureInstruction;

    @Column(name = "is_complete", nullable = false)
    private Boolean isComplete = false;
}
