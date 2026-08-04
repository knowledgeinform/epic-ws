/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.runs;

import javax.persistence.TypedQuery;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;

import edu.jhuapl.sd.sig.epic.model.*;

import edu.jhuapl.sd.sig.epic.data.ProgramRolesDAO;
import edu.jhuapl.sd.sig.epic.data.RosterDAO;
import edu.jhuapl.sd.sig.epic.data.SecondSignatureDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.utils.DataGeneratorUtils;
import edu.jhuapl.sd.sig.epic.utils.TestUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

public class TestSecondSignatureDAO
{

    private static EntityManager em = null;
    private static Users commentUser = null;
    private static Users signerUser = null;
    private static ProgramRole signerRole = null;
    private static ProcedureChangeType signerProcedureChangeType = null;
    private static Lorem lorem = LoremIpsum.getInstance();

    @Context
    SecurityContext sc;

    @BeforeAll
    public static void beforeClass()
    {

        TestUtils.init();

        em = JPAUtils.getEntityManager();

    }

    @BeforeEach
    public void beforeEach()
    {
        JPAUtils.closeEntityManager(em);
        em = JPAUtils.getEntityManager();

        // Ensure users exist.
        if (JPAUtils.getAllRecordsForTable(em, Users.class).size() < 3)
        {
            DataGeneratorUtils.generateRandomUsers(2);
        }
        List<Users> users = JPAUtils.getAllRecordsForTable(em, Users.class);
        commentUser = users.get(0);
        signerUser = users.get(1);

        // Ensure role and change type exist.
        if (ProgramRolesDAO.getProgramRoles(em, null).size() < 2)
        {
            DataGeneratorUtils.generateProgramRole();
        }
        signerRole = ProgramRolesDAO.getProgramRoles(em, null).get(0);
        if (ProgramRolesDAO.getProcedureChangeTypes(em, null).size() < 2)
        {
            DataGeneratorUtils.generateProcedureChangeType();
        }
        signerProcedureChangeType = ProgramRolesDAO.getProcedureChangeTypes(em, null).get(0);
        signerRole.setChangeTypesRequiredFor(Collections.singleton(signerProcedureChangeType));
    }

    private BlackRedLineSignature getBlackRedLineSignatureBaseline()
    {
        ProcedureDetails procedureDetails = null;
        while (procedureDetails == null)
        {
            try
            {
                String query = "SELECT p from ProcedureDetails p where p.editType = 'RUN'";
                TypedQuery<ProcedureDetails> procedureDetailsTypedQuery = em.createQuery(query, ProcedureDetails.class);
                procedureDetails = procedureDetailsTypedQuery.getResultList().get(0);
            }
            catch (IndexOutOfBoundsException ie)
            {
                procedureDetails = null;
                try
                {
                    DataGeneratorUtils.generateProcedureDef(1, 1, 1, 1);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    fail("Could not correctly generate a procedure def");
                }
            }
        }

        // Set program roles and change types.
        Program program = procedureDetails.getProcedureDef().getProgram();
        program.setRequiredProgramRoles(Collections.singleton(TestSecondSignatureDAO.signerRole));

        // Create a comment.
        RedLineComment comment = new RedLineComment();
        comment.setUsers(commentUser);
        comment.setCommentText("");
        comment.setCommentType(CommentType.RED_LINE_COMMENT);
        comment.setCommentTimestamp(new Date());
        comment.setProcedureDetails(procedureDetails);
        comment.setProcedureChangeType(signerProcedureChangeType);
        JPAUtils.basicTransaction(em ->
        {
            em.persist(comment);
        }, "Could not create new RedLineComment.");

        // Ensure user has right program roster role.
        RosterEntry re = new RosterEntry(signerUser, program, signerRole);
        List<UserRolesPair> programRoster = RosterDAO.getRosterForProgram(em, program.getPk());
        if (programRoster.stream().noneMatch(pair -> pair.getUser().equals(signerUser) && pair.getRoles().contains(signerRole)))
        {
            RosterDAO.addMemberToRoster(em, re);
        }

        // Create signature.
        BlackRedLineSignature signature = new BlackRedLineSignature();
        signature.setUser(signerUser);
        signature.setProgramRole(TestSecondSignatureDAO.signerRole);
        signature.setComment(comment);

        return signature;
    }

    private void expectSaveBlackRedLineSignatureListFails(BlackRedLineSignature signature)
    {
        this.expectSaveBlackRedLineSignatureListFails(Arrays.asList(signature));
    }

    private void expectSaveBlackRedLineSignatureListFails(List<BlackRedLineSignature> signatures)
    {

        int initialSignatureCount = this.getSignaturesCount();

        assertThrows(Throwable.class, () ->
        {
            SecondSignatureDAO.saveBlackRedLineSignatureList(em, signatures);
        });

        int finalSignatureCount = this.getSignaturesCount();

        assertEquals(initialSignatureCount, finalSignatureCount);

    }

