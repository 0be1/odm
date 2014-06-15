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

import java.util.Optional;

import javax.naming.Name;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

public class EhCacheCache implements PersistentCache {
    private final Ehcache cache;

    public EhCacheCache(final Ehcache cache) {
	this.cache = checkNotNull(cache, "cache is null");
    }

    @Override
    public Optional<Object> store(final Name key, final Object value) {
	final Element element = new Element(checkNotNull(key, "key is null"), checkNotNull(value, "context is null"));

	cache.acquireWriteLockOnKey(key);

	try {
	    final Element prevElement = cache.replace(element);

	    if (prevElement != null) {
		Object prevValue = (Object) prevElement.getObjectValue();

		return Optional.of(prevValue);
	    } else {
		cache.put(element);

		return Optional.empty();
	    }
	} finally {
	    cache.releaseWriteLockOnKey(key);
	}
    }

    @Override
    public Optional<Object> retrieve(Name key) {
	final Element element = cache.get(key);

	if (element != null) {
	    Object value = element.getObjectValue();
	    
	    return Optional.of(value);
	}

	return Optional.empty();
    }

    @Override
    public boolean remove(Name key) {
	return cache.remove(key);
    }

    @Override
    public void clear() {
	cache.flush();
    }

    @Override
    public boolean contains(Name key) {
	return cache.isKeyInCache(key);
    }
}
