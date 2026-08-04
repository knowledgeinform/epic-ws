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

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "black_red_line_signature")
@PrimaryKeyJoinColumn(name = "pk")
public class BlackRedLineSignature extends SecondSignature
{
    @ManyToOne
    @JoinColumn(name = "comment_fk", referencedColumnName = "pk", nullable = false, foreignKey = @ForeignKey(name = "fk_blackRedLineSignature_comment"))
    @JsonIgnoreProperties({"blackRedLineSignatures"})
    private Comment comment;

    @ManyToOne
    @JoinColumn(
            name = "program_role_pk",
            referencedColumnName = "pk",
            foreignKey = @ForeignKey(name = "fk_program_role_pk1"))
    @JsonIgnoreProperties({"blackRedLineSignatures"})
    private ProgramRole programRole;
}
