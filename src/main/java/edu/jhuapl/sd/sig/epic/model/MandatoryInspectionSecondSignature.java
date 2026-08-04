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
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "mandatory_inspection_second_signature")
@PrimaryKeyJoinColumn(name = "pk")
@AssociationOverride(
        name = "stepDef",
        joinColumns = {@JoinColumn(name = "step_def_id", referencedColumnName = "pk", foreignKey = @ForeignKey(name = "fk_mandatoryInspectionSecondSignature_stepDef"))})
public class MandatoryInspectionSecondSignature extends RunStepSecondSignature
{

    @Override
    public String toString()
    {
        return "mandatory inspection signed by: " + getUser().getDisplayName();
    }
}
