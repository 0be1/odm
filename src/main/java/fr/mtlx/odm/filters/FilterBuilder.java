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


public abstract class FilterBuilder
{
	public static OrFilter or( Filter... filters )
	{
		return new OrFilter( filters );
	}

	public static AndFilter and( Filter... filters )
	{
		return new AndFilter( filters );
	}
	
	public static CompareCriterion property( String propertyName )
	{
		return new PropertyCriterion( propertyName );
	}
	
	public static Filter objectClass( final String objectClass )
	{
		return new ObjectClassFilter( objectClass );
	}
	
	public static Filter not( final Filter filter )
	{
		return new NotFilter( filter );
	}
	
	public static CompareCriterion attribute( final String attributeName )
	{
		return new AttributeCriterion( attributeName );
	}
}
