/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.data;

import edu.jhuapl.sd.sig.epic.model.BlackLineComment;
import edu.jhuapl.sd.sig.epic.model.StepDef;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.WebApplicationException;

public class BlackLineDAO
{

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * This method saves a black line. If the stepManualValidation flag = true, this means this is a black line for a
     * step in which the user is manually validating the step (setting it to done). The manual validation flag needs to
     * be updated to true on the corresponding StepDef in this case.
     * 
     * @param em
     * @param blackLineComment
     * @param stepManualValidation
     * @return
     */
    public static BlackLineComment saveBlackLine(EntityManager em, BlackLineComment blackLineComment, boolean stepManualValidation)
    {
        try
        {
            em.getTransaction().begin();
            if (blackLineComment.getStepDef() != null)
            {
                StepDef stepDef = blackLineComment.getStepDef();
                stepDef.setIsManualValidation(stepManualValidation);
                blackLineComment.setStepDef(em.merge(stepDef));
            }

            blackLineComment = em.merge(blackLineComment);
            em.getTransaction().commit();
            return blackLineComment;
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String msg = "Could not save the black line comment";
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
    }
}
