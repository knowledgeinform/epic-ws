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
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "pk")
@Entity
@Table(name = "testing_phase")
public class TestingPhase
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pk")
    @GenericField
    private Integer pk;

    @Column(name = "name", unique = true)
    private String name;

    @Column(name = "short_name", unique = true)
    private String shortName;

    @Column(name = "code", unique = true)
    private String code;

    @JsonIgnoreProperties("testingPhase")
    @AssociationInverseSide(
            extraction = @ContainerExtraction(BuiltinContainerExtractors.COLLECTION),
            inversePath = @ObjectPath(@PropertyValue(propertyName = "testingPhase")))
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "testingPhase")
    private List<Run> runs;

}
