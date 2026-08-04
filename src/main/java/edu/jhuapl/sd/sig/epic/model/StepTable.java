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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.javers.core.metamodel.annotation.DiffIgnore;

import javax.persistence.*;
import java.util.SortedSet;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "step_table")
@PrimaryKeyJoinColumn(name = "pk")
public class StepTable extends StepDef
{
    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "stepTable", cascade = {CascadeType.ALL}, orphanRemoval = true)
    @OrderBy("rowNumber ASC")
    @JsonIgnoreProperties("stepTable")
    private SortedSet<StepTableRow> stepTableRows;

    @OneToOne
    @JoinColumn(name = "run_value", referencedColumnName = "pk", foreignKey = @ForeignKey(name = "fk_stepTable_runValueStepTable"))
    @DiffIgnore
    private StepTable runValue;

    //	public SortedSet<StepTableRow> getStepTableRows()
    //	{
    //		return stepTableRows;
    //	}
    //
    //	public void setStepTableRows(SortedSet<StepTableRow> stepTableRows)
    //	{
    //		this.stepTableRows = stepTableRows;
    //	}

    public String getValueStringForRowAndCell(int rowIndex, int cellIndex)
    {
        return stepTableRows.stream().filter(row -> row.getRowNumber().equals(rowIndex)).findFirst().get().getValueStringForCellIndex(cellIndex);
    }
}
