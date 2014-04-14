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

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapTemplate;

import com.google.common.collect.Sets;

import fr.mtlx.odm.converters.LdapNameConverter;

public class SpringSessionFactoryImpl extends SessionFactoryImpl implements InitializingBean
{
	private static final long serialVersionUID = 7615860356986827891L;

	private List<String> mappedClasses;

	private final LdapTemplate ldapTemplate;

	private Set<String> operationalAttributes = Sets.newHashSet();
	
	public SpringSessionFactoryImpl( final ContextSource contextSource )
	{
		super( checkNotNull( contextSource ) );

		this.ldapTemplate = new LdapTemplate( contextSource );
	}

	public SpringSessionFactoryImpl( final ContextSource contextSource, final CacheFactory cacheFactory, final String region )
	{
		super( checkNotNull( contextSource ), checkNotNull( cacheFactory ), checkNotNull(region, "region is null" ) );

		this.ldapTemplate = new LdapTemplate( contextSource );
	}
	
	@Override
	public void afterPropertiesSet() throws Exception
	{
		for ( String className : mappedClasses )
		{
			addMappedClass( className );
		}

		addConverter( new LdapNameConverter() );
	}

	public LdapTemplate getLdapTemplate()
	{
		return this.ldapTemplate;
	}

	@Override
	public boolean isOperationalAttribute( String attributeId )
	{
		return super.isOperationalAttribute( attributeId ) || operationalAttributes.contains( attributeId );
	}

	public void setOperationalAttributes( Set<String> operationalAttributes )
	{
		this.operationalAttributes = operationalAttributes;
	}

	public void setMappedClasses( List<String> mappedClasses )
	{
		this.mappedClasses = mappedClasses;
	}
}
