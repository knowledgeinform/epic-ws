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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

@Entity
@Getter
@Setter
@Table(name = "program")
public class Program
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pk")
    @GenericField
    private Integer pk;

    @Column(name = "name", unique = true)
    private String name;

    @Column(name = "code", unique = true)
    private String code;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "program_required_roles",
            joinColumns = @JoinColumn(
                    name = "program_pk",
                    referencedColumnName = "pk"),
            inverseJoinColumns = @JoinColumn(
                    name = "program_role_pk",
                    referencedColumnName = "pk"))
    @JsonIgnoreProperties({"blackRedLineSignatures", "changeTypesRequiredFor", "program"})
    private Set<ProgramRole> requiredProgramRoles;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "program")
    @JsonIgnoreProperties("program")
    private Set<ProcedureChangeType> procedureChangeTypes;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "program")
    @JsonIgnoreProperties({"program", "blackRedLineSignatures"})
    private Set<ProgramRole> programRolesForProgram;

    @AssociationInverseSide(
            extraction = @ContainerExtraction(BuiltinContainerExtractors.COLLECTION),
            inversePath = @ObjectPath(@PropertyValue(propertyName = "program")))
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "program")
    @JsonIgnoreProperties({"program"})
    private List<ProcedureDef> procedureDefs;
}
