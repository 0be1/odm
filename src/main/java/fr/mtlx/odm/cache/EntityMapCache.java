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

import javax.naming.Name;

public class EntityMapCache<T> extends NameKeyCache<T> implements
		EntityCache<T> {
	private final String region;

	public EntityMapCache(final String region) {
		this.region = region;
	}

	@Override
	public T store(Name key, T context) {
		T retval = super.store(key, context);

		if (log.isDebugEnabled()) {
			log.debug("storing {} in {}", key, region);
		}

		return retval;
	}

	@Override
	public T retrieve(Name key) {
		T retval = super.retrieve(key);

		if (log.isDebugEnabled()) {
			if (retval != null) {
				log.debug("cache hit for {} in {}", key, region);
			} else {
				log.debug("cache miss for {} in {}", key, region);
			}
		}

		return retval;
	}

	@Override
	public T remove(Name key) {
		if (log.isDebugEnabled()) {
			log.debug("removing {} from {}", key, region);
		}

		return super.remove(key);
	}
}
