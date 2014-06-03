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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.naming.InvalidNameException;
import javax.naming.directory.DirContext;
import javax.naming.ldap.Rdn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.ContextSource;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.mtlx.odm.cache.CacheManager;
import fr.mtlx.odm.converters.BinaryStringConverter;
import fr.mtlx.odm.converters.BooleanConverter;
import fr.mtlx.odm.converters.Converter;
import fr.mtlx.odm.converters.ConvertionException;
import fr.mtlx.odm.converters.DirectoryStringConverter;
import fr.mtlx.odm.converters.IntegerConverter;
import fr.mtlx.odm.converters.LabeledURIConverter;
import fr.mtlx.odm.converters.TimeConverter;

public class SessionFactoryImpl implements SessionFactory
{
	public static final ImmutableSet<Converter> defaultConverters = new ImmutableSet.Builder<Converter>()
			.add( new BooleanConverter() )
			.add( new IntegerConverter() )
			.add( new DirectoryStringConverter() )
			.add( new BinaryStringConverter() )
			.add( new TimeConverter() )
			.add( new LabeledURIConverter() )
			.build();

	public static final ThreadLocal<Session> session = new ThreadLocal<Session>();

	private Session requestSession;

	public static final ImmutableSet<String> operationalAttributes = ImmutableSet.of( "objectClass" );

	private static final long serialVersionUID = 7615860356986827891L;

	private final List<String> mappedClasses = Lists.newArrayList();

	private final ContextSource contextSource;

	private final Map<String, ClassMetadata<?>> mappedClassesMetadata = Maps.newLinkedHashMap();
	
	private final Map<Class<?>, BasicProxyFactory<?>> proxyFactories = Maps.newConcurrentMap();

	private final Set<Converter> converters = Sets.newHashSet();

	private final CacheManager cacheManager;

	private final Logger log = LoggerFactory.getLogger( this.getClass() );

	public SessionFactoryImpl( final ContextSource contextSource )
	{
		this( contextSource, new NoCacheFactory(), null );
	}

	public SessionFactoryImpl( final ContextSource contextSource, final CacheFactory cacheFactory, @Nullable final String region )
	{
		converters.addAll( defaultConverters );

		this.contextSource = checkNotNull( contextSource );

		this.cacheManager = checkNotNull( cacheFactory ).getCache( this, region );
	}

