/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.mtlx.odm;

import fr.mtlx.odm.cache.EntityCache;
import fr.mtlx.odm.converters.DefaultConverters;

/**
 *
 * @author alex
 */
@SuppressWarnings("serial")
public class SessionFactory2 extends SessionFactoryImpl {

    public SessionFactory2(Class<?>[] mappedClasses) throws MappingException {
        DefaultConverters.defaultSyntaxConverters
                .entrySet().stream().forEach((entry) -> {
                    addConverter(entry.getKey(), entry.getValue());
                });
        DefaultConverters.defaultAttributeConverters
                .entrySet().stream().forEach((entry) -> {
                    addConverter(entry.getKey(), entry.getValue());
                });

        for (Class<?> clazz : mappedClasses) {
            addClass(clazz);
        }

        initialize();
    }

    @Override
    public Session openSession() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> EntityCache<T> getCacheFor(Class<T> persistentClass) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
