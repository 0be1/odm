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

import fr.mtlx.odm.ClassMetadata;
import fr.mtlx.odm.SessionFactoryImpl;

public class BaseCacheManager implements CacheManager {
	private final Map<Class<?>, EntityCache<?>> caches = Maps
			.newConcurrentMap();

	public final SessionFactoryImpl sessionFactory;

	public BaseCacheManager(final SessionFactoryImpl sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public <T> EntityCache<T> getCacheFor(Class<T> persistentClass) {
		ClassMetadata<T> metaData = sessionFactory
				.getClassMetadata(persistentClass);

		if (metaData == null || !metaData.isCacheable()) {
			return new NoCache<T>();
		}

		@SuppressWarnings("unchecked")
		EntityCache<T> cache = (EntityCache<T>) caches.get(persistentClass);

		if (cache == null) {
			cache = new EntityMapCache<T>(persistentClass.getName());

			caches.put(persistentClass, cache);
		}

		return cache;
	}

	@Override
	public void clear() {
		for (EntityCache<?> cache : caches.values()) {
			cache.clear();
		}
	}
}
