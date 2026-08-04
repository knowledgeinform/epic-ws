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
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Setter
@Getter
@Entity
@Table(name = "run_closeout_comment_reply")
@PrimaryKeyJoinColumn(name = "pk")
public class RunCloseoutCommentReply extends Comment
{

    @ManyToOne
    @JoinColumn(name = "run_closeout_comment_pk", referencedColumnName = "pk", nullable = false, foreignKey = @ForeignKey(name = "fk_runCloseoutCommentReply_runCloseoutComment"))
    @JsonIgnoreProperties("replies")
    private RunCloseoutComment runCloseoutComment;
}
