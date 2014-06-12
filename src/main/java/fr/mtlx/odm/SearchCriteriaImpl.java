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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import static fr.mtlx.odm.SessionImpl.getDefaultSearchControls;
import fr.mtlx.odm.filters.Filter;
import fr.mtlx.odm.filters.FilterBuilder;
import fr.mtlx.odm.filters.FilterBuilderImpl;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.naming.Name;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.SearchControls;
import javax.persistence.NonUniqueResultException;

public class SearchCriteriaImpl<T> implements SearchCriteria<T> {

    private final List<Filter> filterStack = Lists.newLinkedList();

    protected final Name base;

    protected final SearchControls controls;

    protected final OperationsImpl<T> ops;

    private final Map<String, Collection> projections = Maps.newHashMap();

    public SearchCriteriaImpl(final OperationsImpl<T> ops, final Name root) {
        this(ops, root, getDefaultSearchControls());
    }

    public SearchCriteriaImpl(final OperationsImpl<T> ops, final Name root, final SearchControls controls) {
        this.controls = checkNotNull(controls);

        this.ops = checkNotNull(ops);

        this.base = checkNotNull(root);
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
            AttributeMetadata attr = ops.metadata
                    .getAttributeMetadata(propertyName);

            if (attr == null) {
                throw new UnsupportedOperationException(String.format(
                        "property %s not found in %s", propertyName,
                        ops.metadata.getPersistentClass()));
            }

            attrs.add(attr.getAttirbuteName());
        }

        controls.setReturningAttributes(attrs.toArray(new String[]{}));

        normalizeControls(controls);

        return this;
    }

    @Override
    public Iterable<List<T>> pages(final int pageSize) {
        return ops.pages(pageSize, encodeFilter(), base, controls);
    }

    protected String encodeFilter() {
        final FilterBuilder<T> fb;

        try {
            fb = new FilterBuilderImpl<>(ops.persistentClass,
                    ops.getSession().getSessionFactory());
        } catch (MappingException ex) {
            return null;
        }
        
        filterStack.stream().forEach((f) -> {
            fb.and(f);
        });

        return fb.toString();

    }

    private void normalizeControls(SearchControls controls) {
        Set<String> attributes = Sets.newLinkedHashSet(Arrays.asList(controls
                .getReturningAttributes()));

        attributes.add("objectClass");

        controls.setReturningAttributes(attributes.toArray(new String[]{}));
    }

    @Override
    public List<T> list() throws javax.naming.SizeLimitExceededException {
        List<T> results = ops.search(base, controls, encodeFilter()).collect(Collectors.toCollection(ArrayList::new));

        projections(results);

        return results;
    }

    @Override
    public void nop() throws javax.naming.SizeLimitExceededException {
        projections(ops.search(base, controls, encodeFilter()).collect(Collectors.toCollection(ArrayList::new)));
    }

    @SuppressWarnings("unchecked")
    private void projections(List<T> results) {
        for (String property : projections.keySet()) {
            final AttributeMetadata t = ops.metadata
                    .getAttributeMetadata(property);

            if (t == null) {
                throw new UnsupportedOperationException(String.format(
                        "property %s not found in %s", property,
                        ops.metadata.getPersistentClass()));
            }

            Class<?> c = (Class<?>) t.getObjectType(); // XXX Cast ??

            ClassAssistant<T> assistant = new ClassAssistant<>(ops.metadata);

            for (T item : results) {
                final Object value = assistant.getValue(item, property);

                if (value != null) {
                    if (!c.isInstance(value)) {
                        throw new UnsupportedOperationException(String.format(
                                "property %s found wrong type %s", property, t));
                    }

                    projections.get(property).add(value);
                }
            }
        }
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
    public <C> SearchCriteriaImpl<T> addProjection(final Collection<C> collection,
            final String property) {
        this.projections.put(checkNotNull(property), checkNotNull(collection));

        return this;
    }

    @Override
    public SearchCriteriaImpl<T> example(T example) {
        filterStack.add(ops.metadata.getByExampleFilter());

        return this;
    }
}