    private int getSignaturesCount()
    {
        EntityManager em = JPAUtils.getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        cq.select(cb.count(cq.from(BlackRedLineSignature.class)));
        int res = em.createQuery(cq).getSingleResult().intValue();
        return res;
    }

    private BlackRedLineSignature expectSaveBlackRedLineSignatureListSucceeds(BlackRedLineSignature signature)
    {
        return this.expectSaveBlackRedLineSignatureListSucceeds(Arrays.asList(signature)).get(0);
    }

    private List<BlackRedLineSignature> expectSaveBlackRedLineSignatureListSucceeds(List<BlackRedLineSignature> signatures)
    {

        try
        {
            signatures = SecondSignatureDAO.saveBlackRedLineSignatureList(em, signatures);
        }
        catch (Exception e)
        {
            Assertions.fail("Error retrieving signature list: " + e.getMessage());
        }

        for (BlackRedLineSignature signature : signatures)
        {
            BlackRedLineSignature dbSignature = JPAUtils.getRecordById(em, BlackRedLineSignature.class, signature.getPk());
            assert (dbSignature.equals(signature));
        }

        return signatures;

    }

    @Test
    public void testSaveBlackRedLineSignatureList_success_newSignatureSaved()
    {
        BlackRedLineSignature signature = this.getBlackRedLineSignatureBaseline();

        this.expectSaveBlackRedLineSignatureListSucceeds(signature);
    }

    @Test
    public void testSaveBlackRedLineSignatureList_success_existingSignatureReplaced()
    {

        int initialSignatureCount = this.getSignaturesCount();

        BlackRedLineSignature signature1 = this.getBlackRedLineSignatureBaseline();
        ((RedBlackLineComment) signature1.getComment()).getProcedureChangeType().setAcceptsAllSignatures(false);
        BlackRedLineSignature dbSignature1 = this.expectSaveBlackRedLineSignatureListSucceeds(signature1);

        RedBlackLineComment comment = (RedBlackLineComment) JPAUtils.getRecordById(em, Comment.class, dbSignature1.getComment().getPk());
        BlackRedLineSignature signature2 = new BlackRedLineSignature();
        signature2.setUser(signerUser);
        signature2.setComment(comment);
        signature2.setProgramRole(signature1.getProgramRole());
        ((RedBlackLineComment) signature2.getComment()).getProcedureChangeType().setAcceptsAllSignatures(false);

        BlackRedLineSignature dbSignature2 = this.expectSaveBlackRedLineSignatureListSucceeds(signature2);

        int finalSignatureCount = this.getSignaturesCount();

        // We added 1 signature and resigned, so expecting initialCount+1.
        assertEquals(initialSignatureCount + 1, finalSignatureCount);

    }

    @Test
    public void testSaveBlackRedLineSignatureList_fail_noSignatureList()
    {
        BlackRedLineSignature signature = null;
        this.expectSaveBlackRedLineSignatureListFails(signature);
    }

    @Test
    public void testSaveBlackRedLineSignatureList_success_emptySignatureList()
    {
        // TODO: Verify expectations for this case. Should the DAO succeed or fail when receiving an empty array? 
        this.expectSaveBlackRedLineSignatureListSucceeds(new ArrayList<BlackRedLineSignature>());
    }

    @Test
    public void testSaveBlackRedLineSignatureList_fail_signerUserNonexistant()
    {

        BlackRedLineSignature signature = this.getBlackRedLineSignatureBaseline();
        signature.setUser(null);

        this.expectSaveBlackRedLineSignatureListFails(signature);

    }

    @Test
    public void testSaveBlackRedLineSignatureList_fail_pinMismatch()
    {

        BlackRedLineSignature signature = this.getBlackRedLineSignatureBaseline();
        signature.getUser().setPin(TestSecondSignatureDAO.signerUser.getPin() + 1);

        this.expectSaveBlackRedLineSignatureListFails(signature);

    }

    @Test
    public void testSaveBlackRedLineSignatureList_fail_noComment()
    {

        BlackRedLineSignature signature = this.getBlackRedLineSignatureBaseline();
        signature.setComment(null);

        this.expectSaveBlackRedLineSignatureListFails(signature);

    }

    @Test
    public void testSaveBlackRedLineSignatureList_fail_roleMismatch()
    {

        BlackRedLineSignature signature = this.getBlackRedLineSignatureBaseline();

        ProgramRole newPr = ProgramRolesDAO.addProgramRole(em, lorem.getWords(2));
        //        em.close();
        signature.setProgramRole(newPr);

        this.expectSaveBlackRedLineSignatureListFails(signature);

    }

}
