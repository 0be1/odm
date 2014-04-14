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

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import fr.mtlx.odm.model.GroupOfPersons;
import fr.mtlx.odm.model.Person;

public class TestAttributeMetadata
{
	@Test
	public void getPropertyName() throws Exception
	{
		AttributeMetadata<Person> metadata = new AttributeMetadata<Person>( Person.class, "surname" );
		
		assertEquals( "surname", metadata.getPropertyName() );
	}

	@Test
	public void getSyntax() throws Exception
	{
		AttributeMetadata<Person> metadata = new AttributeMetadata<Person>( Person.class, "surname" );
		
		assertEquals( "1.3.6.1.4.1.1466.115.121.1.15", metadata.getSyntax() );
	}
	
	@Test
	public void getSyntaxForMultivaluedAttribute() throws Exception
	{
		AttributeMetadata<Person> metadata = new AttributeMetadata<Person>( Person.class, "telephoneNumber" );
		
		assertEquals( "1.3.6.1.4.1.1466.115.121.1.15", metadata.getSyntax() );
	}
	
	
	@Test
	@Ignore
	public void getSyntaxForMultivaluedAttributeReferal() throws Exception
	{
		AttributeMetadata<GroupOfPersons> metadata = new AttributeMetadata<GroupOfPersons>( GroupOfPersons.class, "members" );
		
		assertEquals( "1.3.6.1.4.1.1466.115.121.1.12", metadata.getSyntax() );
	}
}
