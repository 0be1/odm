package fr.mtlx.odm.converters;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;

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

public abstract class AttributeConverter<D, O> implements Converter
{
	protected final Class<D> directoryType;

	protected final Class<O> objectType;

	public AttributeConverter( final Class<D> directoryType, final Class<O> objectType )
	{
		this.directoryType = checkNotNull( directoryType );
		this.objectType = checkNotNull( objectType );
	}

	public abstract D to( final O object ) throws ConvertionException;

	public abstract O from( final D value ) throws ConvertionException;

	@SuppressWarnings( "unchecked" )
	@Override
	public final Object toDirectory( @Nullable final Object object ) throws ConvertionException
	{
		if ( object == null )
			return null;

		if ( objectType.isInstance( object ) )
			return to( (O)object );

		throw new ConvertionException( "type mismatch" );
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public final Object fromDirectory( @Nullable final Object value ) throws ConvertionException
	{
		if ( value == null )
			return null;

		if ( directoryType().isInstance( value ) )
			return from( (D)value );

		throw new ConvertionException( "type mismatch" );
	}

	@Override
	public final Class<?> directoryType()
	{
		return directoryType;
	}

	@Override
	public final Class<?> objectType()
	{
		return objectType;
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if (!(obj instanceof AttributeConverter)) {
			return false;
		}
		
		final AttributeConverter<?, ?> other = (AttributeConverter<?, ?>)obj;
		
		return (this.directoryType.equals( other.directoryType ) && this.objectType.equals( other.objectType ));
	}

	@Override
	public int hashCode()
	{
		return this.directoryType.hashCode() ^ this.objectType.hashCode();
	}
}
