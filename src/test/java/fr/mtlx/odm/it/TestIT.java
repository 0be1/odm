package fr.mtlx.odm.it;

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

import java.io.File;

import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.ldap.LdapName;

import org.apache.directory.server.ldap.LdapServer;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.support.LdapContextSource;

import fr.mtlx.odm.Session;
import fr.mtlx.odm.SessionFactory;
import fr.mtlx.odm.spring.SpringSessionFactoryImpl;
import fr.mtlx.odm.model.Person;

import java.io.IOException;


@Ignore
public class TestIT
{
	private static SessionFactory factory;

	private static EmbeddedDS ds;
	
	private Session session;
	
	static ContextSource getWiredContextSource( LdapServer server )
	{
		LdapContextSource contextSource = new LdapContextSource();
		
		contextSource.setUrl( "ldap://localhost:" + server.getPort() );
		contextSource.setBase( EmbeddedDS.PARTITION );
		contextSource.setUserDn( "cn=admin," + EmbeddedDS.PARTITION );
		contextSource.setPassword( "secret" );
		
		return contextSource;
	}
	
	
	@BeforeClass
	public static void setUp() throws Exception
	{
		ds = new EmbeddedDS(new File("/tmp"));
		
		ContextSource contextSource = getWiredContextSource( ds.getServer() );
		
		factory = new SpringSessionFactoryImpl( contextSource );
		
		factory.addClass( Person.class );
	}
	
	@Before
	public void openSession()
	{
		session = factory.openSession();
	}
	
	@Test
	public void test() throws InvalidNameException, NameNotFoundException
	{
		Person person = new Person();
		
		person.setCommonName( "foo" );
		person.setSurname( "bar" );
		person.setDn( new LdapName( "cn=foo" ) );
		
		session.getOperations( Person.class ).bind( person );
	}
	
	
	@After
	public void closeSession() throws IOException
	{
		if (session != null)
		{
			session.close();
		}
	}
}
