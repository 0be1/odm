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

import java.util.Map;
import java.util.Optional;

import javax.naming.Name;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

public abstract class NameKeyCache<T> implements Cache<T, Name> {

    private final Map<String, Optional<T>> cacheMap;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public NameKeyCache() {
        cacheMap = Maps.newConcurrentMap();
    }

    @Override
    public Optional<T> store(final Name key, final Optional<T> value) {
        return cacheMap.put(getKey(key), checkNotNull(value));
    }

    @Override
    public Optional<T> retrieve(final Name key) {
        return cacheMap.get(getKey(key));
    }

    @Override
    public boolean remove(Name key) {
        return cacheMap.remove(getKey(key)) != null;
    }

    @Override
    public void clear() {
        cacheMap.clear();
    }

    @Override
    public boolean contains(Name key) {
        return cacheMap.containsKey(getKey(key));
    }

    protected String getKey(final Name dn) {
        return dn.toString().toLowerCase();
    }
}
