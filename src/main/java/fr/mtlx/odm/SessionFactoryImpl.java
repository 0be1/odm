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
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.ldap.Rdn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.mtlx.odm.cache.CacheManager;
import fr.mtlx.odm.converters.Converter;
import fr.mtlx.odm.filters.FilterBuilder;
import fr.mtlx.odm.filters.FilterBuilderImpl;

@SuppressWarnings("serial")
public abstract class SessionFactoryImpl implements SessionFactory {
	public static final ThreadLocal<Session> session = new ThreadLocal<Session>();

	private Session requestSession;

	public final Set<String> operationalAttributes = Sets
			.newHashSet("objectClass");

	private final Map<String, PartialClassMetadata<?>> mappedClassesMetadata = Maps
			.newLinkedHashMap();

	private final Map<Class<?>, BasicProxyFactory<?>> proxyFactories = Maps
			.newConcurrentMap();

	private final Map<String, Converter> syntaxConverters = Maps
			.newConcurrentMap();

	private final Map<Type, Converter> attributeConverters = Maps
			.newConcurrentMap();

	private CacheManager cacheManager;

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@SuppressWarnings("unchecked")
	public <T> ClassMetadata<T> getClassMetadata(Class<T> entityClass) {
		return (ClassMetadata<T>) getClassMetadata(checkNotNull(entityClass)
				.getCanonicalName());
	}

	public ClassMetadata<?> getClassMetadata(String entityName) {
		return mappedClassesMetadata.get(checkNotNull(entityName));
	}

	public void addSyntaxConverter(String syntax, Converter converter) {
		this.syntaxConverters
				.put(checkNotNull(syntax), checkNotNull(converter));
	}

	public void addAttributeConverter(Type type, Converter converter) {
		this.attributeConverters.put(checkNotNull(type),
				checkNotNull(converter));
	}

	private <T> void addProxyFactory(Class<T> clazz) {
		proxyFactories.put(clazz, new BasicProxyFactory<T>(clazz,
				new Class[] {}));
	}

	@Override
	public ClassMetadata<?> getClassMetadata(final String[] objectClasses)
			throws IllegalAccessException, InvocationTargetException,
			ClassNotFoundException {
		final Set<String> directoryObjectClasses = ImmutableSet
				.copyOf(objectClasses);

		ClassMetadata<?> metadata = null;
		int Q = 0;

		// filtre sur la classe structurelle
		for (String entryClass : mappedClassesMetadata.keySet()) {
			final ClassMetadata<?> candidateMetadata = mappedClassesMetadata
					.get(entryClass);

			final Set<String> oc = new ImmutableSet.Builder<String>()
					.addAll(candidateMetadata.getAuxiliaryClasses())
					.add(candidateMetadata.getStructuralClass()).build();

			log.debug(
					"trying {} ( {}, {} )",
					new Object[] {
							entryClass,
							candidateMetadata.getStructuralClass(),
							Joiner.on(", ")
									.skipNulls()
									.join(candidateMetadata
											.getAuxiliaryClasses()) });

			if (directoryObjectClasses.containsAll(oc)) {
				final int q = oc.size();

				log.debug("match q = {}", q);

				if (q > Q) {
					Q = q;

					if (metadata != null) {
						if (metadata.getEntryClass().isAssignableFrom(
								candidateMetadata.getEntryClass())) {
							metadata = candidateMetadata;
						}
					} else {
						metadata = candidateMetadata;
					}
				}
			}

			if (Q == directoryObjectClasses.size())
				break;
		}

		return metadata;
	}

	@Override
	public boolean isPersistentClass(String className) {
		return mappedClassesMetadata.get(className) != null;
	}

	@Override
	public boolean isPersistentClass(Class<?> clazz) {
		checkNotNull(clazz);

		for (ClassMetadata<?> m : mappedClassesMetadata.values()) {
			if (clazz.isAssignableFrom(m.getEntryClass()))
				return true;
		}

		return false;
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
		Session s = session.get();
		session.set(null);
		if (s != null)
			s.close();
	}

	@Override
	public boolean isOperationalAttribute(String attributeId) {
		return operationalAttributes.contains(attributeId);
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

		if (cm == null)
			throw new MappingException("not a persistent class");

		ClassAssistant<T> assistant = new ClassAssistant<T>(cm);
		AttributeMetadata meta = cm
				.getAttributeMetadataByPropertyName(propertyName);

		if (meta == null)
			throw new MappingException("unknow attribute");

		return new Rdn(meta.getAttirbuteName(), assistant.getValue(object,
				propertyName));
	}

	public Session getRequestSession() {
		return requestSession;
	}

	public void setRequestSession(Session requestSession) {
		this.requestSession = requestSession;
	}

	public CacheManager getCacheManager() {
		return cacheManager;
	}

	@SuppressWarnings("unchecked")
	public <T> BasicProxyFactory<T> getProxyFactory(Class<T> clazz,
			Class<?>[] interfaces) {
		return (BasicProxyFactory<T>) proxyFactories.get(clazz);
	}

	@Override
	public <T> FilterBuilder<T> filterBuilder(Class<T> persistentClass) {
		return new FilterBuilderImpl<T>(persistentClass, this);
	}

	protected <T> void mapClass(String className) throws MappingException {
		checkNotNull(className);

		if (mappedClassesMetadata.containsKey(className)) {
			log.warn("class {} already mapped", className);
			return;
		}

		try {
			Class<T> persistentClass = (Class<T>) Class.forName(className);

			mappedClassesMetadata.put(className, new ClassMetadataBuilder<T>(
					persistentClass).build());

			addProxyFactory(persistentClass);
		} catch (Exception e) {
			throw new MappingException(e);
		}
	}

	protected void init() {
		for (final PartialClassMetadata<?> metadata : mappedClassesMetadata
				.values()) {
			metadata.init(this);
		}
	}
}
