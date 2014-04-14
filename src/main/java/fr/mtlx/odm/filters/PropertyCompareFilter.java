package fr.mtlx.odm.filters;

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

import fr.mtlx.odm.AttributeMetadata;
import fr.mtlx.odm.ClassAssistant;
import fr.mtlx.odm.ClassMetadata;
import fr.mtlx.odm.MappingException;
import fr.mtlx.odm.Session;

public class PropertyCompareFilter implements Filter
{
	protected final String property;

	protected final Object value;

	protected final Comparison op;

	PropertyCompareFilter( final Comparison op, final String property, final Object value )
	{
		this.property = checkNotNull( property );
		this.value = value;
		this.op = op;
	}

	@Override
	public String encode( final Class<?> persistentClass, final Session session )
	{
		return composeFilter( persistentClass, session ).encode( persistentClass, session );
	}

	protected Filter composeFilter( final Class<?> persistentClass, final Session session )
	{
		final ClassMetadata<?> metadata = checkNotNull( session ).getSessionFactory().getClassMetadata( persistentClass );

		if ( metadata == null )
			throw new MappingException( String.format( "%s is not a persistent class", persistentClass ) );

		final AttributeMetadata<?> attribute = metadata.getAttributeMetadataByPropertyName( property );

		if ( attribute == null )
			throw new MappingException( String.format( "property %s not found in %s", property, checkNotNull( metadata ).getEntryClass() ) );

		return new RawCompareFilter( op, attribute.getAttirbuteName(), formatValue( encodeValue( value, attribute, session ) ) );
	}
	
	protected String formatValue( final String encodedValue )
	{
		return FilterEncoder.encode( encodedValue );
	}
	
	protected String encodeValue( final Object value, final AttributeMetadata<?> attribute, final Session session )
	{
		if ( value != null )
		{
			if ( attribute.getObjectType() instanceof Class<?> )
			{
				Class<?> clazz = (Class<?>)attribute.getObjectType();

				if ( !clazz.isInstance( value ) )
					throw new MappingException( String.format( "wrong type (%s) for property %s in %s, expecting %s", value.getClass(), property, attribute.getObjectType(), clazz ) );

				ClassMetadata<?> refmetadata = session.getSessionFactory().getClassMetadata( clazz );

				if ( refmetadata != null )
				{
					@SuppressWarnings(
					{ "rawtypes", "unchecked" } )
					ClassAssistant<?> assistant = new ClassAssistant( refmetadata );

					Name refdn = assistant.getIdentifier( value );

					return refdn.toString();
				}
			}
			
			return value.toString();
		}
		
		return null;
	}
}
