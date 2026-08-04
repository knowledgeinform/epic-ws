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

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "step_table_cell", uniqueConstraints = @UniqueConstraint(name = "row_cellIndex_UNIQUE", columnNames = {"step_table_row_pk", "cell_index"}))
public class StepTableCell implements Comparable
{
    @Id
    @Column(name = "pk")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer pk;

    @Column(name = "editable")
    private Boolean editable;

    @Column(name = "non_editable_value")
    private String nonEditableValue;

    @Column(name = "cell_index")
    private Integer cellIndex;

    @Column(name = "optional")
    private boolean optional;

    @ManyToOne
    @JoinColumn(name = "step_table_row_pk", referencedColumnName = "pk", nullable = false, foreignKey = @ForeignKey(name = "fk_stepTableCell_stepTableRow"))
    @OrderBy("rowNumber ASC")
    @JsonIgnoreProperties({"stepTable, stepTableCells"})
    private StepTableRow stepTableRow;

    @Override
    public int compareTo(Object o)
    {
        return this.cellIndex.compareTo(((StepTableCell) o).cellIndex);
    }

    @Override
    public String toString()
    {
        String noHtmlStr = "";
        // this.nonEditableValue has lots of html tags need to remove them first before convert it to displayable string.
        if (this.nonEditableValue != null)
        {
            noHtmlStr = this.nonEditableValue.replaceAll("\\<(/?[^\\>]+)\\>", "\\ ").replaceAll("\\s+", " ").replaceAll("&.*?;", "").replaceAll("\\<.*?>", "").trim();
        }

        return "Value for row " + (this.stepTableRow.getRowNumber() + 1) + ", column " + (this.cellIndex + 1) +
                " set to '" + noHtmlStr + "'";
    }
}
