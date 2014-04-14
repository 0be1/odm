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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.PrePersist;

import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.ReflectionUtils.MethodCallback;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.mtlx.odm.filters.Filter;

public class ClassMetadataImpl<T> implements ClassMetadata<T>
{
	private final Class<T> persistentClass;

	private final ImmutableList<String> objectClassHierarchy;

	private final ImmutableSet<String> auxiliaryClasses;

	private final Constructor<T> defaultConstructor;

	private Field identifierField;

	private final Map<String, AttributeMetadata<T>> attributeMetadataByAttributeName = new TreeMap<String, AttributeMetadata<T>>( String.CASE_INSENSITIVE_ORDER );

	private final Map<String, AttributeMetadata<T>> attributeMetadataByPropertyName = new TreeMap<String, AttributeMetadata<T>>( String.CASE_INSENSITIVE_ORDER );

	private final List<Method> prepersistMethods = Lists.newArrayList();

	private final boolean cacheable;

	private final boolean strict;

	@SuppressWarnings(
	{ "unchecked", "rawtypes" } )
	public static <T> ClassMetadata<T> forName( String persistentClassName ) throws MappingException
	{
		try
		{
			return new ClassMetadataImpl( Class.forName( persistentClassName ) );
		}
		catch ( ClassNotFoundException e )
		{
			throw new MappingException( e );
		}
	}

	public static <T> ClassMetadata<T> forClass( Class<T> persistentClass )
	{
		return new ClassMetadataImpl<T>( persistentClass );
	}

	public ClassMetadataImpl( final Class<T> persistentClass ) throws MappingException
	{
		this.persistentClass = checkNotNull( persistentClass );

		if ( Modifier.isAbstract( persistentClass.getModifiers() ) )
			throw new InstantiationError( String.format( "%s is abstract", persistentClass.getName() ) );

		Class<? super T> currentClass = persistentClass;

		List<String> classHierarchy = Lists.newArrayList();
		Set<String> auxiliaryClasses = Sets.newHashSet();

		boolean isStrict = false;

		do
		{
			Entry entry = currentClass.getAnnotation( Entry.class );

			if ( entry != null )
			{
				classHierarchy.addAll( Arrays.asList( entry.objectClasses() ) );

				auxiliaryClasses.addAll( Arrays.asList( entry.auxiliaryObjectClasses() ) );

				isStrict |= entry.ignoreNonMatched();
			}

			currentClass = currentClass.getSuperclass();
		}
		while ( currentClass != null );

		assert currentClass == null;

		this.strict = isStrict;

		if ( classHierarchy.isEmpty() )
			throw new MappingException( String.format( "%s is not a persistent class", persistentClass ) );

		// final fields
		this.objectClassHierarchy = ImmutableList.copyOf( Lists.reverse( classHierarchy ) );

		this.auxiliaryClasses = ImmutableSet.copyOf( auxiliaryClasses );

		try
		{
			this.defaultConstructor = persistentClass.getConstructor( new Class[] {} );
		}
		catch ( SecurityException e )
		{
			throw new MappingException( e );
		}
		catch ( NoSuchMethodException e )
		{
			throw new MappingException( String.format( "no default contructor found for %s", persistentClass ), e );
		}

		findIdentifier();

		initPersistentAttributes();

		persistMethods();

		cacheable = isCacheable( persistentClass );
	}

	private boolean isCacheable( final Class<T> persistentClass )
	{
		final javax.persistence.Cacheable cacheable = persistentClass.getAnnotation( javax.persistence.Cacheable.class );

		return cacheable != null;
	}

