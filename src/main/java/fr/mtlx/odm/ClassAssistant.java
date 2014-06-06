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

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.ldap.LdapName;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.ReflectionUtils.FieldFilter;

public class ClassAssistant<T> {
	private final ClassMetadata<T> metadata;

	public ClassAssistant(final ClassMetadata<T> metadata) {
		this.metadata = checkNotNull(metadata);
	}

	public void setIdentifier(Object object, Name value) {
		String identifier = metadata.getIdentifierPropertyName();

		try {
			PropertyUtils.setSimpleProperty(object, identifier, value);
		} catch (Exception e) {
			throw new UnsupportedOperationException(e);
		}
	}

	public LdapName getIdentifier(Object object) throws InvalidNameException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		String identifier = metadata.getIdentifierPropertyName();

		return new LdapName(BeanUtils.getProperty(object, identifier));
	}

	public boolean isCollection(Type type) {
		Class<?> c = type.getClass();

		return Arrays.asList(c.getInterfaces()).contains(Collection.class)
				|| c.equals(Collection.class);
	}

	public Object getValue(Object object, String propertyName) {
		try {
			return PropertyUtils.getSimpleProperty(object, propertyName);
		} catch (Exception e) {
			throw new UnsupportedOperationException(e);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <V> Collection<V> buildCollection(
			final Class<? extends Collection<?>> collectionType,
			final Type componentType, final Collection<V> elements)
			throws MappingException {
		Collection values;

		if (collectionType.equals(List.class)) {
			values = new ArrayList<V>(elements);
		} else if (collectionType.equals(Set.class)) {
			return new HashSet<V>(elements);
		} else if (collectionType.equals(Collection.class)) {
			values = new ArrayList<V>(elements);
		} else {
			throw new MappingException("unsupported collection");
		}

		return values;
	}

	public void setSimpleProperty(final String propertyName, T entry,
			final Object singleValue) throws MappingException {
		try {
			PropertyUtils.setSimpleProperty(entry, propertyName, singleValue);
		} catch (IllegalAccessException e) {
			throw new MappingException(e);
		} catch (InvocationTargetException e) {
			throw new MappingException(e);
		} catch (NoSuchMethodException e) {
			throw new MappingException(e);
		}
	}

	public <V> void setProperty(final String propertyName, final T entry,
			final Collection<V> multipleValues) throws MappingException {
		@SuppressWarnings("unchecked")
		Class<T> c = (Class<T>) checkNotNull(entry).getClass();

		final AttributeMetadata meta = metadata
				.getAttributeMetadataByPropertyName(propertyName);

		if (meta == null)
			throw new MappingException(String.format(
					"propertyName: unknown property %s", propertyName));

		if (!meta.isMultivalued())
			throw new MappingException(String.format(
					"propertyName: single valued property %s", propertyName));

		final Collection<?> targetValues;

		targetValues = buildCollection(meta.getCollectionType(),
				meta.getObjectType(), multipleValues);

		try {
			PropertyUtils.setProperty(entry, propertyName, targetValues);
		} catch (IllegalAccessException e) {
			throw new MappingException(e);
		} catch (InvocationTargetException e) {
			throw new MappingException(e);
		} catch (NoSuchMethodException e) {
			try {
				ReflectionUtils.doWithFields(c, new FieldCallback() {

					@Override
					public void doWith(final Field f)
							throws IllegalArgumentException,
							IllegalAccessException {
						boolean secured = !f.isAccessible();

						if (secured)
							f.setAccessible(true);

						f.set(entry, targetValues);

						if (secured)
							f.setAccessible(false);
					}
				}, new FieldFilter() {

					@Override
					public boolean matches(final Field field) {
						final int modifiers = field.getModifiers();
						// no static fields please
						return !Modifier.isStatic(modifiers)
								&& field.getName() == propertyName;
					}
				});

			} catch (SecurityException e1) {
				throw new MappingException(e1);
			} catch (IllegalArgumentException e1) {
				throw new MappingException(e1);
			}
		}
	}
}
