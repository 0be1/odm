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
import java.lang.reflect.Method;
import java.util.List;

import javax.annotation.Nullable;
import javax.naming.Name;
import javax.naming.directory.SearchControls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.SizeLimitExceededException;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DirContextProcessor;
import org.springframework.ldap.core.support.AbstractContextMapper;

import com.google.common.collect.Lists;

import fr.mtlx.odm.SessionImpl.CachingContextMapper;
import fr.mtlx.odm.SessionImpl.MappingContextMapper;

public class OperationsImpl<T> implements Operations<T> {
	private static Logger log = LoggerFactory.getLogger(OperationsImpl.class);

	Class<T> persistentClass;

	SessionImpl session;

	ClassMetadata<T> metadata;

	ClassAssistant<T> assistant;

	OperationsImpl(final SessionImpl session, final Class<T> persistentClass) {
		this.persistentClass = checkNotNull(persistentClass);

		this.session = checkNotNull(session);

		this.metadata = ((SessionFactoryImpl) session.getSessionFactory())
				.getClassMetadata(persistentClass);

		if (metadata == null)
			throw new UnsupportedOperationException(String.format(
					"%s is not a persistent class", persistentClass));

		this.assistant = new ClassAssistant<T>(metadata);
	}

	/**
	 * @throws javax.naming.NameNotFoundException
	 */
	@Override
	public T lookup(Name dn) throws javax.naming.NameNotFoundException {
		if (log.isDebugEnabled())
			log.debug("lookup for {}", dn);

		T entry = session.getFromCache(persistentClass, dn);

		if (entry == null) {
			DirContextOperations context;
			final MappingContextMapper<T> mapper = session.new MappingContextMapper<T>(
					persistentClass, assistant);

			context = session.getContextCache().retrieve(dn);

			if (context == null) {
				context = session.doLookup(dn);
			}

			entry = mapper.doMapFromContext(context);

			((SessionFactoryImpl) session.getSessionFactory())
					.getCacheManager().getCacheFor(persistentClass)
					.store(dn, entry);
		}

		return entry;
	}

	@Override
	public T lookupByExample(T example) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchCriteria<T> search(final Name base) {
		return new SearchCriteria<T>(persistentClass, this, base);
	}

	List<T> search(final Class<T> persistentClass, final Name base,
			final SearchControls controls, final String filter,
			@Nullable final DirContextProcessor processor)
			throws javax.naming.SizeLimitExceededException {
		final List<T> results = Lists.newArrayList();

		CachingContextMapper<T> cm = session.new CachingContextMapper<T>();

		try {
			session.getLdapOperations().search(
					base,
					filter,
					controls,
					cm,
					processor == null ? SessionImpl.nullDirContextProcessor
							: processor);
		} catch (SizeLimitExceededException ex) {
			throw new javax.naming.SizeLimitExceededException(
					ex.getExplanation());
		}

		for (DirContextOperations ctx : cm.getContextQueue()) {
			T entry = session.getFromCache(persistentClass, ctx.getDn());

			if (entry == null) {
				final MappingContextMapper<T> mapper = session.new MappingContextMapper<T>(
						persistentClass, assistant);

				entry = mapper.doMapFromContext(ctx);

				((SessionFactoryImpl) session.getSessionFactory())
						.getCacheManager().getCacheFor(persistentClass)
						.store(ctx.getDn(), entry);
			}
			results.add(entry);
		}

		return results;
	}

	List<T> search(final Class<T> persistentClass, final Name base,
			final SearchControls controls, final String filter)
			throws javax.naming.SizeLimitExceededException {
		return search(persistentClass, base, controls, filter, null);
	}

	@Override
	public void bind(T transientObject) {
		Name dn;

		prepersist(transientObject);

		try {
			dn = assistant.getIdentifier(transientObject);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		DirContextOperations context = new DirContextAdapter(dn);

		session.mapToContext(checkNotNull(transientObject), context);

		session.getLdapOperations().bind(context);

		session.getContextCache().store(context);
	}

	private void prepersist(T transientObject) {
		for (Method method : metadata.prepersistMethods()) {
			try {
				method.invoke(transientObject);
			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			} catch (InvocationTargetException e) {
			}
		}
	}

	@Override
	public void modify(T persistentObject) {
		Name dn;

		try {
			dn = assistant.getIdentifier(persistentObject);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		DirContextOperations modifedContext = session.getContextCache()
				.retrieve(dn);

		if (modifedContext == null)
			throw new IllegalArgumentException("not a persistent object");

		session.mapToContext(checkNotNull(persistentObject), modifedContext);

		session.getLdapOperations().modifyAttributes(modifedContext);

		assert dn.equals(modifedContext.getDn());

		session.getContextCache().store(modifedContext);
	}

	@Override
	public void unbind(final T persistentObject) {
		Name dn;

		try {
			dn = assistant.getIdentifier(persistentObject);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		if (!session.getContextCache().contains(dn))
			throw new IllegalArgumentException("not a persistent object");

		session.getLdapOperations().unbind(dn);

		session.getContextCache().remove(dn);

		session.getObjectCacheManager().getCacheFor(persistentClass).remove(dn);
	}

	@Override
	public void purge(final Name base) {
		SearchControls controls = new SearchControls();
		controls.setReturningAttributes(new String[] {});
		controls.setSearchScope(SearchControls.SUBTREE_SCOPE);

		for (Object dn : session.getLdapOperations().search(base, "", controls,
				new AbstractContextMapper<Name>() {
					@Override
					protected Name doMapFromContext(DirContextOperations ctx) {
						return ctx.getDn();
					}
				}, SessionImpl.nullDirContextProcessor)) {
			session.getLdapOperations().unbind((Name) dn);

			session.getContextCache().remove((Name) dn);

			session.getObjectCacheManager().getCacheFor(persistentClass)
					.remove((Name) dn);
		}
	}

	@Override
	public SearchCriteria<T> search(Name base, SearchControls controls) {
		return new SearchCriteria<T>(persistentClass, this, base, controls);
	}

}
