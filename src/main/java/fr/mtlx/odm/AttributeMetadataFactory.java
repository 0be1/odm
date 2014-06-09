package fr.mtlx.odm;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import javax.naming.ldap.LdapName;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import fr.mtlx.odm.converters.Converter;
import fr.mtlx.odm.converters.DefaultConverters;
import static java.lang.String.format;
import static java.util.Arrays.asList;

public class AttributeMetadataFactory {

    private static String guessSyntaxFromType(Type type) {
        return DefaultConverters.defaultSyntaxes.get(type);
    }
    
    private final SessionFactoryImpl sessionFactory;

    private final Class<?> persistentClass;

    public AttributeMetadataFactory(final Class<?> persistantClass,
            final SessionFactoryImpl sessionFactory) {
        this.sessionFactory = checkNotNull(sessionFactory);

        this.persistentClass = checkNotNull(persistantClass);
    }

    private Type inferGenericType(Type type) {

        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;

            Type[] argumentTypes = pType.getActualTypeArguments();

            if (argumentTypes.length == 1) {
                return argumentTypes[0];
            }

            return null;
        } else if (type instanceof TypeVariable<?>) {
            ParameterizedType stype = (ParameterizedType) persistentClass
                    .getGenericSuperclass();

            Type[] actualTypeArguments = stype.getActualTypeArguments();

            // XXX:
            return actualTypeArguments[0];
                // actualTypeArguments[0]
            // for ( Type btype : vType.getBounds() )
            // {
            // if ( argumentTypes.length == 1 )
            // {
            // for (TypeVariable<?> tv : persistentClass.getA )
            // {
            // if (tv.equals( argumentTypes[ 0 ] ))
            // return tv;
            // }
            //
            // return argumentTypes[ 0 ];
            // }

            // return null;
        }

        return type;
    }

    public AttributeMetadata build(final Field f) throws MappingException {
        checkNotNull(f, "f is null");

        return new Builder(f).build();
    }

    private class Builder {

        private final Attribute attribute;
        private final Field f;
        private final Class<?> c;

        Builder(Field f) {
            this.f = checkNotNull(f);

            this.attribute = f.getAnnotation(Attribute.class);

            this.c = f.getType();
        }

        public AttributeMetadata build() throws MappingException {
            final AttributeMetadata meta = new AttributeMetadata();
            meta.setPropertyName(f.getName());
            final String name = getName();
            meta.setAttributeName(name);
            meta.setAttributeAliases(getAliases());
            final Class<?> objectType = getObjectType(c);
            if (objectType == null) {
                throw new MappingException(format("Could not determine type for attibute %s", name));
            }
            meta.setObjectType(objectType);
            String syntax = getSyntax(objectType);
            if (syntax == null) {
                throw new MappingException(format("No syntax specified  for attribute %s", name));
            }
            meta.setSyntax(syntax);
            meta.setSyntaxConverter(getSyntaxConverter(syntax));
            meta.setAttributeConverter(getAttributeConverter(objectType));
            meta.setCollectionType(getCollectionType());
            meta.setDirectoryType(getDirectoryType());
            return meta;
        }

        private Converter getSyntaxConverter(final String syntax) throws MappingException {
            final Converter retval = sessionFactory.getConverter(syntax);
            if (retval == null) {
                throw new MappingException(format("Syntax %s not supported", syntax));
            }
            return retval;
        }

        private Converter getAttributeConverter(final Type objectType) throws MappingException {
            final Converter retval = sessionFactory.getConverter(objectType);
            if (retval == null
                    && !sessionFactory.isPersistentClass((Class<?>) objectType)) {
                throw new MappingException(format("No converter found for type %s", objectType));
            }
            return retval;
        }

        private String getName() {
            String name = null;
            if (attribute != null && !isNullOrEmpty(attribute.name())) {
                name = attribute.name().trim();
            }
            if (isNullOrEmpty(name)) {
                name = f.getName();
            }
            assert !isNullOrEmpty(name);
            return name;
        }

        private String[] getAliases() {
            return attribute != null ? attribute.aliases() : new String[]{};
        }

        private String getSyntax(final Type objectType) throws MappingException {
            String retval = null;
            if (attribute != null && !isNullOrEmpty(attribute.syntax())) {
                retval = emptyToNull(attribute.syntax());
            }
            if (retval == null) {
                retval = guessSyntaxFromType(objectType);
            }
            if (retval == null) {
                final Converter attributeConverter = getAttributeConverter(objectType);

                if (attributeConverter == null
                        && sessionFactory
                        .isPersistentClass((Class<?>) objectType)) {
                    retval = guessSyntaxFromType(LdapName.class);
                } else {
                    retval = guessSyntaxFromType(attributeConverter
                            .directoryType());
                }
            }
            if (retval == null) {
                throw new MappingException("unsupported attribute type"
                        + objectType);
            }
            return retval;
        }

        private Class<?> getObjectType(Class<?> c) {
            Class<?> retval = null;
            if (asList(c.getInterfaces()).contains(Collection.class) || c.equals(Collection.class)) {
                final Type genericFieldType = f.getGenericType();

                if (genericFieldType instanceof ParameterizedType) {
                    final ParameterizedType pType = (ParameterizedType) genericFieldType;

                    Type[] fieldArgTypes = pType.getActualTypeArguments();

                    // Collection<?>, un seul paramètre générique
                    if (fieldArgTypes.length > 1) {
                        throw new UnsupportedOperationException(
                                "multivalued attributes are persisted only as List or Set");
                    }

                    retval = (Class<?>)inferGenericType(fieldArgTypes[0]);
                }
            }
            if (retval == null) {
                retval = (Class<?>)inferGenericType(c);
            }
            if (retval == null) {
                retval = Object.class;
            }
            return retval;
        }

        private Class<?> getDirectoryType() {
            if (attribute != null) {
                switch (attribute.type()) {
                    case BINARY:
                        return byte[].class;
                    case STRING:
                        return String.class;
                    default:
                        return null;
                }
            } else {
                return String.class; // default
            }
        }

        @SuppressWarnings("unchecked")
        private Class<? extends Collection<?>> getCollectionType() {
            if (asList(c.getInterfaces()).contains(Collection.class) || c.equals(Collection.class)) {
                return (Class<? extends Collection<?>>) c;
            } else {
                return null;
            }
        }
    }
}
