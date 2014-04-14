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

import com.google.common.base.Joiner;

public class AttributeCriterion implements CompareCriterion
{
	private final String attributeName;
	
	AttributeCriterion( final String propertyName )
	{
		this.attributeName = checkNotNull( propertyName );
	}
	
	@Override
	public Filter equalsTo( Object value )
	{
		return new AttributeFilter( Comparison.equals, attributeName, value );
	}

	@Override
	public Filter approx( Object value )
	{
		return new AttributeFilter( Comparison.approx, attributeName, value );
	}

	@Override
	public Filter greaterOrEquals( Object value )
	{
		return new AttributeFilter( Comparison.greater, attributeName, value );
	}

	@Override
	public Filter lessOrEquals( Object value )
	{
		return new AttributeFilter( Comparison.less, attributeName, value );
	}

	@Override
	public Filter present()
	{
		return new RawCompareFilter( Comparison.equals, attributeName, "*" );
	}

	@Override
	public Filter substrings( Object... values )
	{
		return new RawCompareFilter( Comparison.equals, attributeName, Joiner.on( '*' ).useForNull( "" ).join( values ) );
	}

	@Override
	public Filter startsWith( Object value )
	{
		return new RawCompareFilter( Comparison.equals, attributeName, "*" + value );
	}

	@Override
	public Filter endsWith( Object value )
	{
		return new RawCompareFilter( Comparison.equals, attributeName, value + "*" );
	}
}
