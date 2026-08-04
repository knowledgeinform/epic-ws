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
import java.util.Date;
import java.util.SortedSet;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "procedure_header")
public class ProcedureHeader
{
    @Id
    @Column(name = "pk")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer pk;

    @Column(name = "text")
    private String text;

    @ManyToOne
    @JoinColumn(name = "author", referencedColumnName = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_procedureHeader_users"))
    @JsonIgnoreProperties({"procedureDetails"})
    private Users user;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_date")
    private Date creationDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "submitted_for_review_date")
    private Date submittedForReviewDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "approved_date")
    private Date approvedDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "released_date")
    private Date releasedDate;

    @OneToOne
    @JoinColumn(name = "procedure_details_pk", referencedColumnName = "pk", nullable = false, foreignKey = @ForeignKey(name = "fk_procedureHeader_procedureDetails"))
    @DiffIgnore
    @JsonIgnoreProperties({"procedureHeader"})
    private ProcedureDetails procedureDetails;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "procedureHeader", cascade = {CascadeType.ALL})
    @OrderBy("filename ASC")
    @JsonIgnoreProperties({"procedureHeader"})
    private SortedSet<ProcedureAttachment> attachments;

}
