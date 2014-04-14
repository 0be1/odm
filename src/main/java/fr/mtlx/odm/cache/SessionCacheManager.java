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

import java.util.Map;

import com.google.common.collect.Maps;

import fr.mtlx.odm.Session;

public class SessionCacheManager implements CacheManager
{
	private final Map<Class<?>, EntityCache<?>> caches = Maps.newConcurrentMap();

	public final Session session;

	public SessionCacheManager( final Session session )
	{
		this.session = session;
	}

	@Override
	public <T> EntityCache<T> getCacheFor( Class<T> persistentClass )
	{
		if ( !session.getSessionFactory().isPersistentClass( persistentClass ) ) { return null; }
		
		@SuppressWarnings( "unchecked" )
		EntityCache<T> cache = (EntityCache<T>)caches.get( persistentClass );

		if ( cache == null )
		{
			cache = new EntityMapCache<T>( "Session" );

			caches.put( persistentClass, cache );
		}

		return cache;
	}

	@Override
	public void clear( )
	{
		for (EntityCache<?> cache : caches.values() )
		{
			cache.clear();
		}
	}
}
