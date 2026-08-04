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

import java.util.*;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.hibernate.annotations.*;

import edu.jhuapl.sd.sig.epic.data.AppConfigurationDAO;
import edu.jhuapl.sd.sig.epic.startup.AppConfiguration;
import edu.jhuapl.sd.sig.epic.startup.AppConfiguration.AppConfigKey;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*;
import org.javers.core.metamodel.annotation.DiffIgnore;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "pk")
@Entity
@org.javers.core.metamodel.annotation.Entity
@Table(name = "procedure_details", uniqueConstraints = {@UniqueConstraint(name = "version_procedure_run_UNIQUE", columnNames = {"run_number", "procedure_def_pk", "procedure_def_version"})})
@JsonIgnoreProperties(ignoreUnknown = true)
@Indexed
public class ProcedureDetails
{
    @Id
    @Column(name = "pk")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int pk;

    @Column(name = "procedure_def_version")
    private int procedureDefVersion;

    @Column(name = "id", unique = true)
    @FullTextField(analyzer = AnalyzerNames.STANDARD)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @GenericField
    private ProcedureStatus status;

    @OneToOne(fetch = FetchType.EAGER, mappedBy = "procedureDetails")
    @JsonIgnoreProperties({"procedureDetails"})
    @IndexedEmbedded
    private Run run;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "procedureDetails")
    @OrderBy("displayOrder ASC")
    @DiffIgnore
    @JsonIgnoreProperties({"procedureDetails"})
    private SortedSet<StepGroupDef> stepGroupDefs;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "procedureDetails")
    @OrderBy("displayOrder ASC")
    @DiffIgnore
    @JsonIgnoreProperties({"procedureDetails"})
    private SortedSet<ProcedureInstruction> procedureInstructions;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "procedureDetails")
    @DiffIgnore
    @JsonIgnoreProperties({"procedureDetails"})
    private Set<ProcedureApproval> procedureApprovals;

    @OneToOne(mappedBy = "procedureDetails", cascade = {CascadeType.PERSIST})
    @DiffIgnore
    @JsonIgnoreProperties({"procedureDetails"})
    private ProcedureHeader procedureHeader;

    @ManyToOne
    @JoinColumn(name = "procedure_def_pk", referencedColumnName = "pk", nullable = false, foreignKey = @ForeignKey(name = "fk_procedureDetails_procedureDef"))
    @DiffIgnore
    @JsonIgnoreProperties({"procedureDetails"})
    @IndexedEmbedded
    @AssociationInverseSide(
            extraction = @ContainerExtraction(BuiltinContainerExtractors.COLLECTION),
            inversePath = @ObjectPath(@PropertyValue(propertyName = "procedureDetails")))
    private ProcedureDef procedureDef;

    @ManyToOne
    @JoinColumn(name = "original_procedure_detail", referencedColumnName = "pk", foreignKey = @ForeignKey(name = "fk_procedureDetail_originalProcedureDetail"))
    @DiffIgnore
    @JsonIgnoreProperties({"procedureDetailRuns"})
    private ProcedureDetails originalProcedureDetails;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "originalProcedureDetails")
    @LazyCollection(LazyCollectionOption.EXTRA)
    @DiffIgnore
    @JsonIgnoreProperties({"originalProcedureDetails"})
    private List<ProcedureDetails> procedureDetailRuns;

    @Column(name = "redlined_version")
    private String redlinedVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "edit_type")
    @GenericField
    private EditType editType;

    @Column(name = "run_number")
    private Integer runNumber;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "procedureDetails")
    @OrderBy("commentTimestamp ASC")
    @DiffIgnore
    @JsonIgnoreProperties({"stepGroupDef", "procedureDetails", "stepDef", "procedureInstruction"})
    private List<BlackLineComment> blackLineComments;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "procedureDetails")
    @OrderBy("commentTimestamp ASC")
    @DiffIgnore
    @JsonIgnoreProperties({"stepGroupDef", "procedureDetails", "stepDef", "procedureInstruction"})
    private List<RedLineComment> redLineComments;

    @Column(name = "hazardous")
    private Boolean hazardous;

    @Column(name = "esd0")
    private Boolean esd0;

    @Column(name = "hazard_description")
    private String hazardDescription;

    @Column(name = "procedure_approval_due_date")
    private Date procedureApprovalDueDate;

    @Fetch(FetchMode.SUBSELECT)
    @ManyToMany(mappedBy = "procedureDetails", fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
    @DiffIgnore
    @JsonIgnoreProperties({"procedureDetails"})
    private Set<Users> favoriteUsers = new HashSet<>();

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "procedureDetails")
    @OrderBy("commentTimestamp ASC")
    @DiffIgnore
    @JsonIgnoreProperties({"stepGroupDef", "procedureDetails", "stepDef", "procedureInstruction"})
    private List<RunCloseoutStickyComment> runCloseoutStickyComments;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "procedureDetails", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("timestamp ASC")
    @DiffIgnore
    @JsonIgnoreProperties({"procedureDetails", "stepDef"})
    private SortedSet<History> histories = new TreeSet<>();

    @JsonIgnore
    public String getProcedureUrl()
    {
        String baseUrl = AppConfiguration.getConfigValue(AppConfigKey.ALLOWED_ORIGIN);
        String contextRoot = AppConfigurationDAO.getConfigForKey(ConfigKey.APP_CONTEXT_ROOT);
        return baseUrl + contextRoot + "/procedure/" + this.getId();
    }

    @JsonIgnore
    public String getRunUrl()
    {
        String baseUrl = AppConfiguration.getConfigValue(AppConfigKey.ALLOWED_ORIGIN);
        String contextRoot = AppConfigurationDAO.getConfigForKey(ConfigKey.APP_CONTEXT_ROOT);
        return baseUrl + contextRoot + "/run/" + this.getId();
    }

    @JsonIgnore
    public Set<StepDef> getAllSteps()
    {
        Set<StepDef> allSteps = new HashSet<>();
        Deque<StepGroupDef> groupsToTraverse = new ArrayDeque<>(this.stepGroupDefs);

        while (!groupsToTraverse.isEmpty())
        {
            StepGroupDef curGroup = groupsToTraverse.pop();
            groupsToTraverse.addAll(curGroup.getStepGroupDefsChildren());
            allSteps.addAll(curGroup.getStepDefs());
        }

        return allSteps;
    }

    @Override
    public String toString()
    {
        String procedureDetailsType;
        String runNumberString = null;
        if (this.run == null)
        {
            procedureDetailsType = "procedure revision";
        }
        else
        {
            procedureDetailsType = "run";
            runNumberString = ", run: " + this.runNumber;
        }
        String addRunNumberString = runNumberString != null ? runNumberString : "";
        return procedureDetailsType + " [id: " + this.id + ", procedure name: " + this.procedureDef.getName() + ", procedure revision: " + this.procedureDefVersion + addRunNumberString + "]";
    }
}
