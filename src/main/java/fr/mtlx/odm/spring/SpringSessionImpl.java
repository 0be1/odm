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

import javax.naming.Name;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DirContextOperations;

import fr.mtlx.odm.Operations;
import fr.mtlx.odm.SessionImpl;
import fr.mtlx.odm.cache.Cache;

public class SpringSessionImpl extends SessionImpl implements Cache<DirContextOperations, Name> {

    private static final Logger log = LoggerFactory.getLogger(SpringSessionImpl.class);

    private final Cache<DirContextOperations, Name> contextCache;
    private final SpringSessionFactoryImpl sessionFactory;

    SpringSessionImpl(final SpringSessionFactoryImpl sessionFactory) {
        this.sessionFactory = sessionFactory;

        this.contextCache = new ContextMapCache();
    }

    @Override
    public SpringSessionFactoryImpl getSessionFactory() {
        return sessionFactory;
    }

    @Override
    public <T> Operations<T> getOperations(Class<T> persistentClass) {
        return new SpringOperationsImpl<>(this, persistentClass);
    }

    @Override
    public Optional<DirContextOperations> store(Name dn, Optional<DirContextOperations> context) {
	contextCache.store(dn, context);

        return context;
    }

    @Override
    public Optional<DirContextOperations> retrieve(Name key) {
        return contextCache.retrieve(key);
    }

    @Override
    public boolean remove(final Name key) {
        return contextCache.remove(key);
    }

    @Override
    public boolean contains(Name key) {
        return contextCache.contains(key);
    }

    @Override
    public void close() {
        contextCache.clear();

        super.close();
    }
}
