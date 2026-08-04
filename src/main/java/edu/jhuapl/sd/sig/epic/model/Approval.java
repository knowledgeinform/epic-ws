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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@EqualsAndHashCode(of = "pk")
@MappedSuperclass
public abstract class Approval
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pk")
    private Integer pk;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_type")
    private ProcedureApprovalType approvalType;

    @Column(name = "is_approved")
    private Boolean isApproved;

    @Column(name = "approver_disabled")
    private Boolean approverDisabled;

    @Column(name = "last_reminder_date")
    private Date lastReminderDate;

    @ManyToOne
    @JoinColumn(name = "")
    @JsonIgnoreProperties("procedureDetails")
    private Users users;
}
