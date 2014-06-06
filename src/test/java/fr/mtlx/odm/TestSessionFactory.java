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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import javax.management.openmbean.InvalidOpenTypeException;
import javax.naming.directory.DirContext;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ldap.core.ContextSource;

import fr.mtlx.odm.model.Person;


public class TestSessionFactory
{
	final String[] mappedClasses = new String[]
	{ "fr.mtlx.odm.model.OrganizationalPerson", };

	private SessionFactoryImpl sessionFactory;

	@Before
	public void init() throws Exception
	{
		ContextSource contextSource = mock( ContextSource.class );

		when( contextSource.getReadWriteContext() ).thenReturn( mock(DirContext.class) );
		
		SessionFactoryImpl sessionFactory = new SessionFactoryImpl()
		{
			@Override
			public Session openSession()
			{
				throw new InvalidOpenTypeException("not implemented");
			}

			@Override
			public void addClass( Class<?> persistentClass )
			{
			}

			@Override
			public void addClass( String persistentClassName )
			{
			}
		};

		this.sessionFactory = sessionFactory;
	}

	@Test
	public void isPersistentClass()
	{
		assertFalse( sessionFactory.isPersistentClass( "foo" ) );

		for ( String className : mappedClasses )
		{
			assertTrue( sessionFactory.isPersistentClass( className ) );
		}
		
		assertTrue(  sessionFactory.isPersistentClass( Person.class ) );
	}
	
	@Test
	public void isPersistentInheritedNotPersistent()
	{
		@SuppressWarnings( "serial" )
		class ExtPerson extends Person
		{ }
		
		assertFalse( sessionFactory.isPersistentClass( ExtPerson.class ) );
	}
	
	@Test
	public void defaultConverters() throws MappingException
	{
		assertNotNull( sessionFactory.getConverter( "1.3.6.1.4.1.1466.115.121.1.15" ) );
	}
}
