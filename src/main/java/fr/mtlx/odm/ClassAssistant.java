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
import static java.lang.String.format;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import static java.lang.reflect.Modifier.isStatic;
import java.lang.reflect.Type;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.ldap.LdapName;
import static org.apache.commons.beanutils.BeanUtils.getProperty;
import org.apache.commons.beanutils.PropertyUtils;
import static org.apache.commons.beanutils.PropertyUtils.getSimpleProperty;
import static org.springframework.util.ReflectionUtils.doWithFields;

public class ClassAssistant<T> {

    private final ClassMetadata<T> metadata;

    public ClassAssistant(final ClassMetadata<T> metadata) {
        this.metadata = checkNotNull(metadata);
    }

    public void setIdentifier(Object object, Name value) {
        String identifier = metadata.getIdentifierPropertyName();

        try {
            PropertyUtils.setSimpleProperty(object, identifier, value);
        } catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public LdapName getIdentifier(Object object) throws InvalidNameException,
            IllegalAccessException, InvocationTargetException,
            NoSuchMethodException {
        String identifier = metadata.getIdentifierPropertyName();

        return new LdapName(getProperty(object, identifier));
    }

    public boolean isCollection(Type type) {
        Class<?> c = type.getClass();

        return asList(c.getInterfaces()).contains(Collection.class)
                || c.equals(Collection.class);
    }

    public Object getValue(Object object, String propertyName) {
        try {
            return getSimpleProperty(object, propertyName);
        } catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <V> Collection<V> buildCollection(
            final Class<? extends Collection<?>> collectionType,
            final Type componentType, final Collection<V> elements)
            throws MappingException {
        Collection values;

        if (collectionType.equals(List.class)) {
            values = new ArrayList<>(elements);
        } else if (collectionType.equals(Set.class)) {
            return new HashSet<>(elements);
        } else if (collectionType.equals(Collection.class)) {
            values = new ArrayList<>(elements);
        } else {
            throw new MappingException("unsupported collection");
        }

        return values;
    }

    public void setSimpleProperty(final String propertyName, T entry,
            final Object singleValue) throws MappingException {
        try {
            PropertyUtils.setSimpleProperty(entry, propertyName, singleValue);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new MappingException(e);
        }
    }

    public <V> void setProperty(final String propertyName, final T entry,
            final Collection<V> multipleValues) throws MappingException {
        @SuppressWarnings("unchecked")
        Class<T> c = (Class<T>) checkNotNull(entry).getClass();

        final AttributeMetadata meta = metadata
                .getAttributeMetadata(propertyName);

        if (meta == null) {
            throw new MappingException(format(
                    "propertyName: unknown property %s", propertyName));
        }

        if (!meta.isMultivalued()) {
            throw new MappingException(format(
                    "propertyName: single valued property %s", propertyName));
        }

        final Collection<?> targetValues;

        targetValues = buildCollection(meta.getCollectionType(),
                meta.getObjectType(), multipleValues);

        try {
            PropertyUtils.setProperty(entry, propertyName, targetValues);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new MappingException(e);
        } catch (NoSuchMethodException e) {
            try {
                doWithFields(c, (final Field f) -> {
                    boolean secured = !f.isAccessible();

                    if (secured) {
                        f.setAccessible(true);
                    }

                    f.set(entry, targetValues);

                    if (secured) {
                        f.setAccessible(false);
                    }
                }, (final Field field) -> {
                    final int modifiers = field.getModifiers();
                    return !isStatic(modifiers) && (field.getName() == null ? propertyName == null : field.getName().equals(propertyName));
                });
            } catch (SecurityException | IllegalArgumentException e1) {
                throw new MappingException(e1);
            }
        }
    }
}
