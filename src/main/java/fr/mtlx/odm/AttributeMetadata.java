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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import fr.mtlx.odm.converters.Converter;

@SuppressWarnings("serial")
public class AttributeMetadata implements Serializable {
	private String propertyName;

	private String attributeName;

	private String[] attributeAliases;

	private Type objectType;

	private Type directoryType;

	private String syntax;

	private Converter syntaxConverter;

	private Converter attributeConverter;

	private Class<?> persistentClass;

	private Class<? extends Collection<?>> collectionType;

	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public void setAttributeAliases(String[] attributeAliases) {
		this.attributeAliases = attributeAliases;
	}

	public void setObjectType(Type objectType) {
		this.objectType = objectType;
	}

	public void setDirectoryType(Type directoryType) {
		this.directoryType = directoryType;
	}

	public void setSyntax(String syntax) {
		this.syntax = syntax;
	}

	public void setSyntaxConverter(Converter syntaxConverter) {
		this.syntaxConverter = syntaxConverter;
	}

	public void setAttributeConverter(Converter attributeConverter) {
		this.attributeConverter = attributeConverter;
	}

	public void setPersistentClass(Class<?> persistentClass) {
		this.persistentClass = persistentClass;
	}

	public void setCollectionType(Class<? extends Collection<?>> collectionType) {
		this.collectionType = collectionType;
	}

	public String getAttirbuteName() {
		return attributeName;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public Type getObjectType() {
		return objectType;
	}

	public Type getDirectoryType() {
		return directoryType;
	}

	public String getSyntax() {
		return syntax;
	}

	public boolean isMultivalued() {
		return collectionType != null;
	}

	public Class<? extends Collection<?>> getCollectionType() {
		return collectionType;
	}

	public String[] getAttributeAliases() {
		return attributeAliases;
	}

	public Collection<?> newCollectionInstance() {
		final Class<? extends Collection<?>> ctype = getCollectionType();

		if (ctype.isAssignableFrom(ArrayList.class)) {
			return new ArrayList<Object>();
		} else if (ctype.isAssignableFrom(HashSet.class)) {
			return new HashSet<Object>();
		}

		return null;
	}

	public Converter getSyntaxConverter() {
		return syntaxConverter;
	}

	public Class<?> getPersistentClass() {
		return persistentClass;
	}

	public Converter getAttributeConverter() {
		return attributeConverter;
	}
}
