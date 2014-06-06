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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nullable;
import javax.naming.Name;
import javax.naming.directory.SearchControls;
import javax.persistence.NonUniqueResultException;

import org.springframework.ldap.control.PagedResultsCookie;
import org.springframework.ldap.control.PagedResultsDirContextProcessor;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.AbstractContextMapper;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.mtlx.odm.SessionImpl.CachingContextMapper;
import fr.mtlx.odm.SessionImpl.MappingContextMapper;
import fr.mtlx.odm.filters.AndFilter;
import fr.mtlx.odm.filters.Filter;
import fr.mtlx.odm.filters.FilterBuilder;
import fr.mtlx.odm.filters.FilterBuilderImpl;
import fr.mtlx.odm.filters.FilterImpl;

public class SearchCriteria<T>
{
	static class CountContextMapper extends AbstractContextMapper<Long>
	{
		private long cp;

		public CountContextMapper()
		{
			cp = 0;
		}

		public Long doMapFromContext( DirContextOperations context )
		{
			cp++;
			return cp;
		}

		public long getCount()
		{
			return cp;
		}
	}

	private class PagedResultIterator implements Iterator<List<T>>
	{
		private PagedResultsCookie cookie = null;

		private final int pageSize;

		private final String filter;

		private final Name base;

		private final Class<T> persistentClass;

		private PagedResultIterator( final Class<T> persistentClass, final int pageSize, final String filter,
				@Nullable final Name base )
		{
			checkArgument( pageSize > 0 );

			this.persistentClass = checkNotNull( persistentClass );
			this.pageSize = pageSize;
			this.filter = checkNotNull( filter );
			this.base = base;
		}

		@Override
		public boolean hasNext()
		{
			return cookie == null || cookie.getCookie() != null;
		}

		@Override
		public List<T> next()
		{
			List<T> results = Lists.newArrayList();

			if ( !hasNext() )
				throw new NoSuchElementException();

			CachingContextMapper<T> cm = ops.session.newCachingContextMapper();

			PagedResultsDirContextProcessor processor = new PagedResultsDirContextProcessor( pageSize, cookie );

			ops.session.getLdapOperations().search( base, filter, controls, cm, processor );

			for ( DirContextOperations ctx : cm.getContextQueue() )
			{
				T entry = ops.session.getFromCache( persistentClass, ctx.getDn() );

				if ( entry == null )
				{
					final MappingContextMapper<T> mapper = ops.session.new MappingContextMapper<T>( persistentClass, ops.assistant );

					entry = mapper.doMapFromContext( ctx );

					((SessionFactoryImpl)ops.session.getSessionFactory()).getCacheManager().getCacheFor( persistentClass ).store( ctx.getDn(), entry );
				}
				
				results.add( entry );
			}

			cookie = processor.getCookie();

			return results;
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}

	// private final ClassMetadata<T> metadata;

	// private final SessionImpl session;

	private final List<Filter> filterStack = Lists.newLinkedList();

	private Name root;

	private final SearchControls controls;

	private final Class<T> persistentClass;
	// private final ClassAssistant<T> assistant;

	private final OperationsImpl<T> ops;

	private final Map<String, Collection> projections = Maps.newHashMap();

	SearchCriteria( final Class<T> persistentClass, final OperationsImpl<T> ops, final Name root )
	{
		this( persistentClass, ops, root, SessionImpl.getDefaultSearchControls() );
	}

	SearchCriteria( final Class<T> persistentClass, final OperationsImpl<T> ops, final Name root, final SearchControls controls )
	{
		this.persistentClass = checkNotNull( persistentClass );

		this.controls = SessionImpl.getDefaultSearchControls();

		this.ops = checkNotNull( ops );

		this.root = checkNotNull( root );
	}

	//
	// SearchCriteria( final SessionImpl session, final ClassMetadata<T>
	// metadata, final String root ) throws InvalidNameException
	// {
	// this( session, metadata, new LdapName( root ) );
	// }
	//
	// SearchCriteria( final SessionImpl session, final ClassMetadata<T>
	// metadata, final Name root )
	// {
	// this.session = checkNotNull( session );
	//
	// this.metadata = checkNotNull( metadata );
	//
	// this.controls = SessionImpl.getDefaultSearchControls();
	//
	// this.root = root;
	//
	// this.assistant = new ClassAssistant<T>( metadata );
	// }

	public SearchCriteria<T> scope( int scope )
	{
		controls.setSearchScope( scope );

		return this;
	}

	@Deprecated
	public SearchCriteria<T> sub( Name root )
	{
		controls.setSearchScope( SearchControls.SUBTREE_SCOPE );

		this.root = checkNotNull( root );

		return this;
	}

	@Deprecated
	public SearchCriteria<T> one( Name root )
	{
		controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );

