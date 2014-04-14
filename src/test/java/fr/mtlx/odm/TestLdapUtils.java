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

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.ldap.LdapName;

import org.junit.Assert;

import org.junit.Test;

import fr.mtlx.odm.utils.LdapUtils;


public class TestLdapUtils
{
	@Test
	public void testgetParent() throws InvalidNameException
	{
		Name name = new LdapName("cn=test,ou=personnes,o=foo,c=bar");
		
		Name parent = LdapUtils.getDistinguishedNameParent( name );
		
		Assert.assertEquals( new LdapName("ou=personnes,o=foo,c=bar"), parent );
		
		Name c = LdapUtils.getDistinguishedNamePrefix( name, "c" );
		
		Name o = LdapUtils.getDistinguishedNamePrefix( name, "o" );
		
		Name ou = LdapUtils.getDistinguishedNamePrefix( name, "ou" );
		
		Name cn = LdapUtils.getDistinguishedNamePrefix( name, "cn" );
		
		Name x = LdapUtils.getDistinguishedNamePrefix( name, "x" );
		
		Assert.assertEquals( new LdapName("c=bar"), c );
		
		Assert.assertEquals( new LdapName("o=foo,c=bar"), o );
		
		Assert.assertEquals( new LdapName("ou=personnes,o=foo,c=bar"), ou );
		
		Assert.assertEquals( name, cn );
		
		Assert.assertNull( x );
		
		
		Assert.assertEquals( new LdapName("ou=structures,o=foo,c=bar"), o.addAll( new LdapName("ou=structures") ) );
	}
}
