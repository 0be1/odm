package fr.mtlx.odm.cache;

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

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

public class EntityEhCache<T> implements EntityCache<T>
{
	private final Ehcache cache;

	public EntityEhCache( final Ehcache cache )
	{
		this.cache = checkNotNull( cache, "cache is null");
	}

	@Override
	public T store( Name key, T context )
	{
		cache.put( new Element( checkNotNull( key, "key is null" ), checkNotNull( context, "context is null" ) ) );
		
		return context;
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public T retrieve( Name key )
	{
		final Element element = cache.get( key );
		
		if (element != null) 
		{
			return (T)element.getObjectValue();
		}
		
		return null;
	}

	@Override
	public T remove( Name key )
	{
		@SuppressWarnings( "unchecked" )
		T retval = (T)cache.get( key );
		
		if ( cache.remove( key ) )
			return retval;
		else
			return null;
	}

	@Override
	public void clear()
	{
		cache.flush();
	}

	@Override
	public boolean contains( Name key )
	{
		return cache.isKeyInCache( key );
	}
}
