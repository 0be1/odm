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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.SearchControls;
import javax.persistence.NonUniqueResultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.mtlx.odm.cache.TypeSafeCache;
import fr.mtlx.odm.filters.Filter;
import fr.mtlx.odm.filters.FilterBuilder;
import fr.mtlx.odm.filters.FilterBuilderImpl;

public class SearchCriteriaImpl<T> implements SearchCriteria<T> {

    private final List<Filter> filterStack = Lists.newLinkedList();

    private Name base;

    private SearchControls controls;

    private final Map<String, Collection> projections = Maps.newHashMap();

    private final OperationsImplementation<T> ops;

    private final SessionImpl session;

    private final ClassMetadata<T> metadata;

    private final Class<T> persistentClass;

    private final TypeSafeCache<T> entryCache;

    private static final Logger log = LoggerFactory.getLogger(SearchCriteriaImpl.class);

    public SearchCriteriaImpl(final SessionImpl session, final Class<T> persistentClass) {
        this.session = checkNotNull(session);

        this.persistentClass = checkNotNull(persistentClass);

        entryCache = new TypeSafeCache<>(persistentClass, session.getCache());

        metadata = session.getSessionFactory().getClassMetadata(persistentClass);
        
        ops = session.getImplementor(persistentClass);
    }

    @Override
    public SearchCriteriaImpl<T> scope(int scope) {
        controls.setSearchScope(scope);

        return this;
    }

    @Override
    public SearchCriteriaImpl<T> add(Filter filter) {
        filterStack.add(checkNotNull(filter));

        return this;
    }

    @Override
    public SearchCriteriaImpl<T> add(FilterBuilder<T> builder) {
        return add(builder.build());
    }

    @Override
    public long count() throws SizeLimitExceededException {
        return ops.count(base, controls, encodeFilter());
    }

    @Override
    public SearchCriteriaImpl<T> countLimit(long limit) {
        controls.setCountLimit(limit);

        return this;
    }

    @Override
    public SearchCriteriaImpl<T> timeLimit(int ms) {
        controls.setTimeLimit(ms);

        return this;
    }

    @Override
    public SearchCriteriaImpl<T> properties(String... properties) {
        final List<String> attrs = Lists.newArrayList();

        for (String propertyName : properties) {
            AttributeMetadata attr = metadata.getAttributeMetadata(propertyName);

            if (attr == null) {
                throw new UnsupportedOperationException(String.format("property %s not found in %s", propertyName,
                        metadata.getPersistentClass()));
            }

            attrs.add(attr.getAttirbuteName());
        }

        controls.setReturningAttributes(attrs.toArray(new String[] {}));

        normalizeControls(controls);

        return this;
    }

    @Override
    public Iterable<List<T>> pages(final int pageSize) throws SizeLimitExceededException {
        return ops.pages(pageSize, encodeFilter(), base, controls);
    }

    @Override
    public List<T> list() throws javax.naming.SizeLimitExceededException {
        List<T> results = Lists.newArrayList(ops.doSearch(base, controls, encodeFilter()));

        for (T entry : results) {
            try {
                metadata.getIdentifier().get(entry);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        projections(results);

        return results;
    }

    @Override
    public void nop() throws javax.naming.SizeLimitExceededException {
        projections(ops.doSearch(base, controls, encodeFilter()));
    }

    @Override
    public T unique() throws NonUniqueResultException {
        List<T> results;

        long countLimit = controls.getCountLimit();

        controls.setCountLimit(1);

        try {
            results = list();
        } catch (javax.naming.SizeLimitExceededException e) {
            throw new NonUniqueResultException(e.getMessage());
        } finally {
            controls.setCountLimit(countLimit);
        }

        projections(results);

        return Iterables.getOnlyElement(results, null);
    }

    @Override
    public <C> SearchCriteriaImpl<T> addProjection(final Collection<C> collection, final String property) {
        this.projections.put(checkNotNull(property), checkNotNull(collection));

        return this;
    }

    @Override
    public SearchCriteriaImpl<T> example(T example) {
        filterStack.add(metadata.getByExampleFilter());

        return this;
    }

    @Override
    public void bind(T transientObject) throws NameNotFoundException {
        try {
            prePersist(checkNotNull(transientObject));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new NameNotFoundException(e.getLocalizedMessage());
        }

        final Name dn = ops.doBind(transientObject);

        entryCache.store(dn, transientObject);
    }

    @Override
    public T lookup(Name dn) throws javax.naming.NameNotFoundException {
        checkNotNull(dn);

        if (log.isDebugEnabled()) {
            log.debug("looking up for {}", dn);
        }

        final T retval = session.getFromCacheStack(persistentClass, dn).orElse(ops.doLookup(dn));

        entryCache.store(dn, retval);

        session.getSessionFactory().getCache().store(dn, retval);
        
        return retval;
    }

    @Override
    public void modify(T persistentObject) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public T lookupByExample(T example) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SearchCriteriaImpl<T> setControls(SearchControls controls) {

        this.controls = checkNotNull(controls);

        return this;
    }

    @Override
    public void unbind(final T persistentObject) throws NameNotFoundException {
        final Name dn = new ClassAssistant<T>(metadata).getIdentifier(persistentObject);

        if (!session.getCache().contains(dn)) {
            throw new IllegalArgumentException("not a persistent object");
        }

        ops.doUnbind(dn);

        session.getCache().remove(dn);
    }

    @Override
    public SearchCriteriaImpl<T> setBase(final Name base) {

        this.base = base;

        return this;
    }

    private final void prePersist(final T transientObject) throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        for (final Method method : metadata.prepersistMethods()) {
            method.invoke(transientObject);
        }
    }

    private String encodeFilter() {
        final FilterBuilder<T> fb;

        try {
            fb = new FilterBuilderImpl<>(persistentClass, session.getSessionFactory());
        } catch (MappingException ex) {
            return null;
        }

        for (Filter f : filterStack) {
            fb.and(f);
        }

        return fb.toString();

    }

    private void normalizeControls(SearchControls controls) {
        Set<String> attributes = Sets.newLinkedHashSet(Arrays.asList(controls.getReturningAttributes()));

        attributes.add("objectClass");

        controls.setReturningAttributes(attributes.toArray(new String[] {}));
    }

    private void projections(Iterable<T> results) {
        for (String property : projections.keySet()) {
            final AttributeMetadata t = metadata.getAttributeMetadata(property);

            if (t == null) {
                throw new UnsupportedOperationException(String.format("property %s not found in %s", property,
                        metadata.getPersistentClass()));
            }

            Class<?> c = (Class<?>) t.getObjectType(); // XXX Cast ??

            ClassAssistant<T> assistant = new ClassAssistant<>(metadata);

            for (T item : results) {
                final Object value = assistant.getValue(item, property);

                if (value != null) {
                    if (!c.isInstance(value)) {
                        throw new UnsupportedOperationException(String.format("property %s found wrong type %s", property, t));
                    }

                    projections.get(property).add(value);
                }
            }
        }
    }
}
