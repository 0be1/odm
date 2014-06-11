package fr.mtlx.odm.filters;

import fr.mtlx.odm.MappingException;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

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
public interface FilterBuilder<T> {

    FilterBuilder<T> or(Stream<Filter> filters);
    
    default FilterBuilder<T> or(Collection<Filter> filters) {
        return or(filters.stream());
    }

    default FilterBuilder<T> or(Filter... filters) {
        return or(Arrays.asList(filters));
    }

    FilterBuilder<T> and(Stream<Filter> filters);

    default FilterBuilder<T> and(Collection<Filter> filters) {
        return and(filters.stream());
    }

    default FilterBuilder<T> and(Filter... filters) {
        return and(Arrays.asList(filters));
    }

    default Filter objectClass(String objectClass) {
        return SimpleFilterBuilder.objectClass(objectClass);
    }

    default Filter not(Filter filter) {
        return new NotFilter(filter);
    }
    
    default CompareCriterion<T> attribute(String attributeName) {
        return new SimpleFilterBuilder<>(attributeName);
    }

    CompareCriterion<T> property(String propertyName) throws MappingException;

    Filter build();
}
