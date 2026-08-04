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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Comparator;

@Getter
@Setter
@EqualsAndHashCode(of = "pk")
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "attachment")
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY)
public abstract class Attachment implements Comparable<Attachment>
{
    @Id
    @Column(name = "pk")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer pk;

    @Column(name = "filename")
    private String filename;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "isImage")
    private Boolean isImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "edit_type")
    private EditType editType;

    @Override
    public int compareTo(Attachment o)
    {

        return Comparator.comparing(Attachment::getFilename)
                .thenComparing(Attachment::getPk, Comparator.nullsFirst(Integer::compareTo))
                .compare(this, o);
    }
}
