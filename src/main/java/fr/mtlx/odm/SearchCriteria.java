/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.mtlx.odm;

import java.util.Collection;
import java.util.List;

import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.SearchControls;
import javax.persistence.NonUniqueResultException;

import fr.mtlx.odm.filters.Filter;
import fr.mtlx.odm.filters.FilterBuilder;

public interface SearchCriteria<T> {

    SearchCriteriaImpl<T> add(Filter filter);
    
    SearchCriteriaImpl<T> add(FilterBuilder<T> builder);

    <C> SearchCriteriaImpl<T> addProjection(final Collection<C> collection, final String property);

    long count() throws SizeLimitExceededException;

    SearchCriteriaImpl<T> countLimit(long limit);

    SearchCriteriaImpl<T> example(T example);

    List<T> list() throws SizeLimitExceededException;

    void nop() throws SizeLimitExceededException;

    Iterable<List<T>> pages(final int pageSize) throws SizeLimitExceededException;

    SearchCriteriaImpl<T> properties(String... properties);

    SearchCriteriaImpl<T> scope(int scope);

    SearchCriteriaImpl<T> timeLimit(int ms);

    T unique() throws NonUniqueResultException;
    
    SearchCriteriaImpl<T> setControls(SearchControls controls);

    T lookupByExample(T example);

    void modify(T persistentObject);

    void unbind(T persistentObject) throws NameNotFoundException;

    SearchCriteriaImpl<T> setBase(Name base);

    T lookup(Name dn) throws NameNotFoundException;

    void bind(T transientObject) throws NameNotFoundException;

}
