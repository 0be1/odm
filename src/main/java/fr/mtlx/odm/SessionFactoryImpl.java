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
import fr.mtlx.odm.filters.FilterBuilderImpl;
import com.google.common.base.Joiner;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fr.mtlx.odm.cache.CacheManager;
import fr.mtlx.odm.converters.Converter;
import fr.mtlx.odm.filters.FilterBuilder;
import fr.mtlx.odm.spring.SpringProxyFactory;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.ldap.Rdn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public abstract class SessionFactoryImpl implements SessionFactory, CacheManager {

    public static final ThreadLocal<Session> session = new ThreadLocal<>();

    public Set<String> operationalAttributes = Sets.newHashSet("objectClass");

    private final Map<Class<?>, PartialClassMetadata<?>> persistentMetadata = Maps.newLinkedHashMap();

    private final Map<Class<?>, SpringProxyFactory<?>> proxyFactories = Maps.newConcurrentMap();

    private final Map<String, Converter> syntaxConverters = Maps.newConcurrentMap();

    private final Map<Type, Converter> attributeConverters = Maps.newConcurrentMap();

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    @SuppressWarnings("unchecked")
    public <T> ClassMetadata<T> getClassMetadata(Class<T> entityClass) {
        return (ClassMetadata<T>) persistentMetadata.get(checkNotNull(entityClass));
    }

    @Override
    public ClassMetadata<?> getClassMetadata(String persistentClassName) {
        checkNotNull(persistentClassName);
        try {
            final Class<?> searchKey = Class.forName(persistentClassName);

            return getClassMetadata(searchKey);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    @Override
    public <T> void addClass(Class<T> persistentClass) throws MappingException {
        checkNotNull(persistentClass, "persistentClass is null");

        if (persistentMetadata.containsKey(persistentClass)) {
            log.warn("class {} already mapped", persistentClass);
            return;
        }

        persistentMetadata.put(persistentClass, ClassMetadataBuilder.build(persistentClass));

        addProxyFactory(persistentClass);
    }

    @Override
    public void addClass(String persistentClassName) throws ClassNotFoundException, MappingException {

        final Class<?> persistentClass = Class.forName(checkNotNull(persistentClassName, "persistentClassName is null"));

        addClass(persistentClass);
    }

    @Override
    public ClassMetadata<?> getClassMetadata(final String[] objectClasses)
            throws IllegalAccessException, InvocationTargetException,
            ClassNotFoundException {
        final Set<String> directoryObjectClasses = ImmutableSet.copyOf(objectClasses);

        ClassMetadata<?> metadata = null;
        int Q = 0;

        // filtre sur la classe structurelle
        for (Type entryClass : persistentMetadata.keySet()) {
            final ClassMetadata<?> candidateMetadata = persistentMetadata
                    .get(entryClass);

            final Set<String> oc = new ImmutableSet.Builder<String>()
                    .addAll(candidateMetadata.getAuxiliaryClasses())
                    .add(candidateMetadata.getStructuralClass()).build();

            log.debug(
                    "trying {} ( {}, {} )",
                    new Object[]{
                        entryClass,
                        candidateMetadata.getStructuralClass(),
                        Joiner.on(", ")
                        .skipNulls()
                        .join(candidateMetadata
                                .getAuxiliaryClasses())});

            if (directoryObjectClasses.containsAll(oc)) {
                final int q = oc.size();

                log.debug("match q = {}", q);

                if (q > Q) {
                    Q = q;

                    if (metadata != null) {
                        if (metadata.getPersistentClass().isAssignableFrom(
                                candidateMetadata.getPersistentClass())) {
                            metadata = candidateMetadata;
                        }
                    } else {
                        metadata = candidateMetadata;
                    }
                }
            }

            if (Q == directoryObjectClasses.size()) {
                break;
            }
        }
        return metadata;
    }

    @Override
    public boolean isPersistentClass(String className) {
        try {
            return isPersistentClass(Class.forName(className));
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    @Override
    public boolean isPersistentClass(Class<?> clazz) {
        checkNotNull(clazz);

        return persistentMetadata.keySet().stream().anyMatch(t -> t.isAssignableFrom(clazz));
    }

    @Override
    public Session getCurrentSession() {
        Session s = session.get();

        // Ouvre une nouvelle Session, si ce Thread n'en a aucune
        if (s == null) {
            s = openSession();
            session.set(s);
        }

        return s;
    }

    @Override
    public void closeSession() {
        final Session s = session.get();

        session.set(null);

        if (s != null) {
            try {
                s.close();
            } catch (IOException ex) {
                log.error("error while closing session", ex);
            }
        }
    }

    @Override
    public boolean isOperationalAttribute(String attributeId
    ) {
        return operationalAttributes.contains(attributeId);
    }

    @Override
    public <T> FilterBuilder<T> filterBuilder(Class<T> persistentClass) throws MappingException {
        
      
        
        return new FilterBuilderImpl<>(persistentClass, this);
    }

    public void addConverter(String syntax, Converter converter) {
        this.syntaxConverters
                .put(checkNotNull(syntax), checkNotNull(converter));
    }

    public void addConverter(Type type, Converter converter) {
        this.attributeConverters.put(checkNotNull(type),
                checkNotNull(converter));
    }

    public Converter getConverter(final String syntax) {
        return syntaxConverters.get(syntax);
    }

    public Converter getConverter(final Type objectType) {
        return attributeConverters.get(objectType);
    }

    public <T> Rdn composeName(T object, String propertyName)
            throws InvalidNameException, MappingException {
        @SuppressWarnings("unchecked")
        ClassMetadata<T> cm = getClassMetadata((Class<T>) object.getClass());

        if (cm == null) {
            throw new MappingException("not a persistent class");
        }

        ClassAssistant<T> assistant = new ClassAssistant<>(cm);
        AttributeMetadata meta = cm
                .getAttributeMetadata(propertyName);

        if (meta == null) {
            throw new MappingException("unknow attribute");
        }

        return new Rdn(meta.getAttirbuteName(), assistant.getValue(object,
                propertyName));
    }

    @SuppressWarnings("unchecked")
    public <T, CTX extends Context> ProxyFactory<T, CTX> getProxyFactory(Class<T> clazz,
            Class<?>[] interfaces) {
        return (ProxyFactory<T, CTX>) proxyFactories.get(clazz);
    }

    protected void initialize() {
        persistentMetadata
                .values().stream().forEach((metadata) -> {
                    metadata.init(this);
                });
    }

    private <T> void addProxyFactory(Class<T> clazz) {
        proxyFactories.put(clazz, new SpringProxyFactory<>(clazz, new Class<?>[]{}));
    }

    public void setOperationalAttributes(Set<String> operationalAttributes) {
        this.operationalAttributes = operationalAttributes;
    }
}
