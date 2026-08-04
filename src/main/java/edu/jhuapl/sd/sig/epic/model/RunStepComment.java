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
import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "run_step_comment")
@PrimaryKeyJoinColumn(name = "pk")
public class RunStepComment extends Comment
{
    @Column(name = "is_nonconformance")
    private Boolean isNonconformance = false;

    @ManyToOne()
    @JoinColumn(name = "step_def_fk", referencedColumnName = "pk", nullable = false, foreignKey = @ForeignKey(name = "fk_runStepComment_stepDef"))
    @JsonIgnoreProperties("runStepComments")
    private StepDef stepDef;
}
