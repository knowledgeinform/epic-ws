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

import java.util.List;
import java.util.Set;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "program_roles")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProgramRole implements Deletable
{

    @Id
    @Column(name = "pk")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer pk;

    @Column(name = "name")
    private String name;

    @Column(name = "bypass_validation")
    private boolean bypassValidation;

    @ManyToOne
    @JoinColumn(name = "program_fk", referencedColumnName = "pk", foreignKey = @ForeignKey(name = "fk_programRoles_program"))
    @JsonIgnoreProperties("requiredProgramRoles")
    @JsonIgnore
    private Program program;

    @Transient
    private Integer programPk;

    @Fetch(FetchMode.SUBSELECT)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "program_role_to_change_type",
            joinColumns = @JoinColumn(
                    name = "program_role_pk",
                    referencedColumnName = "pk"),
            inverseJoinColumns = @JoinColumn(
                    name = "procedure_change_type_pk",
                    referencedColumnName = "pk"))
    @JsonIgnoreProperties("requiredRoleApprovals")
    private Set<ProcedureChangeType> changeTypesRequiredFor;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "programRole")
    @JsonIgnoreProperties({"programRole"})
    @JsonIgnore
    private List<BlackRedLineSignature> blackRedLineSignatures;

    public boolean getDeletable()
    {
        return this.blackRedLineSignatures == null || this.blackRedLineSignatures.isEmpty();
    }

    @JsonProperty
    public void setProgramPk(Integer programPk)
    {
        this.programPk = programPk;
    }

    @JsonProperty
    public Integer getProgramPk()
    {
        if (this.programPk != null)
            return this.programPk;
        if (this.program != null)
            return program.getPk();
        return null;
    }
}
