/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.data.util;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.proxy.pojo.javassist.JavassistLazyInitializer;
import org.javers.core.graph.ObjectAccessProxy;
import org.javers.hibernate.HibernateUnproxyObjectAccessHook;

import java.util.Optional;

public class HibernateEntityAccessHook<T> extends HibernateUnproxyObjectAccessHook
{

    public Optional<ObjectAccessProxy<T>> createAccessor(Object entity)
    {
        if (entity instanceof HibernateProxy)
        {
            LazyInitializer lazyInitializer = ((HibernateProxy) entity).getHibernateLazyInitializer();
            return fromLazyInitializer(lazyInitializer);
        }
        if (entity instanceof JavassistLazyInitializer)
        {
            JavassistLazyInitializer proxy = (JavassistLazyInitializer) entity;
            return fromLazyInitializer(proxy);
        }
        return Optional.empty();
    }

    public Optional<ObjectAccessProxy<T>> fromLazyInitializer(LazyInitializer lazyInitializer)
    {
        return Optional.of(new ObjectAccessProxy(() -> lazyInitializer.getImplementation(), lazyInitializer.getPersistentClass(), lazyInitializer.getIdentifier()));
    }
}
