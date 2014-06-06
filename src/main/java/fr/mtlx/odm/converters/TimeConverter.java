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

public class TimeConverter extends SyntaxConverter<String, Date>
{
	public TimeConverter()
	{
		super( String.class, Date.class );
	}

	public final static SimpleDateFormat generalizedTime;

	@Override
	public String to( final Date object ) throws ConvertionException
	{
		return generalizedTime.format( object );
	}

	@Override
	public Date from( String value ) throws ConvertionException
	{
		try
		{
			return (Date)generalizedTime.parseObject( value );
		}
		catch ( ParseException e )
		{
			throw new ConvertionException( e );
		}
	}

	static
	{
		generalizedTime = new SimpleDateFormat( "yyyyMMddHHmmss'Z'" );

		generalizedTime.setTimeZone( new SimpleTimeZone( 0, "Z" ) );
	}
}
