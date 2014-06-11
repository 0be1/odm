package fr.mtlx.odm.spring;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Sets;

import fr.mtlx.odm.AttributeMetadata;
import fr.mtlx.odm.ClassMetadata;
import fr.mtlx.odm.ContextResolver;
import fr.mtlx.odm.Session;
import fr.mtlx.odm.converters.Converter;
import fr.mtlx.odm.converters.ConvertionException;
import fr.mtlx.odm.converters.EntryResolverConverter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.commons.beanutils.ConversionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DirContextOperations;

class DirContextOperationsResolver implements ContextResolver {

    private final Logger log = LoggerFactory.getLogger(DirContextOperationsResolver.class);

    private final ClassMetadata<?> metadata;

    private final Attributes attributes;

    private final Session session;

    DirContextOperationsResolver(final DirContextOperations context,
            final ClassMetadata<?> metadata, final Session session) {
        this.metadata = checkNotNull(metadata, "metadata is null");

        this.session = checkNotNull(session, "session is null");

        attributes = context.getAttributes();
    }

    private Attribute getAttribute(final AttributeMetadata metadata) {
        final Set<String> names = Sets.newLinkedHashSet();

        names.add(metadata.getAttirbuteName());

        names.addAll(Arrays.asList(metadata.getAttributeAliases()));

        for (final String alias : names) {
            final Attribute attr = attributes.get(alias);

            if (attr != null) {
                return attr;
            }
        }

        return null;
    }

    @Override
    public Object getProperty(final String name) throws NamingException,
            InstantiationException, IllegalAccessException {
        final Attribute attr;
        final AttributeMetadata attributeMetadata = metadata
                .getAttributeMetadata(name);

        if (attributeMetadata == null) {
            throw new InstantiationException("the property " + name
                    + " is not mapped to a directory attribute");
        }

        final Converter converter = attributeMetadata.getSyntaxConverter();

        if (log.isDebugEnabled()) {
            log.debug("attribute {} with syntax {} matching type {}",
                    attributeMetadata.getAttirbuteName(),
                    attributeMetadata.getSyntax(), converter.objectType());
        }

        attr = getAttribute(attributeMetadata);

        if (attr == null) {
            if (log.isDebugEnabled()) {
                log.debug("attribute {} not found", name);
            }

            return null;
        }

        if (attributeMetadata.isMultivalued()) {
            final Collection internalValues = attributeMetadata
                    .newCollectionInstance();

            final NamingEnumeration<?> values = attr.getAll();

            while (values.hasMoreElements()) {
                final Object internalValue = converter.fromDirectory(values
                        .nextElement());

                internalValues.add(convert(internalValue, attributeMetadata));
            }

            return internalValues;
        } else {
            if (attr.size() > 1) {
                throw new ConversionException(String.format(
                        "multiple values found for single valued attribute %s",
                        attributeMetadata.getAttirbuteName()));
            }

            final Object internalValue = converter.fromDirectory(attr.get());

            return convert(internalValue, attributeMetadata);
        }
    }

    private Object convert(Object from, AttributeMetadata metadata) {
        if (from == null) {
            return null;
        } // XXX

        Class<?> objectClass = from.getClass();

        if (metadata.getObjectType().equals(objectClass)) {
            return from;
        }
        
        Converter converter = metadata.getAttributeConverter();

        if (converter != null)
            return converter.fromDirectory(from);
        
        if (session.getSessionFactory().isPersistentClass(metadata.getObjectType())) {
            return new EntryResolverConverter<>(metadata.getObjectType(), session);
        }

        throw new ConvertionException(String.format("%s is not a persistent type.", metadata.getObjectType() ));
       
    }

    @Override
    public void setProperty(String name) throws NamingException, InstantiationException, IllegalAccessException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
