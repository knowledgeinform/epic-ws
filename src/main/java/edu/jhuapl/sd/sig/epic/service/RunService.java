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

import edu.jhuapl.sd.sig.epic.data.RunDAO;
import edu.jhuapl.sd.sig.epic.data.UsersDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.Run;
import edu.jhuapl.sd.sig.epic.model.Users;
import edu.jhuapl.sd.sig.epic.model.ValidationResult;
import edu.jhuapl.sd.sig.epic.model.display.dto.RunEditDTO;
import edu.jhuapl.sd.sig.epic.model.display.dto.RunEditResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;

/**
 * Class for handling editing of run details (currently, name and description).
 */
public class RunService
{
    private static final Logger LOGGER = LogManager.getLogger(RunService.class.getName());
    private final RunEditabilityValidator editabilityValidator = new RunEditabilityValidator();

    /**
     * Update run name and/or description.
     *
     * @param editDTO The edit data containing runPk, name, and description
     * @param username The user performing the edit
     * @return RunEditResult indicating success/failure and previous values
     */
    public RunEditResult updateRunMetadata(RunEditDTO editDTO, String username)
    {
        if (editDTO == null || editDTO.getRunPk() == null)
        {
            return new RunEditResult(false, "Run PK is required", null, null, null);
        }

        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            em.getTransaction().begin();

            Users user = UsersDAO.getUserByUsername(em, username);

            // Load the run
            Run run = RunDAO.getRunByPk(em, editDTO.getRunPk());
            if (run == null)
            {
                em.getTransaction().rollback();
                return new RunEditResult(false, "Run not found", null, null, null);
            }

            // Determine what fields are being edited
            boolean editName = editDTO.getName() != null && !editDTO.getName().equals(run.getName());
            boolean editDescription = editDTO.getDescription() != null && !editDTO.getDescription().equals(run.getDescription());

            if (!editName && !editDescription)
            {
                em.getTransaction().rollback();
                return new RunEditResult(false, "No changes detected", run.getName(), run.getDescription(), run);
            }

            // Validate editability for name
            if (editName)
            {
                ValidationResult nameValidation = editabilityValidator.canEditRunName(run, user);
                if (!nameValidation.isAllowed())
                {
                    em.getTransaction().rollback();
                    return new RunEditResult(false, nameValidation.getDenialReason(), run.getName(), run.getDescription(), run);
                }

                // Check name uniqueness (excluding current run)
                if (!RunDAO.isRunNameUniqueExcludingRun(em, editDTO.getName(), run.getPk()))
                {
                    em.getTransaction().rollback();
                    return new RunEditResult(false, "A run with this name already exists", run.getName(), run.getDescription(), run);
                }
            }

            // Validate editability for description
            if (editDescription)
            {
                ValidationResult descValidation = editabilityValidator.canEditRunDescription(run, user);
                if (!descValidation.isAllowed())
                {
                    em.getTransaction().rollback();
                    return new RunEditResult(false, descValidation.getDenialReason(), run.getName(), run.getDescription(), run);
                }
            }

            // Capture previous values
            String previousName = run.getName();
            String previousDescription = run.getDescription();

            // Update fields
            if (editName)
            {
                run.setName(editDTO.getName());
            }
            if (editDescription)
            {
                run.setDescription(editDTO.getDescription());
            }

            // Save run
            run = em.merge(run);

            // Create History entry
            String historyMessage = buildHistoryMessage(previousName, previousDescription, run.getName(), run.getDescription());
            run.getProcedureDetails().getHistories().add(
                    new edu.jhuapl.sd.sig.epic.model.History(new java.util.Date(), historyMessage, user, null, run.getProcedureDetails()));

            em.getTransaction().commit();

            // Build a specific success message reflecting what was changed
            String successMessage = buildSuccessMessage(editName, editDescription, previousName, previousDescription, run.getName(), run.getDescription());

            LOGGER.info("Successfully updated run metadata for run pk " + run.getPk());
            return new RunEditResult(true, successMessage, previousName, previousDescription, run);

        }
        catch (Exception e)
        {
            if (em != null && em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            LOGGER.error("Error updating run metadata", e);
            return new RunEditResult(false, "Error updating run metadata: " + e.getMessage(), null, null, null);
        }
        finally
        {
            if (em != null && em.isOpen())
            {
                em.close();
            }
        }
    }

    public ValidationResult validateEdit(Run run, Users user, boolean editName, boolean editDescription)
    {
        if (editName && editDescription)
        {
            ValidationResult nameValidation = editabilityValidator.canEditRunName(run, user);
            if (!nameValidation.isAllowed())
            {
                return nameValidation;
            }
            return editabilityValidator.canEditRunDescription(run, user);
        }
        else if (editName)
        {
            return editabilityValidator.canEditRunName(run, user);
        }
        else if (editDescription)
        {
            return editabilityValidator.canEditRunDescription(run, user);
        }
        return new ValidationResult(false, "No fields specified for editing", false);
    }

    /**
     * Build a human-readable history message for the metadata change.
     */
    private String buildHistoryMessage(String oldName, String oldDesc, String newName, String newDesc)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Run metadata updated");

        if (oldName != null && newName != null && !oldName.equals(newName))
        {
            sb.append("; name: '").append(oldName).append("' → '").append(newName).append("'");
        }

        if (oldDesc != null && newDesc != null && !oldDesc.equals(newDesc))
        {
            sb.append("; description: '").append(oldDesc).append("' → '").append(newDesc).append("'");
        }

        return sb.toString();
    }

    /**
     * Build a specific success message reflecting what was actually changed.
     * This message is returned to the frontend and displayed in the toast notification.
     */
    private String buildSuccessMessage(boolean editName, boolean editDescription,
            String oldName, String oldDesc,
            String newName, String newDesc)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Run ");

        if (editName && editDescription)
        {
            sb.append("name and description");
        }
        else if (editName)
        {
            sb.append("name");
        }
        else
        {
            sb.append("description");
        }

        sb.append(" updated successfully");

        return sb.toString();
    }
}
