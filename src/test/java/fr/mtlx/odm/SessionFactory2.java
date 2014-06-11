/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.mtlx.odm;

import java.lang.reflect.Type;
import java.util.Map.Entry;

import fr.mtlx.odm.cache.EntityCache;
import fr.mtlx.odm.converters.Converter;
import fr.mtlx.odm.converters.DefaultConverters;

/**
 *
 * @author alex
 */
@SuppressWarnings("serial")
public class SessionFactory2 extends SessionFactoryImpl {

    public SessionFactory2(Class<?>[] mappedClasses) throws MappingException {
        for( Entry<String, Converter> entry : DefaultConverters.defaultSyntaxConverters.entrySet()) {
            addConverter(entry.getKey(), entry.getValue());
        }
        
        for( Entry<Type, Converter> entry : DefaultConverters.defaultAttributeConverters.entrySet()) {
            addConverter(entry.getKey(), entry.getValue());
        }

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