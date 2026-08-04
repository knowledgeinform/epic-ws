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
import org.javers.core.metamodel.annotation.DiffIgnore;

import javax.persistence.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "step_group_def")
public class StepGroupDef extends ChangeLogsHolder implements Comparable<StepGroupDef>
{

    @Id
    @Column(name = "pk")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer pk;

    @Column(name = "step_group_name")
    private String stepGroupName;

    @Column(name = "description")
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder;

    @ManyToOne
    @JoinColumn(name = "procedure_details_pk", referencedColumnName = "pk", nullable = true, foreignKey = @ForeignKey(name = "fk_stepGroupDef_procedureDetails"))
    @JsonIgnoreProperties({"stepGroupDefs", "originalProcedureDetails", "procedureDetails"})
    private ProcedureDetails procedureDetails;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "stepGroupDef", cascade = CascadeType.ALL)
    @OrderBy("displayOrder ASC")
    @JsonIgnoreProperties({"stepGroupDef"})
    private SortedSet<StepDef> stepDefs;

    @ManyToOne
    @JoinColumn(name = "parent_step_group_def_pk", referencedColumnName = "pk", foreignKey = @ForeignKey(name = "fk_stepGroupDef_stepGroupDefParent"))
    @JsonIgnoreProperties("stepGroupDefsChildren")
    private StepGroupDef stepGroupDefParent;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "stepGroupDefParent", cascade = CascadeType.ALL)
    @OrderBy("displayOrder ASC")
    @JsonIgnoreProperties({"stepGroupDefParent"})
    private SortedSet<StepGroupDef> stepGroupDefsChildren;

    @Enumerated(EnumType.STRING)
    @Column(name = "edit_type")
    private EditType editType;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "stepGroupDef")
    @OrderBy("commentTimestamp ASC")
    @DiffIgnore
    @JsonIgnoreProperties({"stepGroupDef", "procedureDetails", "stepDef", "procedureInstruction"})
    private List<BlackLineComment> blackLineComments;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "stepGroupDef")
    @OrderBy("commentTimestamp ASC")
    @DiffIgnore
    @JsonIgnoreProperties({"stepGroupDef", "procedureDetails", "stepDef", "procedureInstruction"})
    private List<RedLineComment> redLineComments;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "stepGroupDef")
    @OrderBy("commentTimestamp ASC")
    @DiffIgnore
    @JsonIgnoreProperties({"stepGroupDef", "procedureDetails", "stepDef", "procedureInstruction"})
    private List<RunCloseoutStickyComment> runCloseoutStickyComments;

    @Override
    public int compareTo(StepGroupDef o)
    {
        int ret = Comparator.comparing(StepGroupDef::getDisplayOrder)
                .thenComparing(StepGroupDef::getPk, Comparator.nullsFirst(Integer::compareTo))
                .compare(this, o);
        // This would allow duplicate entries when displayOrder and pk are all the same
        if (ret == 0 && this != o)
        {
            ret = 1;
        }
        return ret;
    }

    @Override
    public int hashCode()
    {
        if (this.pk != null)
        {
            return this.pk.hashCode();
        }
        else
        {
            return this.getStepGroupName().hashCode() + this.getDescription().hashCode() + this.getDisplayOrder().hashCode();
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        boolean pkMatch = this.pk.equals(((StepGroupDef) obj).getPk());
        boolean nameMatch = this.stepGroupName.equalsIgnoreCase(((StepGroupDef) obj).getStepGroupName());
        boolean displayOrderMatch = this.displayOrder.equals(((StepGroupDef) obj).getDisplayOrder());
        boolean descriptionMatch = this.description.equalsIgnoreCase(((StepGroupDef) obj).getDescription());
        boolean parentGroupMatch = this.stepGroupDefParent != null ? this.stepGroupDefParent
                .equals(((StepGroupDef) obj).getStepGroupDefParent()) : ((StepGroupDef) obj).getStepGroupDefParent() == null;

        return pkMatch && nameMatch && displayOrderMatch && descriptionMatch && parentGroupMatch;
    }

    @JsonIgnore
    public Set<StepDef> getAllSteps()
    {
        Set<StepDef> allSteps = new HashSet<>(this.stepDefs);
        Deque<StepGroupDef> groupsToTraverse = new ArrayDeque<>(this.stepGroupDefsChildren);

        while (!groupsToTraverse.isEmpty())
        {
            StepGroupDef curGroup = groupsToTraverse.pop();
            if (curGroup.getStepGroupDefsChildren() != null)
                groupsToTraverse.addAll(curGroup.getStepGroupDefsChildren());
            if (curGroup.getStepDefs() != null)
                allSteps.addAll(curGroup.getStepDefs());
        }

        return allSteps;
    }

    @JsonIgnore
    public Set<StepGroupDef> getAllChildGroups()
    {
        Set<StepGroupDef> allChildGroups = new HashSet<>(getStepGroupDefsChildren());
        Deque<StepGroupDef> groupsToTraverse = new ArrayDeque<>(getStepGroupDefsChildren());

        while (!groupsToTraverse.isEmpty())
        {
            StepGroupDef curGroup = groupsToTraverse.pop();
            if (curGroup.getStepGroupDefsChildren() != null)
            {
                allChildGroups.addAll(curGroup.getStepGroupDefsChildren());
                groupsToTraverse.addAll(curGroup.getStepGroupDefsChildren());
            }
        }
        return allChildGroups;
    }

    @JsonIgnore
    @Override
    public String getId()
    { // return the attribute marked as @Id
        return String.valueOf(pk);
    }
}
