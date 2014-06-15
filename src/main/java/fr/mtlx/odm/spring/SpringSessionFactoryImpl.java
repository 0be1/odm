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
import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map.Entry;

import javax.naming.directory.DirContext;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapTemplate;

import fr.mtlx.odm.CacheFactory;
import fr.mtlx.odm.ConcurentMapCacheFactory;
import fr.mtlx.odm.NoCacheFactory;
import fr.mtlx.odm.SessionFactoryImpl;
import fr.mtlx.odm.cache.NoCache;
import fr.mtlx.odm.cache.PersistentCache;
import fr.mtlx.odm.converters.Converter;
import fr.mtlx.odm.converters.DefaultConverters;

@SuppressWarnings("serial")
public class SpringSessionFactoryImpl extends SessionFactoryImpl implements InitializingBean {

    private final ContextSource contextSource;

    private List<String> mappedClasses;

    private final LdapTemplate ldapTemplate;
    
    private PersistentCache cache = new NoCache();
    
    private CacheFactory sessionCacheFactory = new ConcurentMapCacheFactory();
    
    private CacheFactory contextCacheFactory = new ConcurentMapCacheFactory();
    
    private CacheFactory secondLevelCacheFactory = new NoCacheFactory();

    public ContextSource getContextSource() {
	return contextSource;
    }

    public DirContext getDirContext() {
	return contextSource.getReadWriteContext();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
	for (Entry<String, Converter> entry : DefaultConverters.defaultSyntaxConverters.entrySet()) {
	    addConverter(entry.getKey(), entry.getValue());
	}

	for (Entry<Type, Converter> entry : DefaultConverters.defaultAttributeConverters.entrySet()) {
	    addConverter(entry.getKey(), entry.getValue());
	}

	for (String className : mappedClasses) {
	    addClass(className);
	}
	
	if (secondLevelCacheFactory != null) {
	    cache = checkNotNull(secondLevelCacheFactory.getCache());
	}

	initialize();
    }

    public LdapTemplate getLdapTemplate() {
	return this.ldapTemplate;
    }

    @Override
    public boolean isOperationalAttribute(String attributeId) {
	return super.isOperationalAttribute(attributeId) || operationalAttributes.contains(attributeId);
    }

    public void setMappedClasses(List<String> mappedClasses) {
	this.mappedClasses = mappedClasses;
    }

    @Override
    public SpringSessionImpl openSession() {
	return new SpringSessionImpl(this, sessionCacheFactory, contextCacheFactory);
    }

    @Override
    public PersistentCache getCache() {
	return cache;
    }
    
    public void setSessionCacheFactory(CacheFactory sessionCacheFactory) {
        this.sessionCacheFactory = sessionCacheFactory;
    }

    public void setContextCacheFactory(CacheFactory contextCacheFactory) {
        this.contextCacheFactory = contextCacheFactory;
    }

    public void setSecondLevelCacheFactory(CacheFactory secondLevelCacheFactory) {
        this.secondLevelCacheFactory = secondLevelCacheFactory;
    }

    public SpringSessionFactoryImpl(final ContextSource contextSource) {
	this.contextSource = checkNotNull(contextSource);

	this.ldapTemplate = new LdapTemplate(contextSource);
    }
}
