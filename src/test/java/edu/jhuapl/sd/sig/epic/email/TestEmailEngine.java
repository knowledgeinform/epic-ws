/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.email;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import edu.jhuapl.sd.sig.epic.startup.EmailEngine;
import edu.jhuapl.sd.sig.epic.utils.TestUtils;
import edu.jhuapl.sd.sig.epic.startup.AppConfiguration;
import org.junit.jupiter.api.Test;

public class TestEmailEngine
{
    /**
     * Used to test execution of the email engine.
     */
    @Test
    public void testEmailEngine()
    {
        Configurator.setRootLevel(Level.ERROR);
        AppConfiguration.loadAppConfiguration();
        TestUtils.init();
        EmailEngine ee = EmailEngine.getInstance();
        ee.checkAndSendEmails();

        // Test each message type.

        // Users testUser = DataGeneratorUtils.getRandomUser();
        // testUser.setEmail("christopher.backofen@jhuapl.edu");

        // ProcedureDef testDef = new ProcedureDef();
        // testDef.setName("Test Def Name");

        // ProcedureDetails testDetails = new ProcedureDetails();
        // testDetails.setProcedureDef(testDef);
        // testDetails.setId("TEST-PROC-ID");
        // testDetails.setProcedureApprovalDueDate(new Date());

        // ProcedureApproval testApproval = new ProcedureApproval();
        // testApproval.setApprovalType(ProcedureApprovalType.APPROVER);
        // testApproval.setProcedureDetails(testDetails);

        // ee.messageTemplatesMap.forEach((type, template) -> {
        // 	System.out.println("Sending test message: " + type);
        // 	ee.sendMessage(type, testUser, testApproval);
        // });

    }
}
