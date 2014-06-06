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
import fr.mtlx.odm.SessionFactoryImpl;

public class PropertyCriterion<T> implements CompareCriterion<T>
{
	private final String propertyName;
	private final Class<T> persistentClass;

	PropertyCriterion( final Class<T> persistentClass, SessionFactoryImpl sessionFactory , final String propertyName )
	{
		this.propertyName = checkNotNull( propertyName );
		
		this.persistentClass = checkNotNull( persistentClass );
	}
	
	@Override
	public Filter equalsTo( final Object value )
	{
		return new PropertyCompareFilter<T>( persistentClass, Comparison.equals, propertyName, value );
	}

	@Override
	public Filter approx( final Object value )
	{
		return new PropertyCompareFilter<T>( persistentClass, Comparison.approx, propertyName, value );
	}

	@Override
	public Filter greaterOrEquals( final Object value )
	{
		return new PropertyCompareFilter<T>( persistentClass, Comparison.greater, propertyName, value );
	}

	@Override
	public Filter lessOrEquals( final Object value )
	{
		return new PropertyCompareFilter<T>( persistentClass, Comparison.less, propertyName, value );
	}

	@Override
	public Filter present()
	{
		return new PropertyRawCompareFilter<T>( persistentClass, Comparison.equals, propertyName, "*" );
	}

	@Override
	public Filter substrings( final Object... values )
	{
		return new PropertySubstringsWithFilter<T>( persistentClass, propertyName, values );
	}

	@Override
	public Filter startsWith( final Object value )
	{
		return new PropertyStartsWithFilter<T>( persistentClass, propertyName, value );
	}

	@Override
	public Filter endsWith( final Object value )
	{
		return new PropertyEndsWithFilter<T>( persistentClass, propertyName, value );
	}
}
