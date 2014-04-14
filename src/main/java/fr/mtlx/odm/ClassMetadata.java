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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import fr.mtlx.odm.filters.Filter;

import com.google.common.collect.ImmutableSet;

public interface ClassMetadata<T>
{
	Class<T> getEntryClass();
	
	/**
	 * Get the name of the identifier property (or return null)
	 */
	String getIdentifierPropertyName();
	
	/**
	 * Get the identifier type
	 */
	Type getIdentifierType();

	/**
	 * Get the identifier syntax
	 */
	String getIdentifierSyntax();

	Constructor<T> getDefaultConstructor();

	List<String> getObjectClassHierarchy();
	
	ImmutableSet<String> getAuxiliaryClasses();

	String getStructuralClass();
	
	Set<String> getProperties();

	Field getIdentifier();

	Filter getByExampleFilter();

	AttributeMetadata<T> getAttributeMetadataByAttributeName( String attributeName );

	AttributeMetadata<T> getAttributeMetadataByPropertyName( String propertyName );
	
	Method[] prepersistMethods();
	
	boolean isCacheable();
	
	/**
	 * an exception will be raised if non-matched attributes are found in the entry  
	 * @return
	 */
	boolean isStrict();
}