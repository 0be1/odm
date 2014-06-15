package fr.mtlx.odm.spring;

/*
 * #%L
 * fr.mtlx.odm
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2012 - 2013 Alexandre Mathieu <me@mtlx.fr>
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import java.util.Optional;

import org.springframework.ldap.core.DirContextOperations;

import fr.mtlx.odm.CacheFactory;
import fr.mtlx.odm.Operations;
import fr.mtlx.odm.SessionImpl;
import fr.mtlx.odm.cache.NoCache;
import fr.mtlx.odm.cache.TypeSafeCache;

public class SpringSessionImpl extends SessionImpl  {

    private final SpringSessionFactoryImpl sessionFactory;

    private final TypeSafeCache<DirContextOperations> contextCache;

    SpringSessionImpl(final SpringSessionFactoryImpl sessionFactory, final CacheFactory sessionCacheFactory, final CacheFactory contextCacheFactory) {
	super(sessionCacheFactory);
	
        this.sessionFactory = sessionFactory;

        this.contextCache = new TypeSafeCache<DirContextOperations>(DirContextOperations.class, Optional.ofNullable(contextCacheFactory.getCache()).orElse(new NoCache()));
    }

    @Override
    public SpringSessionFactoryImpl getSessionFactory() {
        return sessionFactory;
    }

    @Override
    public <T> Operations<T> getOperations(Class<T> persistentClass) {
        return new SpringOperationsImpl<>(this, persistentClass);
    }
    
    public TypeSafeCache<DirContextOperations> getContextCache() {
        return contextCache;
    }

    @Override
    public void close() {
        contextCache.clear();

        super.close();
    }
}
