package fr.mtlx.odm;

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
import com.google.common.collect.Maps;
import fr.mtlx.odm.cache.Cache;
import fr.mtlx.odm.cache.CacheManager;
import fr.mtlx.odm.cache.EntityCache;
import fr.mtlx.odm.cache.EntityMapCache;
import fr.mtlx.odm.converters.Converter;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.naming.Name;
import javax.naming.directory.SearchControls;

public abstract class SessionImpl implements Session, CacheManager {

    public static SearchControls copySearchControls(final SearchControls controls) {
        final SearchControls retval = new SearchControls();

        retval.setCountLimit(controls.getCountLimit());
        retval.setDerefLinkFlag(controls.getDerefLinkFlag());
        retval.setReturningObjFlag(false);
        retval.setSearchScope(controls.getSearchScope());
        retval.setTimeLimit(controls.getTimeLimit());

        return retval;
    }

    public static SearchControls getDefaultSearchControls() {
        final SearchControls retval = new SearchControls();

        retval.setSearchScope(SearchControls.SUBTREE_SCOPE);

        retval.setReturningObjFlag(false);

        return retval;
    }

    private final Map<Class<?>, EntityCache<?>> caches = Maps.newConcurrentMap();

    @Override
    public boolean isPersistent(final Object obj) {
        final Name dn;

        if (obj == null) {
            return false;
        }

        Cache cache = getCacheFor(obj.getClass());

        if (cache == null) {
            return false;
        }

        return cache.contains(obj);
    }

    @Override
    public void close() {
        clear();
    }

    public Converter getSyntaxConverter(final String syntax) throws MappingException {
        final Converter converter = getSessionFactory().getConverter(syntax);
        if (converter == null) {
            throw new MappingException(String.format("no converter found for syntax %s", syntax));
        }
        return converter;
    }

    private <T> void removeFromCache(final Class<T> persistentClass, final Name dn) {
        if (getSessionFactory() instanceof CacheManager) {
            ((CacheManager) getSessionFactory()).getCacheFor(persistentClass)
                    .remove(dn);
        }

        getCacheFor(persistentClass).remove(dn);
    }

    @Override
    public <T> EntityCache<T> getCacheFor(@Nonnull Class<T> persistentClass) {
        if (!getSessionFactory().isPersistentClass(persistentClass)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        EntityCache<T> cache = (EntityCache<T>) caches.get(persistentClass);
        if (cache == null) {
            cache = new EntityMapCache<>("Session");
            caches.put(persistentClass, cache);
        }
        return cache;
    }

    @Override
    public void clear() {
        caches.values().stream().forEach((cache) -> {
            cache.clear();
        });
    }

    @Override
    public abstract SessionFactoryImpl getSessionFactory();

}
