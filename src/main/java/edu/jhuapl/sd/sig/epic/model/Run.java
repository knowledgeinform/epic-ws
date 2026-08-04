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
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;
import java.util.SortedSet;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "pk")
@Entity
@Table(name = "run")
//@JsonIdentityInfo(
//	generator = ObjectIdGenerators.PropertyGenerator.class,
//	property = "pk"
//)
@Indexed
public class Run
{
    @Id
    @Column(name = "pk")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer pk;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RunStatus status;

    @ManyToOne
    @JoinColumn(name = "author", referencedColumnName = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_run_users"))
    @JsonIgnoreProperties({"procedureDetails"})
    private Users user;

    @Column(name = "name", nullable = false)
    @FullTextField
    private String name;

    @Column(name = "description")
    @FullTextField
    private String description;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"run", "steps"})
    private Set<EquipmentList> equipmentList;

    @OneToOne
    @JoinColumn(name = "procedure_details_pk", referencedColumnName = "pk", nullable = false, foreignKey = @ForeignKey(name = "fk_run_procedureDetails"))
    @JsonIgnoreProperties({"run"})
    @AssociationInverseSide(
            extraction = @ContainerExtraction(BuiltinContainerExtractors.COLLECTION),
            inversePath = @ObjectPath(@PropertyValue(propertyName = "run")))
    private ProcedureDetails procedureDetails;

    @JsonIgnoreProperties("runs")
    @ManyToOne
    @JoinColumn(name = "testing_phase_fk", referencedColumnName = "pk", nullable = false, foreignKey = @ForeignKey(name = "fk_run_testingPhase"))
    @IndexedEmbedded
    private TestingPhase testingPhase;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "run", cascade = {CascadeType.ALL})
    @OrderBy("filename ASC")
    @JsonIgnoreProperties({"run"})
    private SortedSet<RunAttachment> attachments;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "run")
    @OrderBy("approverOrder ASC")
    @JsonIgnoreProperties({"run"})
    private SortedSet<RunApproval> runApprovals;

    @Column(name = "created_date")
    private Date createdDate;

    @Column(name = "closeout_submitted_date")
    private Date closeoutSubmittedDate;

    @Column(name = "closeout_completed_date")
    private Date closeoutCompletedDate;

    @ManyToOne
    @JoinColumn(name = "closeout_submitter", referencedColumnName = "user_id", foreignKey = @ForeignKey(name = "fk_run_users_closeout_submitter"))
    @JsonIgnoreProperties({"procedureDetails"})
    private Users closeoutSubmissionUser;
}
