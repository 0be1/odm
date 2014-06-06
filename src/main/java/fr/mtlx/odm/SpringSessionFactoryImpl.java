package fr.mtlx.odm;

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
import java.util.Set;

import javax.naming.directory.DirContext;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapTemplate;

import com.google.common.collect.Sets;

import fr.mtlx.odm.converters.Converter;
import fr.mtlx.odm.converters.DefaultConverters;

@SuppressWarnings("serial")
public class SpringSessionFactoryImpl extends SessionFactoryImpl implements
		InitializingBean {
	private final ContextSource contextSource;

	private List<String> mappedClasses;

	private final LdapTemplate ldapTemplate;

	private Set<String> operationalAttributes = Sets.newHashSet();

	public SpringSessionFactoryImpl(final ContextSource contextSource) {
		this.contextSource = checkNotNull(contextSource);

		this.ldapTemplate = new LdapTemplate(contextSource);
	}

	public SpringSessionFactoryImpl(final ContextSource contextSource,
			final CacheFactory cacheFactory, final String region) {
		this(checkNotNull(contextSource));
	}

	public ContextSource getContextSource() {
		return contextSource;
	}

	public DirContext getDirContext() {
		return contextSource.getReadWriteContext();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		for (Entry<String, Converter> entry : DefaultConverters.defaultSyntaxConverters
				.entrySet()) {
			addSyntaxConverter(entry.getKey(), entry.getValue());
		}

		for (Entry<Type, Converter> entry : DefaultConverters.defaultAttributeConverters
				.entrySet()) {
			addAttributeConverter(entry.getKey(), entry.getValue());
		}

		for (String className : mappedClasses) {
			mapClass(className);
		}

		init();
	}

	public LdapTemplate getLdapTemplate() {
		return this.ldapTemplate;
	}

	@Override
	public boolean isOperationalAttribute(String attributeId) {
		return super.isOperationalAttribute(attributeId)
				|| operationalAttributes.contains(attributeId);
	}

	public void setOperationalAttributes(Set<String> operationalAttributes) {
		this.operationalAttributes = operationalAttributes;
	}

	public void setMappedClasses(List<String> mappedClasses) {
		this.mappedClasses = mappedClasses;
	}

	@Override
	public Session openSession() {
		return new SessionImpl(this);
	}

	@Override
	public void addClass(Class<?> persistentClass) {
		mappedClasses.add(persistentClass.getCanonicalName());
	}

	@Override
	public void addClass(String persistentClassName) {
		mappedClasses.add(persistentClassName);
	}
}
