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
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.Date;
import java.util.SortedSet;

@Setter
@Getter
@Entity
@Table(name = "run_approval")
@AssociationOverride(
        name = "users",
        joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_runApproval_users"))})
public class RunApproval extends Approval implements Comparable
{
    @ManyToOne
    @JoinColumn(name = "run_pk", referencedColumnName = "pk", nullable = false, foreignKey = @ForeignKey(name = "fk_runApproval_run"))
    @JsonIgnoreProperties({"runApprovals"})
    private Run run;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "runApproval")
    @OrderBy("commentTimestamp ASC")
    private SortedSet<RunCloseoutComment> comments;

    @Column(name = "approver_order")
    private Integer approverOrder;

    @Column(name = "due_date")
    private Date dueDate;

    @Override
    public int compareTo(Object o)
    {
        return this.approverOrder.compareTo(((RunApproval) o).approverOrder);
    }

    @Override
    public String toString()
    {
        String username = this.getUsers().getUsername();
        String approvalDecisionMessage = "";
        if (getIsApproved() == null)
        {
            approvalDecisionMessage = " has removed their approval decision.";
        }
        else if (getIsApproved())
        {
            approvalDecisionMessage = " has approved the run closeout.";
        }
        else
        {
            approvalDecisionMessage = " has rejected the run closeout.";
        }
        return username + approvalDecisionMessage;
    }
}
