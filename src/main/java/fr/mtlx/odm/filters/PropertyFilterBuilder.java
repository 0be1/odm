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
import com.google.common.base.Strings;
import fr.mtlx.odm.AttributeMetadata;
import fr.mtlx.odm.ClassAssistant;
import fr.mtlx.odm.ClassMetadata;
import fr.mtlx.odm.MappingException;
import fr.mtlx.odm.SessionFactoryImpl;
import fr.mtlx.odm.converters.ConvertionException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import javax.naming.InvalidNameException;
import javax.naming.Name;

class PropertyFilterBuilder<T, A> extends SimpleFilterBuilder<T> {

    interface Encoder {

        String encode(Object value);
    }

    private final AttributeMetadata attributeMetadata;

    private final Encoder encoder;

    PropertyFilterBuilder(SessionFactoryImpl sessionFactory, final Class<T> persistentClass, final String property)
            throws MappingException {
        super(getAttributeMetadata(sessionFactory, persistentClass, property).getAttirbuteName());

        attributeMetadata = getAttributeMetadata(sessionFactory, persistentClass, property);

        assert !Strings.isNullOrEmpty(attribute);

        final Type attributeType = attributeMetadata.getObjectType();

        encoder = getEncoder(sessionFactory, attributeType);
    }

    private Encoder getEncoder(SessionFactoryImpl sessionFactory, final Type attributeType) {
        if (attributeType instanceof Class<?>) {
            Class<A> clazz = (Class<A>) attributeType;

            if (sessionFactory.isPersistentClass(clazz)) {
                return dnEncoder(sessionFactory.getClassMetadata(clazz));
            }
        }

        return PropertyFilterBuilder::escapeSpecialChars;
    }

    private static AttributeMetadata getAttributeMetadata(SessionFactoryImpl sessionFactory, Class<?> persistentClass,
            String property) throws MappingException {
        ClassMetadata<?> metadata = sessionFactory.getClassMetadata(checkNotNull(persistentClass));

        if (metadata == null) {
            throw new MappingException(String.format("%s is not a persistent class", persistentClass));
        }

        AttributeMetadata retval = metadata.getAttributeMetadata(property);

        if (retval == null) {
            throw new MappingException(String.format("property %s not found in %s", property, persistentClass));
        }

        return retval;
    }

    @Override
    protected Filter filterType(Comparison op, final Object value) {
        return super.filterType(op, encoder.encode(value));
    }

    private Encoder dnEncoder(final ClassMetadata<A> attributeClassMetadata) {
        return (final Object value) -> {

            if (value == null) {
                return null;
            }

            if (!attributeClassMetadata.getPersistentClass().isInstance(value)) {
                throw new ConvertionException(String.format("wrong type (%s) for property %s in %s, expecting %s",
                        value.getClass(), attributeMetadata.getPropertyName(), attributeMetadata.getObjectType(),
                        attributeClassMetadata.getPersistentClass()));
            }

            Name refdn;
            try {
                refdn = new ClassAssistant<A>(attributeClassMetadata).getIdentifier(value);
            } catch (InvalidNameException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new ConvertionException(e);
            }

            return refdn.toString();
        };
    }
}