		this.root = checkNotNull( root );

		return this;
	}

	@Deprecated
	public SearchCriteria<T> object( Name dn )
	{
		controls.setSearchScope( SearchControls.OBJECT_SCOPE );

		this.root = checkNotNull( dn );

		return this;
	}

	public SearchCriteria<T> add( Filter filter )
	{
		filterStack.add( checkNotNull( filter ) );

		return this;
	}

	public SearchCriteria<T> countLimit( long limit )
	{
		controls.setCountLimit( limit );

		return this;
	}

	public SearchCriteria<T> timeLimit( int ms )
	{
		controls.setTimeLimit( ms );

		return this;
	}

	public SearchCriteria<T> properties( String... properties )
	{
		final List<String> attrs = Lists.newArrayList();

		for ( String propertyName : properties )
		{
			AttributeMetadata attr = ops.metadata.getAttributeMetadataByPropertyName( propertyName );

			if ( attr == null )
				throw new UnsupportedOperationException( String.format( "property %s not found in %s", propertyName, ops.metadata.getEntryClass() ) );

			attrs.add( attr.getAttirbuteName() );
		}

		controls.setReturningAttributes( attrs.toArray( new String[] {} ) );

		normalizeControls( controls );

		return this;
	}

	private String encodeFilter()
	{
		final FilterBuilder<T> fb = new FilterBuilderImpl<T>( persistentClass, ops.session.getSessionFactory() );
		
		final AndFilter rootfilter = fb.and();

		for ( String oc : ops.metadata.getObjectClassHierarchy() )
		{
			rootfilter.add( fb.objectClass( oc ) );
		}

		for ( String oc : ops.metadata.getAuxiliaryClasses() )
		{
			rootfilter.add( fb.objectClass( oc ) );
		}

		for ( Filter f : filterStack )
		{
			rootfilter.add( f );
		}
		
		return rootfilter.encode();

	}

	private void normalizeControls( SearchControls controls )
	{
		Set<String> attributes = Sets.newLinkedHashSet( Arrays.asList( controls.getReturningAttributes() ) );

		attributes.add( "objectClass" );

		controls.setReturningAttributes( attributes.toArray( new String[] {} ) );
	}

	public List<T> list() throws javax.naming.SizeLimitExceededException
	{
		List<T> results = ops.search( ops.metadata.getEntryClass(), root, controls, encodeFilter() );

		projections( results );

		return results;
	}

	public void nop() throws javax.naming.SizeLimitExceededException
	{
		projections( ops.search( ops.metadata.getEntryClass(), root, controls, encodeFilter() ) );
	}

	@SuppressWarnings( "unchecked" )
	private void projections( List<T> results )
	{
		for ( String property : projections.keySet() )
		{
			final AttributeMetadata t = ops.metadata.getAttributeMetadataByPropertyName( property );

			if ( t == null )
				throw new UnsupportedOperationException( String.format( "property %s not found in %s", property, ops.metadata.getEntryClass() ) );

			Class<?> c = (Class<?>)t.getObjectType(); // XXX Cast ??

			for ( T item : results )
			{
				final Object value = ops.assistant.getValue( item, property );

				if ( value != null )
				{
					if ( !c.isInstance( value ) )
						throw new UnsupportedOperationException( String.format( "property %s found wrong type %s", property, t ) );

					projections.get( property ).add( value );
				}
			}
		}
	}

	public T unique() throws NonUniqueResultException
	{
		List<T> results;

		long countLimit = controls.getCountLimit();

		controls.setCountLimit( 1 );

		try
		{
			results = list();
		}
		catch ( javax.naming.SizeLimitExceededException e )
		{
			throw new NonUniqueResultException( e.getMessage() );
		}
		finally
		{
			controls.setCountLimit( countLimit );
		}

		projections( results );

		return Iterables.getOnlyElement( results, null );
	}

	public long count()
	{
		controls.setReturningAttributes( new String[] {} );

		CountContextMapper cm = new CountContextMapper();

		ops.session.getLdapOperations().search( root, encodeFilter(), controls, cm, SessionImpl.nullDirContextProcessor );

		return cm.getCount();
	}

	public Iterable<List<T>> pages( final int pageSize )
	{
		return new Iterable<List<T>>()
		{
			@Override
			public Iterator<List<T>> iterator()
			{
				return new PagedResultIterator( persistentClass, pageSize, encodeFilter(), root );
			}
		};
	}

	public <C> SearchCriteria<T> addProjection( final Collection<C> collection, final String property )
	{
		this.projections.put( checkNotNull( property ), checkNotNull( collection ) );

		return this;
	}

	public SearchCriteria<T> example( T example )
	{
		filterStack.add( ops.metadata.getByExampleFilter() );

		return this;
	}
}
