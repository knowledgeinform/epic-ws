/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.resource.util;

import edu.jhuapl.sd.sig.epic.startup.AppConfiguration;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class CORSFilter implements ContainerResponseFilter
{

    public void filter(ContainerRequestContext request,
            ContainerResponseContext response) throws IOException
    {
        response.getHeaders().add("Access-Control-Allow-Origin", AppConfiguration.getConfigValue(AppConfiguration.AppConfigKey.ALLOWED_ORIGIN));
        response.getHeaders().add("Access-Control-Allow-Headers",
                "origin, content-type, accept, authorization, jsnlog-requestid");
        response.getHeaders().add("Access-Control-Allow-Credentials", "true");
        response.getHeaders().add("Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD");
    }
}
