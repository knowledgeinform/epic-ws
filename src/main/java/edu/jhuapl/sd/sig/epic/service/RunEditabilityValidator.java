/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.service;

import edu.jhuapl.sd.sig.epic.model.Run;
import edu.jhuapl.sd.sig.epic.model.RunStatus;
import edu.jhuapl.sd.sig.epic.model.Users;
import edu.jhuapl.sd.sig.epic.model.ValidationResult;

/**
 * Encapsulates business rules for WHEN and WHO can edit run name/description.
 */
public class RunEditabilityValidator
{

    /**
     * Check if user can edit run name based on run status and user role.
     * 
     * @param run The run being edited
     * @param user The user attempting to edit
     * @return ValidationResult indicating if edit is allowed
     */
    public ValidationResult canEditRunName(Run run, Users user)
    {
        return validateEdit(run, user, "name");
    }

    /**
     * Check if user can edit run description based on run status and user role.
     * 
     * @param run The run being edited
     * @param user The user attempting to edit
     * @return ValidationResult indicating if edit is allowed
     */
    public ValidationResult canEditRunDescription(Run run, Users user)
    {
        return validateEdit(run, user, "description");
    }

    /**
     * Core validation logic for run name/description editability.
     * Shared by canEditRunName() and canEditRunDescription() to avoid duplication.
     * 
     * @param run The run being edited
     * @param user The user attempting to edit
     * @param fieldName The field name for denial messages ("name" or "description")
     * @return ValidationResult indicating if edit is allowed
     */
    private ValidationResult validateEdit(Run run, Users user, String fieldName)
    {
        if (run == null || run.getStatus() == null)
        {
            return new ValidationResult(false, "Run or status is null", false);
        }

        RunStatus status = run.getStatus();
        boolean isCreator = run.getUser() != null && (user.getUserId() == run.getUser().getUserId());
        boolean isAdmin = user.getIsAdmin() != null && user.getIsAdmin();

        switch (status)
        {
            case RUNNING:
                if (isCreator || isAdmin)
                {
                    return new ValidationResult(true, null, false);
                }
                return new ValidationResult(false, "Only the run creator or an admin can edit the run " + fieldName + " in RUNNING status", false);

            case REVIEWING:
                if (isAdmin)
                {
                    return new ValidationResult(true, null, false);
                }
                return new ValidationResult(false, "Only an admin can edit the run " + fieldName + " in REVIEWING status", false);

            case CORRECTING:
                if (isCreator || isAdmin)
                {
                    return new ValidationResult(true, null, false);
                }
                return new ValidationResult(false, "Only the run creator or an admin can edit the run " + fieldName + " in CORRECTING status", false);

            case APPROVED:
            case COMPLETED:
            case ABANDONED:
                return new ValidationResult(false, "Cannot edit run " + fieldName + " in " + status + " status. Edits are not permitted.", false);

            default:
                return new ValidationResult(false, "Unknown run status: " + status, false);
        }
    }
}
