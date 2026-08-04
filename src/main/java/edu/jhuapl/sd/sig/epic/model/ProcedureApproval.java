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
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import lombok.Getter;
import lombok.Setter;
import org.javers.core.metamodel.annotation.DiffIgnore;

import javax.persistence.*;
import java.util.SortedSet;

@Setter
@Getter
@Entity
@Table(name = "procedure_approvals")
@AssociationOverride(
        name = "users",
        joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_procedureApprovals_users"))})
public class ProcedureApproval extends Approval
{
    @ManyToOne
    @JoinColumn(name = "procedure_details_pk", referencedColumnName = "pk", nullable = false, foreignKey = @ForeignKey(name = "fk_procedureApprovals_procedureDetails"))
    @DiffIgnore
    @JsonIgnoreProperties({"procedureApprovals"})
    private ProcedureDetails procedureDetails;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "procedureApproval")
    @OrderBy("commentTimestamp ASC")
    @DiffIgnore
    @JsonIgnoreProperties({"procedureApproval"})
    private SortedSet<ApprovalComment> comments;

    @Override
    public String toString()
    {
        String username = this.getUsers().getDisplayName();
        String approvalDecisionMessage = "";
        if (getIsApproved() == null)
        {
            approvalDecisionMessage = " has removed their approval decision.";
        }
        else if (getIsApproved())
        {
            approvalDecisionMessage = " has approved the procedure revision.";
        }
        else
        {
            approvalDecisionMessage = " has rejected the procedure revision.";
        }
        return getApprovalType().toString() + " " + username + approvalDecisionMessage;
    }
}
