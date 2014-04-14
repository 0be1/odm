package fr.mtlx.odm.converters;

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

public abstract class AbstractConverter implements Converter
{
	@Override
	public Object toDirectory( @Nullable final Object object )  throws ConvertionException
	{
		if (object == null)
			return object;
		
		if (! objectType().isInstance( object ) )
			throw new ConvertionException("type mismatch");
		
		return object;
	}

	@Override
	public Object fromDirectory( final Object value )  throws ConvertionException
	{
		if (value == null)
			return null;
		
		if (! directoryType().isInstance( value ) )
			throw new ConvertionException("type mismatch");
		
		return value;
	}
	
	@Override
	public abstract String getSyntax();
	

	@Override
	public abstract Class<?> directoryType();
	

	@Override
	public abstract Class<?> objectType();
	
}
