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

public class PropertyCriterion implements CompareCriterion
{
	private final String propertyName;
	
	PropertyCriterion( final String propertyName )
	{
		this.propertyName = checkNotNull( propertyName );
	}
	
	@Override
	public Filter equalsTo( final Object value )
	{
		return new PropertyCompareFilter( Comparison.equals, propertyName, value );
	}

	@Override
	public Filter approx( final Object value )
	{
		return new PropertyCompareFilter( Comparison.approx, propertyName, value );
	}

	@Override
	public Filter greaterOrEquals( final Object value )
	{
		return new PropertyCompareFilter( Comparison.greater, propertyName, value );
	}

	@Override
	public Filter lessOrEquals( final Object value )
	{
		return new PropertyCompareFilter( Comparison.less, propertyName, value );
	}

	@Override
	public Filter present()
	{
		return new PropertyRawCompareFilter( Comparison.equals, propertyName, "*" );
	}

	@Override
	public Filter substrings( final Object... values )
	{
		return new PropertySubstringsWithFilter( propertyName, values );
	}

	@Override
	public Filter startsWith( final Object value )
	{
		return new PropertyStartsWithFilter( propertyName, value );
	}

	@Override
	public Filter endsWith( final Object value )
	{
		return new PropertyEndsWithFilter( propertyName, value );
	}
}
