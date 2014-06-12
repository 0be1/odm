package fr.mtlx.odm.spring;

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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.SizeLimitExceededException;
import org.springframework.ldap.control.PagedResultsCookie;
import org.springframework.ldap.control.PagedResultsDirContextProcessor;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DirContextProcessor;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.mtlx.odm.AttributeMetadata;
import fr.mtlx.odm.ClassAssistant;
import fr.mtlx.odm.ClassMetadata;
import fr.mtlx.odm.MappingException;
import fr.mtlx.odm.OperationsImpl;
import fr.mtlx.odm.converters.Converter;
import fr.mtlx.odm.converters.ConvertionException;
import fr.mtlx.odm.utils.TypeCheckConverter;

public class SpringOperationsImpl<T> extends OperationsImpl<T> {

    private static final Logger log = LoggerFactory.getLogger(SpringOperationsImpl.class);

    private final MappingContextMapper contextMapper;

    private final ClassAssistant<T> assistant;

    private final LdapOperations operations;

    private final TypeCheckConverter<T> typeChecker = new TypeCheckConverter<T>(persistentClass);

    private final TypeCheckConverter<ClassMetadata<? extends T>> metadataConverter;

    public SpringOperationsImpl(final SpringSessionImpl session, final Class<T> persistentClass) {
	super(session, persistentClass);
	
	this.assistant = new ClassAssistant<>(metadata);

	this.contextMapper = new MappingContextMapper(assistant);

	this.operations = new LdapTemplate(session.getSessionFactory().getContextSource());

	metadataConverter = new TypeCheckConverter<ClassMetadata<? extends T>>(metadata.getClass());
    }

    @Override
    public SpringSessionImpl getSession() {
        return (SpringSessionImpl) super.getSession();
    }

    @Override
    public void bind(T transientObject) {
        Name dn;

	prePersist(checkNotNull(transientObject));

	dn = assistant.getIdentifier(transientObject);

        DirContextOperations context = new DirContextAdapter(dn);

	mapToContext(transientObject, context);

        operations.bind(context);

	getSession().getCache().store(dn, context);
    }

    @Override
    public void modify(T persistentObject) {
	throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void doUnbind(final Name dn) {
	operations.unbind(dn);

	getSession().getContextCache().remove(dn);
    }

    @Override
    protected T doLookup(Name dn) {
	T entry;

	DirContextOperations context = getSession().getContextCache().retrieve(dn).orElse(doContextLookup(dn));

	final MappingContextMapper mapper = new MappingContextMapper(assistant);

	// XXX : il faut stocker le context dans le cache avant de faire le
	// mapping !
	getSession().getContextCache().store(dn, context);

	entry = mapper.doMapFromContext(context);

	return entry;
    }

    @Override
    public Stream<T> search(final Name base, final SearchControls controls, final String filter)
            throws javax.naming.SizeLimitExceededException {
        return search(base, controls, filter, Optional.empty());
    }

    public List<T> search(final Name base, final SearchControls controls, final String filter,
	    final Optional<DirContextProcessor> processor) throws javax.naming.SizeLimitExceededException {
	final CachingContextMapper<T> cm = new CachingContextMapper<>();

	try {
	    operations.search(base, filter, controls, cm, processor.orElse(nullDirContextProcessor));
	} catch (SizeLimitExceededException ex) {
	    throw new javax.naming.SizeLimitExceededException(ex.getExplanation());
	}

	List<T> results = new ArrayList<>();

	for (DirContextOperations ctx : cm.getContextQueue()) {

	    Object object = getSession().getFromCache(ctx.getDn()).orElse(contextMapper.doMapFromContext(ctx));

	    results.add(typeChecker.convert(object));
	}

	return results;
    }

    @Override
    public long count(final Name base, final SearchControls controls, final String filter) {
        controls.setReturningAttributes(RETURN_NO_ATTRIBUTES);

        CountContextMapper cm = new CountContextMapper();

        operations.search(base, filter, controls, cm, nullDirContextProcessor);

        return cm.getCount();
    }

    private void mapToContext(final T transientObject,
            final DirContextOperations context) {

        try {
            final Set<String> objectClasses = Sets.newHashSet();

            final String[] objectClassesRaw = context
                    .getStringAttributes("objectClass");

            if (objectClassesRaw != null) {
                objectClasses.addAll(Arrays.asList(objectClassesRaw));
            }

            metadata.getObjectClassHierarchy().stream().filter((objectClass) -> (!objectClasses.contains(objectClass))).forEach((objectClass) -> {
                context.addAttributeValue("objectClass", objectClass);
            });

            for (String propertyName : metadata.getProperties()) {
                final AttributeMetadata ameta = metadata
                        .getAttributeMetadata(propertyName);

                final Converter converter = getSession().getSyntaxConverter(ameta
                        .getSyntax());

                if (ameta.isMultivalued()) {
                    final Collection<?> values = (Collection<?>) assistant
                            .getValue(transientObject, propertyName);

                    if (values != null) {
                        for (Object value : values) {
                            if (value != null) {
                                context.addAttributeValue(
                                        ameta.getAttirbuteName(),
                                        converter.toDirectory(value));
                            }
                        }
                    } else {
                        // remove the attribute from the entry
                        context.setAttributeValues(ameta.getAttirbuteName(),
                                null);
                    }
                } else {
                    final String attributeId = ameta.getAttirbuteName();

                    final Object currentValue = converter.toDirectory(assistant
                            .getValue(transientObject, propertyName));

                    final Object persistedValue = converter
                            .fromDirectory(context
                                    .getObjectAttribute(attributeId));

                    if (persistedValue != null && currentValue != null) {
                        if (!persistedValue.equals(currentValue)) {
                            context.removeAttributeValue(attributeId,
                                    persistedValue);

                            context.addAttributeValue(attributeId, currentValue);
                        }
                    } else if (persistedValue != null) {
                        context.removeAttributeValue(attributeId,
                                persistedValue);
                    } else if (currentValue != null) {
                        context.removeAttributeValue(attributeId,
                                persistedValue); // null
                        context.addAttributeValue(attributeId, currentValue);
                    }
                }
            }
        } catch (MappingException | ConvertionException e) {
            throw new RuntimeException(e);
        }
    }

    private DirContextOperations doContextLookup(final Name dn) {
	final DirContextOperations ctx = operations.lookupContext(checkNotNull(dn, "dn is null"));

        final Name _dn = ctx.getDn();

        assert dn.equals(_dn) : "lookup DN does not match context DN";

	if (log.isDebugEnabled()) {
	    log.debug("lookup context {} done", _dn);
	}

	return ctx;
    }

    static class CountContextMapper extends AbstractContextMapper<Long> {

        private long cp;

        CountContextMapper() {
            cp = 0;
        }

        @Override
        public Long doMapFromContext(DirContextOperations context) {
            cp++;
            return cp;
        }

        public long getCount() {
            return cp;
        }
    }

    static final DirContextProcessor nullDirContextProcessor = new DirContextProcessor() {
        @Override
        public void postProcess(DirContext ctx) throws NamingException {
            // Do nothing
        }

        @Override
        public void preProcess(DirContext ctx) throws NamingException {
            // Do nothing
        }
    };

    @Override
    public Iterable<List<T>> pages(final int pageSize, String filter, Name base, final SearchControls controls) {

        class PagedResultIterator implements Iterator<List<T>> {

            private PagedResultsCookie cookie = null;

            @Override
            public boolean hasNext() {
                return cookie == null || cookie.getCookie() != null;
            }

            @Override
            public List<T> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                PagedResultsDirContextProcessor processor = new PagedResultsDirContextProcessor(pageSize, cookie);

                try {
                    Stream<T> s = search(base, controls, filter, Optional.of(processor));

                    final List<T> results = s.collect(Collectors.toCollection(ArrayList<T>::new));

                    cookie = processor.getCookie();

                    return results;
                } catch (javax.naming.SizeLimitExceededException ex) {
                    throw new NoSuchElementException(ex.getExplanation());
                }
            }
        }

        return () -> new PagedResultIterator();
    }
    
