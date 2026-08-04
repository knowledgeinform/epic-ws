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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.javers.core.metamodel.annotation.DiffIgnore;

import javax.persistence.*;
import java.util.*;

@Getter
@Setter
//@NoArgsConstructor
//@AllArgsConstructor
@EqualsAndHashCode(of = "pk")
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "step_def")
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        visible = true,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = StepSingleValue.class, name = "SINGLE_VALUE"),
        @JsonSubTypes.Type(value = StepTable.class, name = "TABLE"),
        @JsonSubTypes.Type(value = StepCheckbox.class, name = "CHECKBOX")
})
public abstract class StepDef extends ChangeLogsHolder implements Comparable<StepDef>
{
    @Id
    @Column(name = "pk")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer pk;

    @Column(name = "instructions")
    private String instructions;

    @Column(name = "display_order")
    private Integer displayOrder;

    @ManyToOne
    @JoinColumn(name = "step_group_def_pk", referencedColumnName = "pk", nullable = false, foreignKey = @ForeignKey(name = "fk_stepDef_stepGroupDef"))
    @OrderBy("displayOrder ASC")
    @DiffIgnore
    @JsonIgnoreProperties({"stepDefs"})
    private StepGroupDef stepGroupDef;

    @Column(name = "step_name")
    private String stepName;

    @Column(name = "require_witness")
    private Boolean requireWitness;

    @Column(name = "esd0")
    private Boolean esd0;

    @Column(name = "hazardous")
    private Boolean hazardous;

    @Column(name = "mandatory_inspection")
    private Boolean mandatoryInspection;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private StepType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "edit_type")
    private EditType editType;

    @Column(name = "run_value_saved_timestamp")
    private Date runValueSavedTimestamp;

    @ManyToOne
    @JoinColumn(name = "run_value_entry_user", referencedColumnName = "user_id", foreignKey = @ForeignKey(name = "fk_stepDef_users"))
    private Users runValueEntryUser;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "stepDef", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("timestamp ASC")
    @DiffIgnore
    @JsonIgnoreProperties({"stepDef", "procedureDetails"})
    private SortedSet<History> histories;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "stepDef")
    @OrderBy("commentTimestamp ASC")
    @DiffIgnore
    @JsonIgnoreProperties({"stepGroupDef", "procedureDetails", "stepDef", "procedureInstruction"})
    private List<BlackLineComment> blackLineComments;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "stepDef")
    @OrderBy("commentTimestamp ASC")
    @DiffIgnore
    @JsonIgnoreProperties({"stepGroupDef", "procedureDetails", "stepDef", "procedureInstruction"})
    private List<RedLineComment> redLineComments;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "stepDef")
    @OrderBy("commentTimestamp ASC")
    @DiffIgnore
    @JsonIgnoreProperties({"stepDef"})
    private SortedSet<RunStepComment> runStepComments;

    @Column(name = "manual_validation")
    private Boolean isManualValidation = false;

    @Column(name = "allow_equipment_entry")
    private Boolean allowEquipmentEntry = false;

    @ManyToMany(mappedBy = "steps", fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
    @JsonIgnoreProperties({"run", "steps"})
    private Set<EquipmentList> equipment;

    @OneToOne(fetch = FetchType.EAGER, mappedBy = "stepDef")
    @JsonIgnoreProperties({"stepDef"})
    private WitnessSecondSignature witnessSecondSignature;

    @OneToOne(fetch = FetchType.EAGER, mappedBy = "stepDef")
    @JsonIgnoreProperties({"stepDef"})
    private MandatoryInspectionSecondSignature mandatoryInspectionSecondSignature;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "stepDef", cascade = {CascadeType.ALL})
    @OrderBy("filename ASC")
    @JsonIgnoreProperties({"stepDef"})
    private SortedSet<RunStepAttachment> runStepAttachments;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "stepDef", cascade = {CascadeType.ALL})
    @OrderBy("filename ASC")
    @JsonIgnoreProperties({"stepDef"})
    private SortedSet<StepDefAttachment> stepDefAttachments;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "stepDef")
    @OrderBy("commentTimestamp ASC")
    @DiffIgnore
    @JsonIgnoreProperties({"stepGroupDef", "procedureDetails", "stepDef", "procedureInstruction"})
    private List<RunCloseoutStickyComment> runCloseoutStickyComments;

    @Override
    public int compareTo(StepDef o)
    {
        int ret = Comparator.comparing(StepDef::getDisplayOrder)
                .thenComparing(StepDef::getPk, Comparator.nullsFirst(Integer::compareTo))
                .compare(this, o);
        // This would allow duplicate entries when displayOrder and pk are all the same
        if (ret == 0 && this != o)
        {
            ret = 1;
        }
        return ret;
    }

    @JsonIgnore
    public ProcedureDetails getParentProcedureDetails()
    {
        StepGroupDef parentGroup = this.stepGroupDef;
        while (parentGroup.getStepGroupDefParent() != null)
        {
            parentGroup = parentGroup.getStepGroupDefParent();
        }
        return parentGroup.getProcedureDetails();
    }

    @Override
    @JsonIgnore
    public String getId()
    { // return the attribute marked as @Id
        return String.valueOf(pk);
    }
}
