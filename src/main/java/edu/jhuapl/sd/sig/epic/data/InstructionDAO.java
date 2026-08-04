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

import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.EditType;
import edu.jhuapl.sd.sig.epic.model.ProcedureDetails;
import edu.jhuapl.sd.sig.epic.model.ProcedureInstruction;
import edu.jhuapl.sd.sig.epic.model.RedLineComment;
import edu.jhuapl.sd.sig.epic.model.util.CopyUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.WebApplicationException;
import java.util.*;

public class InstructionDAO
{

    private static final Logger LOGGER = LogManager.getLogger();

    public static SortedSet<ProcedureInstruction> getInstructionSections(EntityManager em, Integer proc_def_ver_id)
    {
        SortedSet<ProcedureInstruction> sections;
        ProcedureDetails pdv = JPAUtils.getRecordById(em, ProcedureDetails.class, proc_def_ver_id);
        sections = pdv.getProcedureInstructions();

        return sections;
    }

    public static ProcedureInstruction insertNewInstructionSection(EntityManager em, String section_name, Integer display_order, Integer proc_def_ver_id)
    {
        ProcedureInstruction pi = new ProcedureInstruction();
        try
        {

            em.getTransaction().begin();
            ProcedureDetails pdv = JPAUtils.getRecordById(em, ProcedureDetails.class, proc_def_ver_id);

            //move all pi's with display order after the new order down by 1
            for (ProcedureInstruction inst : pdv.getProcedureInstructions())
            {
                if (inst.getDisplayOrder() >= display_order)
                {
                    inst.setDisplayOrder(inst.getDisplayOrder() + 1);
                    em.merge(inst);
                }
            }
            pi.setSectionName(section_name);
            pi.setDisplayOrder(display_order);
            pi.setEditType(EditType.ORIGINAL);

            pi.setProcedureDetails(pdv);
            em.persist(pi);

            SortedSet<ProcedureInstruction> instructions = pdv.getProcedureInstructions();
            instructions.add(pi);
            pdv.setProcedureInstructions(instructions);
            em.merge(pdv);

            em.getTransaction().commit();
            //refreshing entitymanager persistence context for the proc dev version
            //			em.refresh(pdv);
        }
        catch (Exception e)
        {
            LOGGER.error("Error inserting new instruction section into db", e);
            throw e;
        }

        return pi;
    }

    public static ProcedureInstruction updateInstructionSection(EntityManager em, Integer section_id, String name, String section_text,
            Integer display_order)
    {
        ProcedureInstruction pi;

        try
        {

            em.getTransaction().begin();

            pi = JPAUtils.getRecordById(em, ProcedureInstruction.class, section_id);
            pi.setSectionName(name);
            pi.setText(section_text);
            pi.setDisplayOrder(display_order);

            em.merge(pi);

            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            pi = null;
            LOGGER.error("Error updating instruction section into db", e);
        }

        return pi;

    }

    public static ProcedureInstruction deleteInstructionSection(EntityManager em, Integer section_id)
    {
        ProcedureInstruction section = JPAUtils.getRecordById(em, ProcedureInstruction.class, section_id);

        try
        {
            em.getTransaction().begin();
            em.remove(section);
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            LOGGER.error("Error removing instruction section " + section_id + " from db");
        }

        return section;
    }

    public static ProcedureInstruction copyInstruction(EntityManager em, Integer instructionPk, EditType editTypeForNewInstruction, RedLineComment redLineComment)
    {
        ProcedureInstruction instructionToCopy = JPAUtils.getRecordById(em, ProcedureInstruction.class, instructionPk);
        ProcedureDetails targetPd = instructionToCopy.getProcedureDetails();
        return copyInstructionsToProcedure(em, new TreeSet<>(Arrays.asList(instructionToCopy)), targetPd, editTypeForNewInstruction, redLineComment).first();
    }

    public static SortedSet<ProcedureInstruction> copyInstructionsToProcedure(EntityManager em, SortedSet<ProcedureInstruction> instructionsToCopy,
            ProcedureDetails targetPd, EditType editTypeForNewInstructions,
            RedLineComment redLineComment)
    {
        try
        {
            em.getTransaction().begin();
            SortedSet<ProcedureInstruction> newInstructions = CopyUtils.copyProcedureInstructions(instructionsToCopy, targetPd, editTypeForNewInstructions, false);
            for (ProcedureInstruction newPI : newInstructions)
            {
                newPI.setDisplayOrder(targetPd.getProcedureInstructions().size() + 1);
                targetPd.getProcedureInstructions().add(newPI);
                em.persist(newPI);

                if (redLineComment != null)
                {
                    // the red line comment might be applied multiple times (to each new instruction)
                    RedLineComment newComment = new RedLineComment(redLineComment.getCommentTimestamp(), redLineComment.getCommentText(),
                            redLineComment.getCommentType(), redLineComment.getUsers());
                    newComment.setProcedureChangeType(redLineComment.getProcedureChangeType());
                    newComment.setProcedureDetails(targetPd);
                    newComment.setProcedureInstruction(newPI);
                    em.persist(newComment);
                    List<RedLineComment> redLineComments = newPI.getRedLineComments();
                    if (redLineComments == null)
                    {
                        redLineComments = new ArrayList<>();
                    }
                    redLineComments.add(newComment);
                    newPI.setRedLineComments(redLineComments);
                }
            }

            em.merge(targetPd);
            em.getTransaction().commit();
            return newInstructions;
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String msg = "Could not copy instruction sections!";
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
    }
}
