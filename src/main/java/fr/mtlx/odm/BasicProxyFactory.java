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
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.naming.InvalidNameException;
import javax.naming.NamingException;

import org.springframework.ldap.core.DirContextOperations;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyObject;

@SuppressWarnings( "rawtypes" )
class BasicProxyFactory<T>
{
	private class DirContextHandler<S> implements MethodHandler
	{
		private final Map<String, Boolean> invoked = Maps.newHashMap();
		private final ClassMetadata<S> metadata;
		private final Class<S> proxiedClass;
		private final Object proxiedObject;

		private final ProxyResolver<S> resolver;

		public DirContextHandler( ProxyObject proxiedObject, Class<S> proxiedClass, DirContextOperations context, Session session )
		{
			this.proxiedObject = checkNotNull( proxiedObject, "proxiedObject is null" );
			this.proxiedClass = checkNotNull( proxiedClass, "proxiedClass is null" );

			this.metadata = session.getSessionFactory().getClassMetadata( proxiedClass );

			resolver = new ProxyResolver<S>( context, metadata, session );
		}

		private String capitalizeFirstLetter( String original )
		{
			if ( original.length() == 0 )
				return original;

			return original.substring( 0, 1 ).toUpperCase() + original.substring( 1 );
		}

		private String getPropertyName( final Method method )
		{
			final String name = method.getName();

			if ( name.startsWith( "get" ) || name.startsWith( "set" ) )
			{
				return name.substring( 3 );
			}
			else if ( name.startsWith( "is" ) ) { return name.substring( 2 ); }

			return null;
		}

		private Method getSetter( final String property ) throws SecurityException, NoSuchMethodException
		{
			final AttributeMetadata<S> att = metadata.getAttributeMetadataByPropertyName( property );
			if ( att.isMultivalued() )
			{
				return proxiedClass.getMethod( getSetterName( property ), att.getCollectionType() );
			}
			else
			{
				return proxiedClass.getMethod( getSetterName( property ), (Class)att.getObjectType() );
			}
		}

		private String getSetterName( final String property )
		{
			return "set" + capitalizeFirstLetter( property );
		}

		private boolean hasGetterSignature( Method method )
		{
			return method.getParameterTypes().length == 0 && method.getReturnType() != null;
		}

		private boolean hasSetterSignature( Method method )
		{
			return method.getParameterTypes().length == 1 && ( method.getReturnType() == null || method.getReturnType() == void.class );
		}

		@Override
		public Object invoke( Object object, final Method method, final Method method1, final Object[] args ) throws Exception
		{
			final String name = method.getName();

			if ( "toString".equals( name ) )
			{
				return proxiedClass.getName() + "@" + System.identityHashCode( object );
			}
			else if ( "equals".equals( name ) )
			{
				return proxiedObject == object;
			}
			else if ( "hashCode".equals( name ) )
			{
				return System.identityHashCode( object );
			}
			else if ( !isHandled( method ) ) { return method1.invoke( object, args ); }

			final String property = getPropertyName( method );

			assert property != null;

			if ( !invoked.containsKey( property ) )
			{
				setProperty( object, property );

				invoked.put( property, true );
			}

			return method1.invoke( proxiedObject );
		}

		private void setProperty( final Object object, final String property ) throws NamingException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException
		{
			final Object value = resolver.getProperty( property );

			getSetter( property ).invoke( object, value );
		}

		private boolean isHandled( Method method )
		{
			if ( hasGetterSignature( method ) )
			{
				final String property = getPropertyName( method );

				if ( property != null )
				{
					if ( property.equals( metadata.getIdentifierPropertyName() ) ) { return false; }

					return metadata.getAttributeMetadataByPropertyName( property ) != null;
				}
			}

			return false;
		}
	}

	private static final MethodFilter FINALIZE_FILTER = new MethodFilter()
	{
		@Override
		public boolean isHandled( Method m )
		{
			// skip finalize methods
			return ! ( m.getParameterTypes().length == 0 && m.getName().equals( "finalize" ) );
		}
	};

	private final Set<Class> interfaces;

	private final Class proxyClass;

	private final Class<T> superClass;

	public BasicProxyFactory( final Class<T> superClass, @Nullable final Class[] interfaces )
	{
		this.superClass = checkNotNull( superClass );

		this.interfaces = Sets.newHashSet( interfaces );

		javassist.util.proxy.ProxyFactory factory = new javassist.util.proxy.ProxyFactory();

		factory.setFilter( FINALIZE_FILTER );

		factory.setSuperclass( this.superClass );

		if ( interfaces != null && interfaces.length > 0 )
		{
			factory.setInterfaces( interfaces );
		}

		proxyClass = factory.createClass();
	}

	public Class[] getInterfaces()
	{
		return interfaces.toArray( new Class[] {} );
	}

	public T getProxy( final Session session, final DirContextOperations dirContext ) throws InstantiationException, IllegalAccessException, InvalidNameException
	{
		ProxyObject proxy = (ProxyObject)proxyClass.newInstance();
		proxy.setHandler( new DirContextHandler<T>( proxy, superClass, dirContext, session ) );

		@SuppressWarnings( "unchecked" )
		T retval = (T)proxy;

		return retval;
	}

	public Class getProxyClass()
	{
		return proxyClass;
	}

	public Class<T> getSuperClass()
	{
		return superClass;
	}

	public boolean isImplementing( final Class<?> clazz )
	{
		for ( Class iClazz : interfaces )
		{
			if ( clazz.isAssignableFrom( iClazz ) ) { return true; }
		}

		return false;
	}
}
