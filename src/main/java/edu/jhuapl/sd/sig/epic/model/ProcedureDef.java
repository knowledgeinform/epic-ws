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
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*;
import org.javers.core.metamodel.annotation.DiffIgnore;

import javax.persistence.*;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.Comparator;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "pk")
@Entity
@Table(name = "procedure_def")
@Indexed
public class ProcedureDef
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pk")
    private int pk;

    @Column(name = "name", unique = true)
    @FullTextField
    private String name;

    @Column(name = "description")
    @FullTextField
    private String description;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "procedureDef", cascade = {CascadeType.PERSIST})
    @DiffIgnore
    @JsonIgnoreProperties({"procedureDef"})
    @AssociationInverseSide(
            extraction = @ContainerExtraction(BuiltinContainerExtractors.COLLECTION),
            inversePath = @ObjectPath(@PropertyValue(propertyName = "procedureDef")))
    @IndexedEmbedded(includePaths = {
            "id", "status", "editType"
    })
    private Set<ProcedureDetails> procedureDetails;

    @ManyToOne
    @JoinColumn(name = "program_pk", referencedColumnName = "pk", nullable = false, foreignKey = @ForeignKey(name = "fk_procedureDef_program"))
    @IndexedEmbedded
    @JsonIgnoreProperties({"procedureDefs"})
    private Program program;

    @ManyToOne
    @JoinColumn(name = "subsystem_pk", referencedColumnName = "pk", nullable = false, foreignKey = @ForeignKey(name = "fk_procedureDef_subsystem"))
    @IndexedEmbedded
    @JsonIgnoreProperties({"procedureDefs"})
    private Subsystem subsystem;

    /**
     * Returns all revisions for a given procedure definition, sorted by `procedure_def_version`.
     * Returns only definitions; no runs.
     */
    @JsonIgnore
    public SortedSet<ProcedureDetails> getLatestRevisions()
    {

        // Filter out runs.
        Set<ProcedureDetails> pds = this.getProcedureDetails().stream()
                .filter(rev -> rev.getEditType() == EditType.ORIGINAL)
                .collect(Collectors.toSet());

        // Define set ordering
        SortedSet<ProcedureDetails> sortedRevisions = new TreeSet<ProcedureDetails>(new Comparator<ProcedureDetails>()
        {
            @Override
            public int compare(ProcedureDetails pd1, ProcedureDetails pd2)
            {
                return pd1.getProcedureDefVersion() - pd2.getProcedureDefVersion();
            }
        });

        // Add items
        sortedRevisions.addAll(pds);

        return sortedRevisions;
    }
}
