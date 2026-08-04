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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;
import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "pk")
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "comment")
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        visible = true,
        property = "commentType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ApprovalComment.class, name = "APPROVAL_COMMENT"),
        @JsonSubTypes.Type(value = ApprovalCommentReply.class, name = "APPROVAL_COMMENT_REPLY"),
        @JsonSubTypes.Type(value = RunStepComment.class, name = "RUN_STEP_COMMENT"),
        @JsonSubTypes.Type(value = BlackLineComment.class, name = "BLACK_LINE_COMMENT"),
        @JsonSubTypes.Type(value = RedLineComment.class, name = "RED_LINE_COMMENT"),
        @JsonSubTypes.Type(value = RunCloseoutComment.class, name = "RUN_CLOSEOUT_COMMENT"),
        @JsonSubTypes.Type(value = RunCloseoutCommentReply.class, name = "RUN_CLOSEOUT_COMMENT_REPLY"),
        @JsonSubTypes.Type(value = RunCloseoutStickyComment.class, name = "RUN_CLOSEOUT_STICKY_COMMENT"),
})
public abstract class Comment implements Comparable
{

    @Id
    @Column(name = "pk")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer pk;

    @Column(name = "comment_timestamp")
    private Date commentTimestamp;

    @Column(name = "comment_text")
    private String commentText;

    @Enumerated(EnumType.STRING)
    @Column(name = "comment_type")
    private CommentType commentType;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_comment_users"))
    @JsonIgnoreProperties("procedureDetails")
    private Users users;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "comment")
    @JsonIgnoreProperties({"comment"})
    private Set<BlackRedLineSignature> blackRedLineSignatures;

    @Override
    public int compareTo(Object o)
    {
        return this.commentTimestamp.compareTo(((Comment) o).commentTimestamp);
    }

    public Comment(Date commentTimestamp, String commentText, CommentType commentType, Users users)
    {
        this.commentTimestamp = commentTimestamp;
        this.commentText = commentText;
        this.commentType = commentType;
        this.users = users;
    }
}
