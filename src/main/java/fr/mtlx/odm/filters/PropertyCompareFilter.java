package fr.mtlx.odm.filters;

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
import fr.mtlx.odm.AttributeMetadata;
import fr.mtlx.odm.ClassAssistant;
import fr.mtlx.odm.ClassMetadata;
import fr.mtlx.odm.SessionFactoryImpl;
import fr.mtlx.odm.converters.ConvertionException;
import java.lang.reflect.InvocationTargetException;
import javax.naming.InvalidNameException;
import javax.naming.Name;

public class PropertyCompareFilter<T> extends FilterImpl {

    protected final String property;

    protected final Object value;

    protected final Comparison op;

    protected final Class<T> persistentClass;

    private final Filter filter;

    PropertyCompareFilter(SessionFactoryImpl sessionFactory, final Class<T> persistantClass, final Comparison op,
            final String property, final Object value) {
        super(sessionFactory);
        this.property = checkNotNull(property);
        this.value = value;
        this.op = op;

        this.persistentClass = checkNotNull(persistantClass);

        filter = composeFilter();
    }

    @Override
    public String encode() {
        return filter.encode();
    }

    protected final Filter composeFilter() {
        final ClassMetadata<T> metadata = getSessionFactory().getClassMetadata(
                persistentClass);

        if (metadata == null) {
            throw new UnsupportedOperationException(String.format(
                    "%s is not a persistent class", persistentClass));
        }

        final AttributeMetadata attribute = metadata
                .getAttributeMetadata(property);

        if (attribute == null) {
            throw new UnsupportedOperationException(String.format(
                    "property %s not found in %s", property,
                    checkNotNull(metadata).getPersistentClass()));
        }

        return new RawCompareFilter(getSessionFactory(), op, attribute.getAttirbuteName(),
                formatValue(encodeValue(value, attribute)));
    }

    protected String formatValue(final String encodedValue) {
        return FilterEncoder.encode(encodedValue);
    }

    protected String encodeValue(final Object value,
            final AttributeMetadata attribute) {
        if (value != null) {
            if (attribute.getObjectType() instanceof Class<?>) {
                Class<?> clazz = (Class<?>) attribute.getObjectType();

                if (!clazz.isInstance(value)) {
                    throw new ConvertionException(
                            String.format(
                                    "wrong type (%s) for property %s in %s, expecting %s",
                                    value.getClass(), property,
                                    attribute.getObjectType(), clazz));
                }

                ClassMetadata<?> refmetadata = getSessionFactory()
                        .getClassMetadata(clazz);

                if (refmetadata != null) {
                    @SuppressWarnings({"rawtypes", "unchecked"})
                    ClassAssistant<?> assistant = new ClassAssistant(
                            refmetadata);

                    Name refdn;
                    try {
                        refdn = assistant.getIdentifier(value);
                    } catch (InvalidNameException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        throw new ConvertionException(e);
                    }

                    return refdn.toString();
                }
            }

            return value.toString();
        }

        return null;
    }
}
