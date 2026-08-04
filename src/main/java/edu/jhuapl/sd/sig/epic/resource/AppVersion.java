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

import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Properties;

@Secured
@Path("/AppVersion")
public class AppVersion
{

    @Context
    private ServletContext ctx;

    private static final Logger LOGGER = LogManager.getLogger();

    @GET
    @Produces({MediaType.TEXT_PLAIN})
    public String getAppVersionDisplayString()
    {
        String v = "version : n/a";
        try
        {
            Properties prop = new Properties();
            prop.load(ctx.getResourceAsStream("/META-INF/MANIFEST.MF"));
            String buildDate = prop.getProperty("build-date");
            String buildVersion = prop.getProperty("Implementation-Version");

            if (buildDate == null || buildDate.isEmpty())
            {
                buildDate = "n/a";
            }

            if (buildVersion == null || buildVersion.isEmpty())
            {
                buildVersion = "dev";
            }
            v = "version: " + buildVersion + " (" + buildDate + ")";
        }
        catch (IOException e)
        {
            LOGGER.error("Unable to gather version info. ", e);
        }
        return v;
    }
}
