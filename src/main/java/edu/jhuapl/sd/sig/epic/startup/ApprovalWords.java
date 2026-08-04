/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.startup;

import lombok.Getter;
import lombok.Setter;

/**
 * Defines words that should be used for sending emails. Used by the Email Engine. Needs to be in a separate file so that FreeMarker can access the structure for use in writing email templates.
 */
@Setter
@Getter
public class ApprovalWords
{

    private String userRoleNoun;

    private String userRole;

    private String action;

    private String subjectAction;

    public ApprovalWords(String userRoleNoun, String userRole, String action, String subjectAction)
    {
        this.userRoleNoun = userRoleNoun;
        this.userRole = userRole;
        this.action = action;
        this.subjectAction = subjectAction;
    }

}
