/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.resource;

import java.net.InetAddress;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import edu.jhuapl.sd.sig.epic.startup.EmailEngine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Path("/Dev")
public class Dev
{
    private static final Logger LOGGER = LogManager.getLogger();

    @GET
    @Path("/sendProcedureReminders")
    public void sendProcedureReminders()
    {
        EmailEngine ee = EmailEngine.getInstance();
        ee.checkAndSendEmails();
    }

    @GET
    @Path("/getBaseUrl")
    public String getBaseUrl()
    {
        String baseUrl = "";
        try
        {
            InetAddress host = InetAddress.getLocalHost();
            baseUrl = host.getHostName();
        }
        catch (Exception e)
        {}
        return "Base URL: " + baseUrl;
    }

}
