package fr.mtlx.odm;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.persistence.PrePersist;

import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ClassMetadataBuilder<T>
{
	final Class<T> persistentClass;
	
	public ClassMetadataBuilder(final Class<T> persistentClass)
	{
		this.persistentClass = checkNotNull( persistentClass );
	}
	
	public PartialClassMetadata<T> build() throws MappingException
	{
		final PartialClassMetadata<T> metadata = new PartialClassMetadata<T>( persistentClass );

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
		
		if ( classHierarchy.isEmpty() )
			throw new MappingException( String.format( "%s is not a persistent class", persistentClass ) );

		metadata.setObjectClassHierarchy( ImmutableList.copyOf( Lists.reverse( classHierarchy ) ) );

		metadata.setAuxiliaryClasses( ImmutableSet.copyOf( auxiliaryClasses ) );

		try
		{
			metadata.setDefaultConstructor( persistentClass.getConstructor( new Class[] {} ) );
		}
		catch ( SecurityException e )
		{
			throw new MappingException( e );
		}
		catch ( NoSuchMethodException e )
		{
			throw new MappingException( String.format( "no default contructor found for %s", persistentClass ), e );
		}

		metadata.setStrict( isStrict );
		
		metadata.setCacheable( isCacheable() );
		
		metadata.setPrepersistMethods( persistMethods() );
		
		return metadata;
	}
	
	private List<Method> persistMethods()
	{
		final List<Method> prepersistMethods = Lists.newArrayList();
		
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
		
		return prepersistMethods;
	}

	private boolean isCacheable()
	{
		return persistentClass.getAnnotation( javax.persistence.Cacheable.class ) != null;
	}
}
