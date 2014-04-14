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

import javax.naming.Name;
import javax.naming.NameNotFoundException;

import fr.mtlx.odm.ClassAssistant;
import fr.mtlx.odm.ClassMetadata;
import fr.mtlx.odm.MappingException;
import fr.mtlx.odm.Session;

public class EntryResolverConverter<T> extends LdapNameConverter
{
	private final Session session;

	private final Class<T> persistentClass;

	private final ClassMetadata<T> metadata;

	public EntryResolverConverter( final Class<T> persistentClass, final Session session )
	{
		super();

		this.session = checkNotNull( session, "session is null" );

		this.persistentClass = checkNotNull( persistentClass, "persistentClass is null" );

		this.metadata = this.session.getSessionFactory().getClassMetadata( this.persistentClass );

		if ( metadata == null )
			throw new MappingException( String.format( "%s is not a persistent class", this.persistentClass ) );
	}

	@Override
	public Object toDirectory( final Object object ) throws ConvertionException
	{
		final ClassAssistant<T> assistant = new ClassAssistant<T>( metadata );

		try
		{
			final Name dn = assistant.getIdentifier( object );

			return super.toDirectory( dn );
		}
		catch ( MappingException e )
		{
			throw new ConvertionException( e );
		}
	}

	@Override
	public Object fromDirectory( Object value ) throws ConvertionException
	{
		final Name dn = (Name)super.fromDirectory( value );

		if ( dn == null )
			return null;

		try
		{
			return session.getOperations( persistentClass ).lookup( dn );
		}
		catch ( NameNotFoundException e )
		{
			throw new ConvertionException( e );
		}
	}
}
