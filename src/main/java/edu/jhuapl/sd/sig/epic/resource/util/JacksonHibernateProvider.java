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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

//jackson module to assist with lazy-loading, will null out lazy collections by default during serialization....
@Provider
public class JacksonHibernateProvider implements ContextResolver<ObjectMapper>
{

    @Override
    public ObjectMapper getContext(Class<?> type)
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Hibernate5Module());
        return mapper;
    }
}
