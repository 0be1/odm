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
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

import fr.mtlx.odm.cache.PersistentCache;
import fr.mtlx.odm.filters.FilterBuilder;

public interface SessionFactory extends Serializable {

    boolean isPersistentClass(final String className);
    
    /**
     * clazz est persistente si c'est une super classe d'une classe mappée
     *
     * @param clazz
     * @return true if clazz is persistent
     */
    boolean isPersistentClass(final Class<?> clazz);

    Session openSession();

    void closeSession();

    <T> void addClass(final Class<T> persistentClass) throws MappingException;

    void addClass(final String persistentClassName) throws MappingException, ClassNotFoundException;

    Session getCurrentSession();

    <T> FilterBuilder<T> filterBuilder(Class<T> persistentClass) throws MappingException;
    
    boolean isOperationalAttribute(String attributeId);

    <T> ClassMetadata<T> getClassMetadata(Class<T> entityClass);

    ClassMetadata<?> getClassMetadata(String entityClassName);

    ClassMetadata<?> getClassMetadata(final String[] objectClasses)
            throws IllegalAccessException, InvocationTargetException,
            ClassNotFoundException;
    
    PersistentCache getCache();
}
