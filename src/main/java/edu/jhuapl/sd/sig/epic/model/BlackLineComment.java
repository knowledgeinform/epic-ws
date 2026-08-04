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

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "black_line_comment")
@PrimaryKeyJoinColumn(name = "pk")
@AssociationOverrides({
        @AssociationOverride(name = "procedureDetails",
                joinColumns = @JoinColumn(name = "procedure_details_fk", referencedColumnName = "pk", foreignKey = @ForeignKey(name = "fk_blackLineComment_procedureDetails"))),
        @AssociationOverride(name = "stepGroupDef",
                joinColumns = @JoinColumn(name = "step_group_def_fk", referencedColumnName = "pk", foreignKey = @ForeignKey(name = "fk_blackLineComment_stepGroupDef"))),
        @AssociationOverride(name = "stepDef", joinColumns = @JoinColumn(name = "step_def_fk", referencedColumnName = "pk", foreignKey = @ForeignKey(name = "fk_blackLineComment_stepDef"))),
        @AssociationOverride(name = "procedureInstruction",
                joinColumns = @JoinColumn(name = "procedure_instruction_fk", referencedColumnName = "pk", foreignKey = @ForeignKey(name = "fk_blackLineComment_procedureInstruction"))),
        @AssociationOverride(name = "procedureChangeType",
                joinColumns = @JoinColumn(name = "procedure_change_type", nullable = false, referencedColumnName = "pk", foreignKey = @ForeignKey(name = "fk_procedure_change_type1")))
})
public class BlackLineComment extends RedBlackLineComment
{}
