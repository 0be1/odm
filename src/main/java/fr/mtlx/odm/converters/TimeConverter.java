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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;

public class TimeConverter extends AbstractConverter
{
	public final static SimpleDateFormat generalizedTime;
	
	static
	{
		generalizedTime = new SimpleDateFormat( "yyyyMMddHHmmss'Z'" );

		generalizedTime.setTimeZone( new SimpleTimeZone( 0, "Z" ) );	
	}
	
	@Override
	public String getSyntax()
	{
		return "1.3.6.1.4.1.1466.115.121.1.24";
	}

	@Override
	public Object toDirectory( final Object object ) throws ConvertionException
	{
		final Date retval = (Date)super.toDirectory( object );
		
		return retval != null ? generalizedTime.format( retval ) : retval;
	}

	@Override
	public Object fromDirectory( final Object value ) throws ConvertionException
	{
		final String retval = (String)super.fromDirectory( value );
		
		try
		{
			return retval != null ? generalizedTime.parseObject( retval ) : retval;
		}
		catch ( ParseException e )
		{
			throw new ConvertionException( e );
		}
	}

	@Override
	public Class<?> directoryType()
	{
		return String.class;
	}

	@Override
	public Class<?> objectType()
	{
		return Date.class;
	}
}
