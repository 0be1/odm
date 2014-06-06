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

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Queue;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DirContextProcessor;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.mtlx.odm.cache.CacheManager;
import fr.mtlx.odm.cache.ContextCache;
import fr.mtlx.odm.cache.ContextMapCache;
import fr.mtlx.odm.cache.EntityCache;
import fr.mtlx.odm.cache.SessionCacheManager;
import fr.mtlx.odm.converters.Converter;

public class SessionImpl implements Session {
	class CachingContextMapper<T> extends AbstractContextMapper<T> {
		private final Queue<DirContextOperations> queue = Lists.newLinkedList();

		public Queue<DirContextOperations> getContextQueue() {
			return queue;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected T doMapFromContext(final DirContextOperations ctx) {
			contextCache.store(ctx);

			queue.offer(ctx);

			return (T) ctx;
		}

	}

	class MappingContextMapper<T> extends AbstractContextMapper<T> {
		private final Class<T> persistentClass;

		private final ClassAssistant<T> assistant;

		MappingContextMapper(final Class<T> persistentClass,
				final ClassAssistant<T> assistant) {
			this.persistentClass = checkNotNull(persistentClass);
			this.assistant = checkNotNull(assistant);
		}

		@SuppressWarnings("unchecked")
		@Override
		protected T doMapFromContext(final DirContextOperations ctx) {
			final T entry;

			try {
				entry = (T) dirContextObjectFactory(checkNotNull(ctx));

				assistant.setIdentifier(entry, ctx.getDn());
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (InvalidNameException e) {
				throw new RuntimeException(e);
			}

			getObjectCacheManager().getCacheFor(persistentClass).store(
					ctx.getDn(), entry);

			return entry;
		}
	}

	private final Logger log = LoggerFactory.getLogger(Session.class);

	private final ContextCache contextCache = new ContextMapCache();

	private final LdapTemplate ops;

	private final SessionFactoryImpl sessionFactory;

	private final CacheManager objectCacheManager;

	public final static DirContextProcessor nullDirContextProcessor = new DirContextProcessor() {
		@Override
		public void postProcess(DirContext ctx) throws NamingException {
			// Do nothing
		}

		@Override
		public void preProcess(DirContext ctx) throws NamingException {
			// Do nothing
		}
	};

	public static SearchControls copySearchControls(
			final SearchControls controls) {
		final SearchControls retval = new SearchControls();

		retval.setCountLimit(controls.getCountLimit());
		retval.setDerefLinkFlag(controls.getDerefLinkFlag());
		retval.setReturningObjFlag(false);
		retval.setSearchScope(controls.getSearchScope());
		retval.setTimeLimit(controls.getTimeLimit());

		return retval;
	}

	public static SearchControls getDefaultSearchControls() {
		final SearchControls retval = new SearchControls();

		retval.setSearchScope(SearchControls.SUBTREE_SCOPE);

		retval.setReturningObjFlag(false);

		return retval;
	}

	SessionImpl(final SpringSessionFactoryImpl sessionFactory) {
		this.sessionFactory = checkNotNull(sessionFactory,
				"sessionFactory is null");

		this.ops = new LdapTemplate(sessionFactory.getContextSource());

		this.objectCacheManager = new SessionCacheManager(this);
	}

	@Override
	public void close() {
		getContextCache().clear();

		getObjectCacheManager().clear();
	}

	// @SuppressWarnings( { "unchecked", "rawtypes" } )
	@Override
	public Converter getSyntaxConverter(final String syntax)
			throws MappingException {

		final Converter converter = getSessionFactory().getConverter(syntax);

		// if ( converter == null )
		// {
		// final Type concreteType = inferGenericType( objectType );
		//
		// if ( objectType instanceof Class<?> )
		// {
		// Class<?> persistentClass = (Class<?>)concreteType;
		//
		// if ( getSessionFactory().isPersistentClass( persistentClass ) )
		// {
		// converter = new EntryResolverConverter( persistentClass, this );
		// }
		// }
		// }

		if (converter == null)
			throw new MappingException(String.format(
					"no converter found for syntax %s", syntax));

		return converter;
	}

	@Override
	public LdapOperations getLdapOperations() {
		return ops;
	}

