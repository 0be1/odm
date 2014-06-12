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
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

import javax.naming.Name;
import javax.naming.directory.SearchControls;

import fr.mtlx.odm.cache.NoCache;
import fr.mtlx.odm.cache.PersistentCache;
import fr.mtlx.odm.converters.Converter;

public abstract class SessionImpl implements Session {

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

    private final PersistentCache cache;

    public PersistentCache getCache() {
	return cache;
    }

    public SessionImpl(final CacheFactory cacheFactory) {
	cache = Optional.ofNullable(checkNotNull(cacheFactory).getCache()).orElse(new NoCache());
    }

    @Override
    public <T> boolean isPersistent(final T obj) {
	if (obj == null) {
	    return false;
	}

	Optional<Name> dn = extractDn(obj);

	if (dn.isPresent())
	    return cache.contains(dn.get());
	else
	    return false;
    }

    @Override
    public void close() {
	getCache().clear();
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<Name> extractDn(T obj) {
	final ClassMetadata<T> metadata = (ClassMetadata<T>) getSessionFactory().getClassMetadata(obj.getClass());

	if (metadata == null) {
	    return Optional.empty();
	}

	return Optional.ofNullable(new ClassAssistant<T>(metadata).getIdentifier(obj));
    }

    public final Optional<Object> getFromCache(final Name dn) {
	final PersistentCache sessionCache = getCache();

	if (sessionCache.contains(dn)) {
	    return sessionCache.retrieve(dn);
	}

	final PersistentCache secondLevelcache = getSessionFactory().getCache();

	if (secondLevelcache != null) {
	    Optional<Object> entry = secondLevelcache.retrieve(dn);

	    if (entry.isPresent()) {
		sessionCache.store(dn, entry.get());

		return entry;
	    }
	}

	return Optional.empty();
    }

    public Converter getSyntaxConverter(final String syntax) throws MappingException {
	final Converter converter = getSessionFactory().getConverter(syntax);

	if (converter == null) {
	    throw new MappingException(String.format("no converter found for syntax %s", syntax));
	}

	return converter;
    }

    @Override
    public abstract SessionFactoryImpl getSessionFactory();
}
