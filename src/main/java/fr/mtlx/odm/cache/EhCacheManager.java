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

import net.sf.ehcache.Ehcache;
import fr.mtlx.odm.ClassMetadata;
import fr.mtlx.odm.SessionFactory;
import fr.mtlx.odm.SessionFactoryImpl;

public class EhCacheManager implements CacheManager
{
	private final String regionName;
	private final net.sf.ehcache.CacheManager cacheManager;

	public final SessionFactoryImpl sessionFactory;

	public EhCacheManager( final SessionFactoryImpl sessionFactory, final net.sf.ehcache.CacheManager cacheManager, final String regionName )
	{
		this.sessionFactory = sessionFactory;
		this.cacheManager = cacheManager;
		this.regionName = regionName;
	}

	private String cacheName( final Class<?> persistentClass )
	{
		return regionName + '#' + persistentClass.getName();

	}

	private Ehcache getCache( final Class<?> persistentClass )
	{
		final String name = cacheName( persistentClass );

		final Ehcache cache = cacheManager.getEhcache( cacheName( persistentClass ) );

		if ( cache != null ) { return cache; }

		final Ehcache memoryOnlyCache = new net.sf.ehcache.Cache( name, 5000, false, false, 5, 2 );

		cacheManager.addCache( memoryOnlyCache );

		return cacheManager.getEhcache( name );
	}

	@Override
	public <T> EntityCache<T> getCacheFor( Class<T> persistentClass )
	{
		final ClassMetadata<T> metaData = sessionFactory.getClassMetadata( persistentClass );

		if ( metaData == null || !metaData.isCacheable() ) { return new NoCache<T>(); }

		return new EntityEhCache<T>( getCache( metaData.getClass() ) );
	}

	@Override
	public void clear()
	{
		final String prefix = regionName + '#';
		
		for (String name : cacheManager.getCacheNames())
		{
			if (name.startsWith( prefix ))
			{
				cacheManager.getEhcache( name ).flush();
			}
		}
	}

}