	@Override
	public SessionFactoryImpl getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public boolean isPersistent(final Object obj) {
		final Name dn;
		final Class<?> clazz;
		final ClassMetadata<?> metadata;

		if (obj == null)
			return false;

		clazz = obj.getClass();

		metadata = getSessionFactory().getClassMetadata(clazz);

		if (metadata == null)
			return false;

		try {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			final ClassAssistant<?> assistant = new ClassAssistant(metadata);

			dn = assistant.getIdentifier(obj);
		} catch (Exception e) {
			return false;
		}

		return contextCache.contains(dn);
	}

	@Override
	public void modifyAttributes(final Name dn, final ModificationItem[] mods) {
		ops.modifyAttributes(dn, mods);
	}

	<T> CachingContextMapper<T> newCachingContextMapper() {
		return new CachingContextMapper<T>();
	}

	private Object dirContextObjectFactory(final DirContextOperations context)
			throws ClassNotFoundException, InstantiationException,
			InvalidNameException {
		final String[] objectClasses = context
				.getStringAttributes("objectClass");

		assert objectClasses != null && objectClasses.length > 0;

		try {
			final ClassMetadata<?> metadata = sessionFactory
					.getClassMetadata(objectClasses);

			return sessionFactory.getProxyFactory(metadata.getEntryClass(),
					new Class[] {}).getProxy(this, context);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	DirContextOperations doLookup(final Name dn) {
		final DirContextOperations ctx = ops.lookupContext(checkNotNull(dn,
				"dn is null"));

		final Name _dn = ctx.getDn();

		assert dn.equals(_dn) : "lookup DN does not match context DN";

		if (log.isDebugEnabled()) {
			log.debug("caching context {} ", _dn);
		}

		contextCache.store(ctx);

		return ctx;
	}

	void mapToContext(final Object transientObject,
			final DirContextOperations context) {
		final ClassMetadata<?> metadata = getSessionFactory().getClassMetadata(
				checkNotNull(transientObject).getClass());

		try {
			final Set<String> objectClasses = Sets.newHashSet();

			final String[] objectClassesRaw = context
					.getStringAttributes("objectClass");

			if (objectClassesRaw != null)
				objectClasses.addAll(Arrays.asList(objectClassesRaw));

			// ObjectClasses
			for (String objectClass : metadata.getObjectClassHierarchy()) {
				if (!objectClasses.contains(objectClass))
					context.addAttributeValue("objectClass", objectClass);
			}

			for (String propertyName : metadata.getProperties()) {
				final AttributeMetadata ameta = metadata
						.getAttributeMetadataByPropertyName(propertyName);

				@SuppressWarnings({ "rawtypes", "unchecked" })
				final ClassAssistant<?> assistant = new ClassAssistant(metadata);

				final Converter converter = getSyntaxConverter(ameta
						.getSyntax());

				if (ameta.isMultivalued()) {
					final Collection<?> values = (Collection<?>) assistant
							.getValue(transientObject, propertyName);

					if (values != null) {
						for (Object value : values) {
							if (value != null)
								context.addAttributeValue(
										ameta.getAttirbuteName(),
										converter.toDirectory(value));
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
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public <T> Operations<T> getOperations(final Class<T> persistentClass) {
		return new OperationsImpl<T>(this, checkNotNull(persistentClass));
	}

	public ContextCache getContextCache() {
		return contextCache;
	}

	<T> T getFromCache(final Class<T> persistentClass, final Name dn) {
		final EntityCache<T> sessionCache = getObjectCacheManager()
				.getCacheFor(persistentClass);

		T entry = sessionCache.retrieve(dn);

		if (entry == null) {
			final EntityCache<T> factoryCache = getSessionFactory()
					.getCacheManager().getCacheFor(persistentClass);

			if (factoryCache != null) {
				entry = factoryCache.retrieve(dn);

				if (entry != null) {
					sessionCache.store(dn, entry);
				}
			}
		}

		return entry;
	}

	<T> void removeFromCache(final Class<T> persistentClass, final Name dn) {
		getSessionFactory().getCacheManager().getCacheFor(persistentClass)
				.remove(dn);

		getObjectCacheManager().getCacheFor(persistentClass).remove(dn);
	}

	public CacheManager getObjectCacheManager() {
		return objectCacheManager;
	}
}
