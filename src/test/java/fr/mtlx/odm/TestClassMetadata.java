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

//import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.lang.reflect.Constructor;
import java.util.Set;

import org.junit.Test;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import fr.mtlx.odm.model.GroupOfPersons;
import fr.mtlx.odm.model.OrganizationalPerson;
import fr.mtlx.odm.model.Person;

public class TestClassMetadata
{
	@Test
	public void testGetEntityClass() throws MappingException
	{
		String className = Person.class.getName();

		ClassMetadata<?> metadata = ClassMetadataImpl.forName( className );

		assertEquals( Person.class, metadata.getEntryClass() );
	}

	@Test
	public void testGetStructuralClass() throws MappingException
	{
		ClassMetadata<Person> metadata = ClassMetadataImpl.forClass( Person.class );

		assertThat( metadata.getStructuralClass(), equalTo( "person" ) );
	}

	@Test
	public void testGetEntryClass() throws MappingException
	{
		ClassMetadata<Person> metadata = ClassMetadataImpl.forClass( Person.class );

		assertTrue( metadata.getEntryClass().equals( Person.class ) );
	}

	@Test
	public void testGetDefaultConstructor() throws Exception
	{
		ClassMetadata<Person> metadata = ClassMetadataImpl.forClass( Person.class );

		Constructor<?> ctor = metadata.getDefaultConstructor();

		assertNotNull( ctor );

		Object obj = ctor.newInstance( new Object[] {} );

		assertNotNull( obj );

		assertTrue( obj instanceof Person  );
	}

	@Test
	public void getObjectClassHierarchy() throws MappingException
	{
		ClassMetadata<OrganizationalPerson> metadata = ClassMetadataImpl.forClass( OrganizationalPerson.class );

		assertThat( metadata.getObjectClassHierarchy(), contains( "top", "person", "organizationalPerson" ) );
	}

	@Test
	public void getAttributeMetadata() throws MappingException
	{
		ClassMetadata<?> metadata = ClassMetadataImpl.forClass( Person.class );
		AttributeMetadata<?> ameta = metadata.getAttributeMetadataByAttributeName( "sn" );

		assertNotNull( ameta );

		assertThat( ameta.getPropertyName(), equals( "surname" ) );
		
		metadata = new ClassMetadataImpl<GroupOfPersons>( GroupOfPersons.class );
		
		assertNotNull( metadata.getAttributeMetadataByPropertyName( "members" ) );
	}
	
	@SuppressWarnings( "serial" )
	public static class ExtendedPerson extends Person
	{
		public ExtendedPerson()
		{
			super();
		}
		
		@Attribute
		private String  extended;
		
	}
	
	@Test
	public void getAttributeMetadataInherited() throws MappingException
	{
		ClassMetadata<?> metadata = ClassMetadataImpl.forClass( ExtendedPerson.class );
		AttributeMetadata<?> ameta = metadata.getAttributeMetadataByAttributeName( "sn" );

		assertNotNull( ameta );

		assertThat( ameta.getPropertyName(), equalTo( "surname" ) );
		
		assertNotNull( metadata.getAttributeMetadataByPropertyName( "extended" ) );
	}


	@Test
	public void getAuxiliaryClasses() throws MappingException
	{
		ClassMetadata<Person> metadata = ClassMetadataImpl.forClass( Person.class );

		assertTrue( metadata.getAuxiliaryClasses().isEmpty() );
	}

	@Test
	public void getProperties() throws MappingException
	{
		ClassMetadata<Person> metadata = ClassMetadataImpl.forClass( Person.class );

		Set<String> properties = metadata.getProperties();

		assertTrue( properties.contains( "surname" ) );
		
		assertTrue( properties.contains( "commonName" ) );

		assertFalse( "static property", Iterables.any( properties, new Predicate<String>()
		{
			@Override
			public boolean apply( String input )
			{
				return input != null && "serialVersionUID".equals( input );
			}

		} ) );

		assertFalse( "identifier property", Iterables.any( properties, new Predicate<String>()
		{
			@Override
			public boolean apply( String input )
			{
				return input != null && "id".equals( input );
			}

		} ) );
	}
}
