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
import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.ldap.LdapName;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;

public class LdapNameTest
{
	private Name n1, n2, n3, n4; 
	
	@Before
	public void init() throws InvalidNameException
	{
		n1 = new LdapName("cn=admin,dc=foo,dc=bar");
		
		n2 = new LdapName("CN=admin,DC=foo,DC=bar");
		
		n3 = new LdapName("commonName=ADMIN,dc=FOO,dc=BAR");
		
		n4 = new LdapName("cn=admin,dc=foo,dc=bar");
	}

	@Test
	public void equals()
	{
		assertTrue(  n1.equals( n2 ) );
		
		assertTrue(  n1.compareTo( n4 ) == 0 );
		
		assertFalse(  n1.equals( n3 ) );
		
		assertFalse(  n2.equals( n3 ) );
	}

	@Test
	public void hash()
	{
		Set<Name> s = Sets.newHashSet();
		
		assertTrue( s.add( n1 ) );
		
		assertFalse( s.add( n2 ) );
		
		assertFalse( s.add( n4 ) );
		
		assertTrue( s.add( n3 ) );
	}
}
