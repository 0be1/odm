package fr.mtlx.odm.converters;

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

import javax.naming.NameNotFoundException;
import javax.naming.ldap.LdapName;

import fr.mtlx.odm.ClassAssistant;
import fr.mtlx.odm.ClassMetadata;
import fr.mtlx.odm.Session;
import java.lang.reflect.InvocationTargetException;
import javax.naming.InvalidNameException;

public class EntryResolverConverter<T extends Object> extends
		AttributeConverter<LdapName, T> {
	private final Session session;

	private final ClassMetadata<T> metadata;

	public EntryResolverConverter(final Class<T> objectClass,
			final Session session) {
		super(LdapName.class, objectClass);

		this.session = checkNotNull(session, "session is null");

		this.metadata = session.getSessionFactory().getClassMetadata(
				this.objectType);

		if (metadata == null) {
                    throw new UnsupportedOperationException(String.format(
                            "%s is not a persistent class", this.objectType));
                }
	}

	@Override
	public LdapName to(final T object) throws ConvertionException {
		final ClassAssistant<T> assistant = new ClassAssistant<>(metadata);

		try {
			return assistant.getIdentifier(object);
		} catch (InvalidNameException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new ConvertionException(e);
		}
	}

	@Override
	public T from(LdapName dn) throws ConvertionException {
		try {
			return session.getOperations(objectType).lookup(dn);
		} catch (NameNotFoundException e) {
			throw new ConvertionException(e);
		}
	}
}
