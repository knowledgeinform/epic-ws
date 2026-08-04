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

import java.util.Set;
import java.util.SortedSet;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "procedure_change_types")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcedureChangeType implements Deletable
{

    @Id
    @Column(name = "pk")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer pk;

    @Column(name = "name")
    private String name;

    @Column(name = "is_enabled")
    private Boolean isEnabled;

    @Column(name = "accepts_all_signatures")
    private Boolean acceptsAllSignatures;

    @Column(name = "description")
    private String description;

    @ManyToOne
    @JoinColumn(name = "program_fk", referencedColumnName = "pk", foreignKey = @ForeignKey(name = "fk_procedureChangeTypes_program"))
    @JsonIgnoreProperties({"requiredProgramRoles", "procedureChangeTypes", "programRolesForProgram"})
    @JsonIgnore
    private Program program;

    @Transient
    private Integer programPk;

    @Fetch(FetchMode.SUBSELECT)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "program_role_to_change_type",
            joinColumns = @JoinColumn(
                    name = "procedure_change_type_pk",
                    referencedColumnName = "pk"),
            inverseJoinColumns = @JoinColumn(
                    name = "program_role_pk",
                    referencedColumnName = "pk"))
    @JsonIgnoreProperties({"changeTypesRequiredFor", "blackRedLineSignatures"})
    private Set<ProgramRole> requiredRoleApprovals;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "procedureChangeType")
    @OrderBy("commentTimestamp ASC")
    @JsonIgnore()
    private SortedSet<RedLineComment> redLineComments;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "procedureChangeType")
    @OrderBy("commentTimestamp ASC")
    @JsonIgnore()
    private SortedSet<BlackLineComment> blackLineComments;

    public boolean getDeletable()
    {
        return (this.blackLineComments == null || this.blackLineComments.isEmpty()) &&
                (this.redLineComments == null || this.redLineComments.isEmpty());
    }

    @JsonProperty
    public void setProgramPk(Integer programPk)
    {
        this.programPk = programPk;
    }

    @JsonProperty
    public Integer getProgramPk()
    {
        return this.programPk;
    }
}
