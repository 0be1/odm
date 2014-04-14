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

import java.lang.reflect.Type;

import javax.naming.Name;
import javax.naming.directory.ModificationItem;

import org.springframework.ldap.core.LdapOperations;

import fr.mtlx.odm.converters.Converter;

public interface Session
{
	void close();

	void modifyAttributes( Name dn, ModificationItem[] mods );
	
	SessionFactory getSessionFactory();

	boolean isPersistent( Object obj );

	Converter getConverter( String syntax, Type objectType, Type directoryType ) throws MappingException;
	
	LdapOperations getLdapOperations();
	
	<T> Operations<T> getOperations( Class<T> persistentClass ); 
}
