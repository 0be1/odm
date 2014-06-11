/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.mtlx.odm;

import fr.mtlx.odm.filters.Filter;
import fr.mtlx.odm.filters.FilterBuilder;
import java.util.Collection;
import java.util.List;
import javax.naming.Name;
import javax.naming.SizeLimitExceededException;
import javax.persistence.NonUniqueResultException;

/**
 *
 * @author alex
 * @param <T>
 */
public interface SearchCriteria<T> {

    SearchCriteriaImpl<T> add(Filter filter);
    
    default SearchCriteriaImpl<T> add(FilterBuilder<T> builder) {
        return add(builder.build());
    }

    <C> SearchCriteriaImpl<T> addProjection(final Collection<C> collection, final String property);

    long count() throws SizeLimitExceededException;

    SearchCriteriaImpl<T> countLimit(long limit);

    SearchCriteriaImpl<T> example(T example);

    List<T> list() throws SizeLimitExceededException;

    void nop() throws SizeLimitExceededException;

    Iterable<List<T>> pages(final int pageSize);

    SearchCriteriaImpl<T> properties(String... properties);

    SearchCriteriaImpl<T> scope(int scope);

    SearchCriteriaImpl<T> timeLimit(int ms);

    T unique() throws NonUniqueResultException;

}
