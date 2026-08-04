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
import edu.jhuapl.sd.sig.epic.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class SecondSignatureDAO
{
    private static final Logger LOGGER = LogManager.getLogger();

    public static StepDef saveRunStepSecondSignature(EntityManager em, RunStepSecondSignature signatureData)
    {
        // first get the step
        StepDef stepDef = em.find(StepDef.class, signatureData.getStepDef().getPk());

        RunStepSecondSignature secondSignature = null;
        if (signatureData.getType().equals(SecondSignatureType.WITNESS))
        {
            secondSignature = new WitnessSecondSignature();
        }
        else if (signatureData.getType().equals(SecondSignatureType.MANDATORY_INSPECTION))
        {
            secondSignature = new MandatoryInspectionSecondSignature();
        }

        try
        {
            em.getTransaction().begin();

            // then get the user
            Users user = UsersDAO.getUserByUsername(em, signatureData.getUser().getUsername());

            if (user == null)
            {
                throw new WebApplicationException("Did not find user with username " + signatureData.getUser().getUsername());
            }
            // validate that the pin matches the user. If doesn't, throw an exception
            if (!user.getPin().equals(signatureData.getUser().getPin()))
            {
                throw new WebApplicationException("Username found, but submitted pin is not a match for this user.");
            }

            RunStepSecondSignature currentSecondSignature = null;
            if (signatureData.getType().equals(SecondSignatureType.WITNESS))
            {
                currentSecondSignature = stepDef.getWitnessSecondSignature();
            }
            else if (signatureData.getType().equals(SecondSignatureType.MANDATORY_INSPECTION))
            {
                currentSecondSignature = stepDef.getMandatoryInspectionSecondSignature();
            }

            // set the field of the secondSignature object
            secondSignature.setStepDef(stepDef);
            secondSignature.setTimestamp(new Date());
            secondSignature.setType(signatureData.getType());
            secondSignature.setUser(user);

            // save the signature
            em.persist(secondSignature);

            if (signatureData.getType().equals(SecondSignatureType.WITNESS))
            {
                stepDef.setWitnessSecondSignature((WitnessSecondSignature) secondSignature);
            }
            else if (signatureData.getType().equals(SecondSignatureType.MANDATORY_INSPECTION))
            {
                stepDef.setMandatoryInspectionSecondSignature((MandatoryInspectionSecondSignature) secondSignature);
            }

            History history;
            if (currentSecondSignature == null)
            {
                history = new History(currentSecondSignature, secondSignature, user);
                history.setStepDef(stepDef);
            }
            else
            {
                history = new History(secondSignature.getTimestamp(), "Removed " + currentSecondSignature.toString() + ", added " + secondSignature.toString(), secondSignature.getUser(), stepDef,
                        null);
            }
            em.persist(history);
            stepDef.getHistories().add(history);

            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            LOGGER.error("Problem while saving run step second signature", e);
            throw new WebApplicationException(e.getMessage(), e);
        }
        return stepDef;
    }

    public static List<BlackRedLineSignature> saveBlackRedLineSignatureList(EntityManager em, List<BlackRedLineSignature> signatureData) throws Exception
    {
        List<BlackRedLineSignature> savedSignatures = new ArrayList<>();
        try
        {
            em.getTransaction().begin();
            for (BlackRedLineSignature signature : signatureData)
            {

                // find the black/red line comment for this signature
                RedBlackLineComment comment = (RedBlackLineComment) JPAUtils.getRecordById(em, Comment.class, signature.getComment().getPk());
                if (comment == null)
                {
                    String message = "Could not find black or red line comment with an id of " + signature.getComment().getPk() + " for this signature.";
                    LOGGER.error(message);
                    throw new WebApplicationException(message);
                }

                // Validate/Get the role.
                if (!comment.getProcedureChangeType().getAcceptsAllSignatures())
                {
                    if (signature.getProgramRole() == null)
                    {
                        String message = "No role provided, and Procedure Change Type does not accept all signatures.";
                        LOGGER.warn(message);
                        throw new WebApplicationException(message);
                    }
                }
                // Check if the program role exists
                ProgramRole role = null;
                if (signature.getProgramRole() != null)
                {
                    role = JPAUtils.getRecordById(em, ProgramRole.class, signature.getProgramRole().getPk());
                }

                // get the user that signed the signature
                Users signer = UsersDAO.getUserByUsername(em, signature.getUser().getUsername());
                // check that user exists
                if (signer == null)
                {
                    String message = "Did not find user with username " + signature.getUser().getUsername();
                    LOGGER.warn(message);
                    throw new WebApplicationException(message);
                }

                // KVF 01/16/2021: Per user feedback on red/black line multiple signatures, removed requirement
                // that signatory not be the author of the red/black line.

                // validate that the pin matches the user. If doesn't, throw an exception
                if (!signer.getPin().equals(signature.getUser().getPin()))
                {
                    String message = "Username " + signer.getUsername() + " found, but submitted pin is not a match for this user";
                    LOGGER.warn(message);
                    throw new WebApplicationException(message);
                }

                // Check for role
                if (role == null && !comment.getProcedureChangeType().getAcceptsAllSignatures())
                {
                    // check that the comment does not accept all signatures.
                    if (!comment.getProcedureChangeType().getAcceptsAllSignatures())
                    {
                        String message = "Could not find role with an id of " + signature.getProgramRole().getPk() + " for this signature.";
                        LOGGER.warn(message);
                        throw new WebApplicationException(message);
                    }
                }

                // Get programPk.
                Integer programPk = null;

                // Validate signer role.
                if (!comment.getProcedureChangeType().getAcceptsAllSignatures() && !role.isBypassValidation())
                {
                    // if here, the signatory must have a matching role on the program
                    if (comment.getProcedureDetails() != null)
                    {
                        programPk = comment.getProcedureDetails().getProcedureDef().getProgram().getPk();
                    }
                    else if (comment.getStepDef() != null)
                    {
                        programPk = comment.getStepDef().getParentProcedureDetails().getProcedureDef().getProgram().getPk();
                    }
                    else if (comment.getStepGroupDef() != null)
                    {
                        programPk = comment.getStepGroupDef().getProcedureDetails().getProcedureDef().getProgram().getPk();
                    }
                    else if (comment.getProcedureInstruction() != null)
                    {
                        programPk = comment.getProcedureInstruction().getProcedureDetails().getProcedureDef().getProgram().getPk();
                    }

                    if (programPk == null)
                    {
                        String message = "Could not determine programPk.";
                        LOGGER.error(message);
                        throw new WebApplicationException(message);
                    }

                    List<UserRolesPair> roster = RosterDAO.getRosterForProgram(em, programPk);
                    Integer rolePk = role.getPk();
                    Boolean userHasRole = roster.stream()
                            .anyMatch(pair ->
                            {
                                boolean signerMatches = pair.getUser().getUserId() == signer.getUserId();
                                boolean roleMatches = pair.getRoles()
                                        .stream()
                                        .anyMatch(r -> r.getPk().equals(rolePk));
                                return signerMatches && roleMatches;
                            });
                    if (!userHasRole)
                    {
                        String message = "Signer does not have the correct program role.";
                        LOGGER.warn(message);
                        throw new WebApplicationException(message);
                    }
                }

                // Replace any pre-existing signatures for role.
                if (role != null)
                {
                    ProgramRole finalRole1 = role;
                    Optional<BlackRedLineSignature> signatureToReplace = comment.getBlackRedLineSignatures().stream().filter(s -> s.getProgramRole().equals(finalRole1)).findFirst();
                    signatureToReplace.ifPresent(s ->
                    {
                        JPAUtils.basicTransaction(emgr ->
                        {
                            emgr.remove(emgr.merge(s));
                        }, "Could not replace signature.");
                    });
                }

                // create a signature object and set its fields
                BlackRedLineSignature lineSignature = new BlackRedLineSignature();
                lineSignature.setComment(comment);
                lineSignature.setTimestamp(new Date());
                lineSignature.setType(SecondSignatureType.BLACK_RED_LINE);
                lineSignature.setUser(signer);
                lineSignature.setProgramRole(role);

                // save the signature
                em.persist(lineSignature);
                comment.getBlackRedLineSignatures().add(lineSignature);
                em.merge(comment);
                savedSignatures.add(lineSignature);
            }
            em.getTransaction().commit();
        }
        catch (WebApplicationException e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            throw new WebApplicationException(e.getMessage(), e);
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }

            LOGGER.error(e.getMessage());
            throw new Exception(e.getMessage(), e);
        }
        return savedSignatures;
    }
}
