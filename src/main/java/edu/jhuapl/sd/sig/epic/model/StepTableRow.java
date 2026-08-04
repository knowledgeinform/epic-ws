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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.javers.core.metamodel.annotation.DiffIgnore;

import javax.persistence.*;
import java.util.SortedSet;

@Entity
@Table(name = "step_table_row", uniqueConstraints = @UniqueConstraint(name = "stepTable_rowNumber_UNIQUE", columnNames = {"step_table_pk", "row_number"}))
public class StepTableRow implements Comparable
{

    @Id
    @Column(name = "pk")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer pk;

    @Column(name = "row_number")
    private Integer rowNumber;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "stepTableRow", cascade = {CascadeType.ALL}, orphanRemoval = true)
    @OrderBy("cellIndex ASC")
    @JsonIgnoreProperties("stepTableRow")
    private SortedSet<StepTableCell> stepTableCells;

    @ManyToOne
    @JoinColumn(name = "step_table_pk", referencedColumnName = "pk", nullable = false, foreignKey = @ForeignKey(name = "fk_stepTableRow_stepTable"))
    @JsonIgnoreProperties("stepTableRows")
    @DiffIgnore
    private StepTable stepTable;

    public Integer getPk()
    {
        return pk;
    }

    public void setPk(Integer pk)
    {
        this.pk = pk;
    }

    public Integer getRowNumber()
    {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber)
    {
        this.rowNumber = rowNumber;
    }

    public SortedSet<StepTableCell> getStepTableCells()
    {
        return stepTableCells;
    }

    public void setStepTableCells(SortedSet<StepTableCell> stepTableCells)
    {
        this.stepTableCells = stepTableCells;
    }

    public StepTable getStepTable()
    {
        return stepTable;
    }

    public void setStepTable(StepTable stepTable)
    {
        this.stepTable = stepTable;
    }

    @JsonIgnore
    public String getRowContents()
    {
        StringBuilder sb = new StringBuilder();
        for (StepTableCell c : stepTableCells)
        {
            if (sb.length() > 0)
            {
                sb.append(",");
            }
            sb.append("\"");
            sb.append(c.getNonEditableValue());
            sb.append("\"");
        }

        return sb.toString();
    }

    @Override
    public int compareTo(Object o)
    {
        return this.rowNumber.compareTo(((StepTableRow) o).rowNumber);
    }

    public String getValueStringForCellIndex(int cellIndex)
    {
        return stepTableCells.stream().filter(cell -> cell.getCellIndex().equals(cellIndex)).findFirst().get().toString();
    }
}
