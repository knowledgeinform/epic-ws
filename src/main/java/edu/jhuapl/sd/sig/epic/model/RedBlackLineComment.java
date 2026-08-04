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
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import java.util.Date;

@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor
public abstract class RedBlackLineComment extends Comment
{
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn()
    @JsonIgnoreProperties({"blackLineComments", "originalProcedureDetails", "procedureDetailRuns", "redLineComments", "stepGroupDefs", "procedureApprovals", "procedureInstructions",
            "runCloseoutStickyComments", "histories"})
    private ProcedureDetails procedureDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn()
    @JsonIgnoreProperties({"blackLineComments", "procedureDetails", "stepDefs", "stepGroupDefParent", "stepGroupDefsChildren", "redLineComments", "runCloseoutStickyComments"})
    private StepGroupDef stepGroupDef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn()
    @JsonIgnoreProperties({"blackLineComments", "procedureDetails", "stepGroupDef", "runStepComments", "redLineComments", "histories", "equipmentList", "runCloseoutStickyComments"})
    private StepDef stepDef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn()
    @JsonIgnoreProperties({"blackLineComments", "procedureDetails", "redLineComments", "runCloseoutStickyComments"})
    private ProcedureInstruction procedureInstruction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn()
    private ProcedureChangeType procedureChangeType;

    public RedBlackLineComment(Date commentTimestamp, String commentText, CommentType commentType, Users users)
    {
        super(commentTimestamp, commentText, commentType, users);
    }

    public RedBlackLineComment(Date commentTimestamp, String commentText, CommentType commentType, Users users, ProcedureChangeType procedureChangeType)
    {
        super(commentTimestamp, commentText, commentType, users);
        this.procedureChangeType = procedureChangeType;
    }
}
