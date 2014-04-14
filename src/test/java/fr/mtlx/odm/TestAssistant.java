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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.ldap.LdapName;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import fr.mtlx.odm.model.Person;

public class TestAssistant
{
	ClassAssistant<Person> assistant;
	
	@Before
	public void init()
	{
		ClassMetadata<Person> metadata = ClassMetadataImpl.forClass( Person.class );
		
		assistant = new ClassAssistant<Person>( metadata );
	}

	@Test
	public void setIdentifier() throws InvalidNameException, MappingException
	{
		Name dn = new LdapName( "uid=foo,ou=person" );

		Person entry = new Person();

		assistant.setIdentifier( entry, dn );

		assertTrue( entry.getDn().equals( dn ) );
	}

	@Test
	public void setSimpleProperty() throws MappingException
	{
		final String surname = RandomStringUtils.randomAlphabetic( 10 );

		Person entry = new Person();

		assistant.setSimpleProperty( "surname", entry, surname );

		assertThat( entry.getSurname(), is( surname ) );
	}

	@Test
	public void testSetValues() throws MappingException
	{
		final String[] tels = new String[]
		{ "0123456789", "987654321" };

		Person entry = new Person();

		assistant.setProperty( "telephoneNumber", entry, Arrays.asList( tels ) );

		assertThat( entry.getTelephoneNumber().size(), is( 2 ) );

		assertThat( entry.getTelephoneNumber().get( 0 ), is( "0123456789" ) );

		assertThat( entry.getTelephoneNumber().get( 1 ), is( "987654321" ) );
	}
}
