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

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

//Defines the base URI for all resource URIs.
@ApplicationPath("/resources")
//The java class declares root resource and provider classes
public class MyApplication extends ResourceConfig
{

    public MyApplication()
    {
        // Register resources and providers using package-scanning.
        packages("edu.jhuapl.sd.sig.epic.resource", "edu.jhuapl.sd.sig.epic.resource.util");
        register(MultiPartFeature.class);
        register(new MyApplicationBinder());

        // Register my custom provider - not needed if it's in my.package.
        //	register(SecurityRequestFilter.class);
        // Register an instance of LoggingFilter.
        //	register(new LoggingFilter(LOGGER, true));

        // Enable Tracing support.
        //	property(ServerProperties.TRACING, "ALL");
    }

    //The method returns a non-empty collection with classes, that must be included in the published JAX-RS application
    //	@Override
    //	public Set<Class<?>> getClasses()
    //	{
    //		HashSet h = new HashSet<Class<?>>();
    //
    //		h.add(GenericExceptionMapper.class);
    //		h.add(CORSFilter.class);
    //		h.add(Approvals.class);
    //		h.add(AppVersion.class);
    //		h.add(CreateProcedure.class);
    //		h.add(FindProcedures.class);
    //		h.add(Programs.class);
    //		h.add(Runs.class);
    //		h.add(TpDrafts.class);
    //		h.add(AuthorProcedure.class);
    //		h.add(SaveProcedureHeaderUserData.class);
    //		h.add(AllUsers.class);
    //		h.add(SaveProcedureData.class);
    //		h.add(SaveInstructionInfoSectionData.class);
    //		h.add(UpdateInstructionInfoData.class);
    //		h.add(SaveStepGroupData.class);
    //
    //		return h;
    //	}
}
