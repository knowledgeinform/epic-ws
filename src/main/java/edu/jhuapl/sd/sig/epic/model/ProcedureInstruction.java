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
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.Comparator;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "pk")
@Entity
@Table(name = "procedure_instruction")
public class ProcedureInstruction extends ChangeLogsHolder implements Comparable<ProcedureInstruction>
{

    @Id
    @Column(name = "pk")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer pk;

    @Column(name = "section_name")
    private String sectionName;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "text")
    private String text;

    @ManyToOne
    @JoinColumn(name = "procedure_details_pk", referencedColumnName = "pk", nullable = false, foreignKey = @ForeignKey(name = "fk_procedureInstruction_procedureDetails"))
    @JsonIgnoreProperties({"procedureInstructions"})
    private ProcedureDetails procedureDetails;

    @Enumerated(EnumType.STRING)
    @Column(name = "edit_type")
    private EditType editType;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "procedureInstruction")
    @OrderBy("commentTimestamp ASC")
    @JsonIgnoreProperties({"procedureInstruction", "procedureDetails", "stepGroupDef", "stepDef"})
    private List<BlackLineComment> blackLineComments;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "procedureInstruction")
    @OrderBy("commentTimestamp ASC")
    @JsonIgnoreProperties({"procedureInstruction", "procedureDetails", "stepGroupDef", "stepDef"})
    private List<RedLineComment> redLineComments;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "procedureInstruction")
    @OrderBy("commentTimestamp ASC")
    @JsonIgnoreProperties({"procedureInstruction", "procedureDetails", "stepGroupDef", "stepDef"})
    private List<RunCloseoutStickyComment> runCloseoutStickyComments;

    @Override
    public int compareTo(ProcedureInstruction o)
    {

        return Comparator.comparing(ProcedureInstruction::getDisplayOrder)
                .thenComparing(ProcedureInstruction::getPk, Comparator.nullsFirst(Integer::compareTo))
                .compare(this, o);
        //        return this.displayOrder.compareTo(((ProcedureInstruction) o).displayOrder);
    }

    @Override
    @JsonIgnore
    public String getId()
    { // return the attribute with @Id
        return String.valueOf(pk);
    }
}
