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

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import javax.naming.InvalidNameException;
import javax.naming.directory.DirContext;
import javax.naming.ldap.Rdn;

import org.springframework.ldap.core.ContextSource;

import fr.mtlx.odm.cache.CacheManager;
import fr.mtlx.odm.converters.Converter;

public interface SessionFactory extends Serializable
{
	<T> ClassMetadata<T> getClassMetadata( Class<T> entityClass );

	ClassMetadata<?> getClassMetadata( String entityName );

	boolean isPersistentClass( String className );

	/**
	 * clazz est persistente si c'est une super classe d'une classe mappée
	 * @param clazz
	 * @return true if clazz is persistent
	 */
	boolean isPersistentClass( Class<?> clazz );

	DirContext getDirContext();

	ContextSource getContextSource();
	
	Session openSession();
	
	void closeSession();
	
	Session getCurrentSession();
	
	boolean isOperationalAttribute( String attributeId );
	
	Converter getConverter( String syntax, Type objectType, Type directoryType );

	<T> Rdn composeName( final T object, final String propertyName ) throws InvalidNameException, MappingException;

	CacheManager getCacheManager();

	ClassMetadata<?> getClassMetadata( String[] objectClasses ) throws IllegalAccessException, InvocationTargetException, ClassNotFoundException;
	
	<T> BasicProxyFactory<T> getProxyFactory( Class<T> clazz, Class<?>[] interfaces );
}