    class CachingContextMapper<T> extends AbstractContextMapper<T> {

        private final Queue<DirContextOperations> queue = Lists.newLinkedList();

        public Queue<DirContextOperations> getContextQueue() {
            return queue;
        }

	@SuppressWarnings("unchecked")
	@Override
	protected T doMapFromContext(final DirContextOperations ctx) {
	    getSession().getContextCache().store(ctx.getDn(), ctx);

            queue.offer(ctx);

            return (T) ctx;
        }
    }

    class MappingContextMapper extends AbstractContextMapper<T> {

	private final ClassAssistant<T> assistant;

	MappingContextMapper(final ClassAssistant<T> assistant) {
	    this.assistant = checkNotNull(assistant);
	}

	@Override
	protected T doMapFromContext(final DirContextOperations ctx) {
	    final T entry;
	    try {
		entry = typeChecker.convert(dirContextObjectFactory(checkNotNull(ctx)));

		assistant.setIdentifier(entry, ctx.getDn());
	    } catch (InstantiationException | InvalidNameException | IllegalAccessException | InvocationTargetException e) {
		throw new RuntimeException(e);
	    }

	    getSession().getCache().store(ctx.getDn(), Optional.of(entry));
	    return entry;
	}
    }
    
     private Object dirContextObjectFactory(final DirContextOperations context)
            throws ClassNotFoundException, InstantiationException,
            InvalidNameException, IllegalAccessException {
        final String[] objectClasses = context
                .getStringAttributes("objectClass");

    private Object dirContextObjectFactory(final DirContextOperations context) throws InstantiationException,
	    InvalidNameException, IllegalAccessException, InvocationTargetException {
	final String[] objectClasses = context.getStringAttributes("objectClass");

	assert objectClasses != null && objectClasses.length > 0;

	Class<? extends T> realPersistentClass; 
	
	try {
	    realPersistentClass =  metadataConverter.convert( getSession().getSessionFactory().getClassMetadata( objectClasses) ).getPersistentClass();
	} catch (ClassNotFoundException e) {
	    realPersistentClass = metadata.getPersistentClass();
	}

	return getSession().getSessionFactory().getProxyFactory(realPersistentClass, new Class[] {})
		.getProxy(getSession(), context);
    }
}
