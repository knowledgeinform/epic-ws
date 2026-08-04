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

public enum MessageType
{
    NOTIFICATION_PROCEDURE_WAITING,
    NOTIFICATION_PROCEDURE_DRAFT,
    NOTIFICATION_PROCEDURE_REMOVED,
    REMINDER_PROCEDURE_REVIEW_OR_APPROVE,
    NOTIFICATION_RUN_CLOSEOUT_WAITING,
    NOTIFICATION_RUN_CLOSEOUT_RETRACTED,
    NOTIFICATION_RUN_CLOSEOUT_APPROVER_REMOVED,
    REMINDER_RUN_CLOSEOUT_APPROVE,
    NOTIFICATION_RUN_CLOSEOUT_FULLY_APPROVED,
    NOTIFICATION_RUN_CLOSEOUT_CORRECTIONS_NEEDED,
    NOTIFICATION_PROGRAM_EXPORT_READY,
    NOTIFICATION_PROGRAM_EXPORT_FAILED,
}