	private void initPersistentAttributes()
	{
		ReflectionUtils.doWithFields( persistentClass, new FieldCallback()
		{
			public void doWith( final Field f ) throws IllegalArgumentException, IllegalAccessException
			{
				AttributeMetadata<T> metadata;

				if ( f == null || identifierField.equals( f ) || isTransient( f ) )
					return;

				// Allow fields to be redefined in subclasses,
				// ignoring fields in super class with the same name
				if ( attributeMetadataByPropertyName.containsKey( f.getName() ) )
					return;

				try
				{
					metadata = new AttributeMetadata<T>( persistentClass, f );
				}
				catch ( MappingException e )
				{
					throw new IllegalArgumentException( e );
				}

				if ( attributeMetadataByAttributeName.containsKey( metadata.getAttirbuteName() ) )
				{
					// throw IllegalArgumentException( "Attribute " +
					// metadata.getAttirbuteName() + " already declared." );
				}
				else
				{
					attributeMetadataByAttributeName.put( metadata.getAttirbuteName(), metadata );
				}
				for ( String alias : metadata.getAttributeAliases() )
				{
					if ( attributeMetadataByAttributeName.containsKey( alias ) )
					{
					throw new IllegalArgumentException( "Attribute " + alias + " already declared." );
					}

					attributeMetadataByAttributeName.put( alias, metadata );
				}

				attributeMetadataByPropertyName.put( f.getName(), metadata );
			}
		} );
	}

	private boolean isTransient( final Field field )
	{
		int modifiers = checkNotNull( field ).getModifiers();

		if ( Modifier.isStatic( modifiers ) || Modifier.isTransient( modifiers ) )
			return true;

		return false;
	}

	private void persistMethods()
	{
		ReflectionUtils.doWithMethods( persistentClass, new MethodCallback()
		{
			@Override
			public void doWith( Method method ) throws IllegalArgumentException, IllegalAccessException
			{
				if ( method.getAnnotation( PrePersist.class ) != null )
				{
					prepersistMethods.add( method );
				}
			}
		} );
	}

	private void findIdentifier()
	{
		ReflectionUtils.doWithFields( persistentClass, new FieldCallback()
		{
			@Override
			public void doWith( Field field ) throws IllegalArgumentException, IllegalAccessException
			{
				Id identifier;

				identifier = field.getAnnotation( Id.class );

				if ( identifier != null )
					identifierField = field;
			}
		} );
	}

	@Override
	public Field getIdentifier()
	{
		return identifierField;
	}

	public String getIdentifierPropertyName()
	{
		return identifierField.getName();
	}

	public Type getIdentifierType()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String getIdentifierSyntax()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public List<String> getObjectClassHierarchy()
	{
		assert objectClassHierarchy != null && !objectClassHierarchy.isEmpty();

		return Lists.newArrayList( objectClassHierarchy );
	}

	public ImmutableSet<String> getAuxiliaryClasses()
	{
		assert auxiliaryClasses != null;

		return auxiliaryClasses;
	}

	public Constructor<T> getDefaultConstructor()
	{
		assert defaultConstructor != null;

		return defaultConstructor;
	}

	public String getStructuralClass()
	{
		assert objectClassHierarchy != null && !objectClassHierarchy.isEmpty();

		return Iterables.getLast( objectClassHierarchy );
	}

	@Override
	public Class<T> getEntryClass()
	{
		return this.persistentClass;
	}

	@Override
	public Filter getByExampleFilter()
	{
		return null;
	}

	@Override
	public AttributeMetadata<T> getAttributeMetadataByAttributeName( final String attributeId )
	{
		return attributeMetadataByAttributeName.get( attributeId );
	}

	@Override
	public AttributeMetadata<T> getAttributeMetadataByPropertyName( String propertyName )
	{
		return attributeMetadataByPropertyName.get( propertyName );
	}

	@Override
	public ImmutableSet<String> getProperties()
	{
		return ImmutableSet.copyOf( attributeMetadataByPropertyName.keySet() );
	}

	@Override
	public Method[] prepersistMethods()
	{
		return prepersistMethods.toArray( new Method[] {} );
	}

	@Override
	public boolean isCacheable()
	{
		return cacheable;
	}

	@Override
	public boolean isStrict()
	{
		return strict;
	}
}
