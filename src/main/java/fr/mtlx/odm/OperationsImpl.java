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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.annotation.Nonnull;
import javax.naming.Name;
import javax.naming.directory.SearchControls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class OperationsImpl<T> implements Operations<T> {

    public static final String[] RETURN_NO_ATTRIBUTES = new String[] {};
    
    private static final Logger log = LoggerFactory.getLogger(OperationsImpl.class);

    protected final Class<T> persistentClass;

    protected final ClassMetadata<T> metadata;

    private final SessionImpl session;

    public OperationsImpl(final SessionImpl session, final Class<T> persistentClass) {
	this.persistentClass = checkNotNull(persistentClass);

	this.session = checkNotNull(session);

	this.metadata = session.getSessionFactory().getClassMetadata(persistentClass);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T lookup(Name dn) throws javax.naming.NameNotFoundException {
	checkNotNull(dn);

	if (log.isDebugEnabled()) {
	    log.debug("lookup for {}", dn);
	}

	Object retval = getSession().getFromCache(dn).orElseGet(() -> {
	    Object obj = doLookup(dn);

	    getSession().getSessionFactory().getCache().store(dn, obj);

	    return obj;

	});

	if (persistentClass.isInstance(retval)) {
	    return (T) retval;
	}

	throw new ClassCastException(String.format("Cannot cast %s to %s while looking up for %s", retval.getClass(),
		persistentClass, dn));
    }

    @Override
    public T lookupByExample(T example) {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SearchCriteriaImpl<T> search(Name base, SearchControls controls) {
	return new SearchCriteriaImpl<>(this, base, controls);
    }

    @Override
    public SearchCriteriaImpl<T> search(Name base) {
	return new SearchCriteriaImpl<>(this, base);
    }

    public abstract Stream<T> search(final Name base, final SearchControls controls, final String filter)
            throws javax.naming.SizeLimitExceededException;

    public abstract long count(final Name base, final SearchControls controls, final String filter)
	    throws javax.naming.SizeLimitExceededException;

    public abstract Iterable<List<T>> pages(final int pageSize, String filter, Name base, final SearchControls controls);

    public SessionImpl getSession() {
	return session;
    }

    protected final ClassMetadata<T> getMetadata() {
	return metadata;
    }

    // hook
    protected abstract @Nonnull Object doLookup(@Nonnull final Name dn);

    @Override
    public void unbind(final T persistentObject) {
	Name dn;

	dn = new ClassAssistant<T>(metadata).getIdentifier(persistentObject);

	if (!getSession().getCache().contains(dn)) {
	    throw new IllegalArgumentException("not a persistent object");
	}

	doUnbind(dn);

	getSession().getCache().remove(dn);
    }
    
    protected abstract void doUnbind(Name dn);

    protected final void prePersist(final T transientObject) {
	for (final Method method : getMetadata().prepersistMethods()) {
	    try {
		method.invoke(transientObject);
	    } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
	    }
	}
    }
}