	public SessionFactoryImpl( final ContextSource contextSource, @Nullable final CacheFactory cacheFactory, @Nullable String region,
			@Nullable final List<String> mappedClasses ) throws MappingException
	{
		this( contextSource, cacheFactory == null ? new NoCacheFactory() : cacheFactory, region );

		if ( mappedClasses != null )
		{
			for ( String className : mappedClasses )
			{
				addMappedClass( className );
			}
		}
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public <T> ClassMetadata<T> getClassMetadata( Class<T> entityClass )
	{
		return (ClassMetadata<T>)getClassMetadata( checkNotNull( entityClass ).getCanonicalName() );
	}

	@Override
	public ClassMetadata<?> getClassMetadata( String entityName )
	{
		return mappedClassesMetadata.get( checkNotNull( entityName ) );
	}

	@SuppressWarnings(
	{ "unchecked", "rawtypes" } )
	public void addMappedClass( String className ) throws MappingException
	{
		checkNotNull( className );

		if ( mappedClassesMetadata.containsKey( className ) )
			return;

		try
		{
			Class<?> persistentClass;

			persistentClass = Class.forName( className );

			mappedClassesMetadata.put( className, new ClassMetadataImpl( persistentClass ) );
			
			addProxyFactory( persistentClass );
		}
		catch ( Exception e )
		{
			throw new MappingException( e );
		}

		mappedClasses.add( className );
	}

	public void addConverter( Converter converter )
	{
		this.converters.add( checkNotNull( converter ) );
	}

	private  <T> void addProxyFactory( Class<T> clazz )
	{
		proxyFactories.put( clazz, new BasicProxyFactory<T>( clazz, new Class[] {} ) );
	}
	
	@Override
	public ClassMetadata<?> getClassMetadata( final String[] objectClasses ) throws IllegalAccessException, InvocationTargetException,
			ClassNotFoundException
	{
		final Set<String> directoryObjectClasses = ImmutableSet.copyOf( objectClasses );

		ClassMetadata<?> metadata = null;
		int Q = 0;

		// filtre sur la classe structurelle
		for ( String entryClass : mappedClassesMetadata.keySet() )
		{
			final ClassMetadata<?> candidateMetadata = mappedClassesMetadata.get( entryClass );

			final Set<String> oc = new ImmutableSet.Builder<String>()
					.addAll( candidateMetadata.getAuxiliaryClasses() )
					.add( candidateMetadata.getStructuralClass() )
					.build();

			log.debug( "trying {} ( {}, {} )", new Object[]
			{ entryClass, candidateMetadata.getStructuralClass(), Joiner.on( ", " ).skipNulls().join( candidateMetadata.getAuxiliaryClasses() ) } );

			if ( directoryObjectClasses.containsAll( oc ) )
			{
				final int q = oc.size();

				log.debug( "match q = {}", q );

				if ( q > Q )
				{
					Q = q;

					if ( metadata != null )
					{
						if ( metadata.getEntryClass().isAssignableFrom( candidateMetadata.getEntryClass() ) )
						{
							metadata = candidateMetadata;
						}
					}
					else
					{
						metadata = candidateMetadata;
					}
				}
			}

			if ( Q == directoryObjectClasses.size() )
				break;
		}

		return metadata;
	}

	@Override
	public boolean isPersistentClass( String className )
	{
		return mappedClassesMetadata.get( className ) != null;
	}

	@Override
	public boolean isPersistentClass( Class<?> clazz )
	{
		checkNotNull( clazz );

		for ( ClassMetadata<?> m : mappedClassesMetadata.values() )
		{
			if ( clazz.isAssignableFrom( m.getEntryClass() ) )
				return true;
		}

		return false;
	}

	@Override
	public Session getCurrentSession()
	{
		Session s = session.get();

		// Ouvre une nouvelle Session, si ce Thread n'en a aucune
		if ( s == null )
		{
			s = openSession();
			session.set( s );
		}

		return s;
	}

	@Override
	public void closeSession()
	{
		Session s = session.get();
		session.set( null );
		if ( s != null )
			s.close();
	}

	@Override
	public Session openSession()
	{
		return new SessionImpl( this );
	}

	@Override
	public ContextSource getContextSource()
	{
		return contextSource;
	}

	@Override
	public DirContext getDirContext()
	{
		return contextSource.getReadWriteContext();
	}

	@Override
	public boolean isOperationalAttribute( String attributeId )
	{
		return operationalAttributes.contains( attributeId );
	}

	private Converter getConverterForSyntax( String syntax )
	{
		for ( Converter c : converters )
		{
			if ( c.getSyntax().equals( syntax ) )
				return c;
		}

		return null;
	}

	@Override
	public Converter getConverter( final String syntax, final Type objectType, final Type directoryType )
	{
		Converter converter = getConverterForSyntax( syntax );

		if ( converter != null )
		{
			Class<?> boxedObjectType;

			if ( objectType instanceof Class<?> )
				boxedObjectType = (Class<?>)objectType;
			else if ( objectType == Integer.TYPE )
				boxedObjectType = Integer.class;
			else if ( objectType == Boolean.TYPE )
				boxedObjectType = Boolean.class;
			else if ( objectType == Short.TYPE )
				boxedObjectType = Short.class;
			else if ( objectType == Byte.TYPE )
				boxedObjectType = Byte.class;
			else
				throw new ConvertionException( String.format( "Type %s not supported", objectType ) );

			// Type checking
			if ( converter.directoryType() != directoryType || converter.objectType() != boxedObjectType ) { throw new ConvertionException( String.format( "converter type mismatch got (%s, %s) expected (%s, %s)",
					converter.directoryType(), converter.objectType(), directoryType, objectType ) ); }
		}

		return converter;
	}

	@Override
	public <T> Rdn composeName( T object, String propertyName ) throws InvalidNameException, MappingException
	{
		@SuppressWarnings( "unchecked" )
		ClassMetadata<T> cm = getClassMetadata( (Class<T>)object.getClass() );

		if ( cm == null )
			throw new MappingException( "not a persistent class" );

		ClassAssistant<T> assistant = new ClassAssistant<T>( cm );
		AttributeMetadata<T> am = cm.getAttributeMetadataByPropertyName( propertyName );

		if ( am == null )
			throw new MappingException( "unknow attribute" );

		return new Rdn( am.getAttirbuteName(), assistant.getValue( object, propertyName ) );
	}

	
	public Session getRequestSession()
	{
		return requestSession;
	}

	public void setRequestSession( Session requestSession )
	{
		this.requestSession = requestSession;
	}

	@Override
	public CacheManager getCacheManager()
	{
		return cacheManager;
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public <T> BasicProxyFactory<T> getProxyFactory( Class<T> clazz, Class<?>[] interfaces )
	{
		return (BasicProxyFactory<T>)proxyFactories.get(clazz);
	}
}
