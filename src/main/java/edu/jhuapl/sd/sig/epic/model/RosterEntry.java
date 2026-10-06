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

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.ForeignKey;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "roster")
@IdClass(RosterEntry.class)
public class RosterEntry implements Serializable
{

    private static final long serialVersionUID = 7733149511663697882L;

    @Id
    @ManyToOne
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "user_id",
            foreignKey = @ForeignKey(name = "fk_user_id"))
    private Users user;

    @Id
    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(
            name = "program_pk",
            referencedColumnName = "pk",
            foreignKey = @ForeignKey(name = "fk_program_pk"))
    private Program program;

    @Id
    @ManyToOne
    @JoinColumn(
            name = "role_pk",
            referencedColumnName = "pk",
            foreignKey = @ForeignKey(name = "fk_role_pk"))
    private ProgramRole role;

}
