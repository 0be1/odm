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

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import javax.naming.Name;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

public class AttributeMetadata<T> implements Serializable
{
	public static final ImmutableMap<Type, String> defaultSyntaxes =
			new ImmutableMap.Builder<Type, String>()
					.put( Integer.TYPE, "1.3.6.1.4.1.1466.115.121.1.27" )
					.put( Integer.class, "1.3.6.1.4.1.1466.115.121.1.27" )
					.put( Short.TYPE, "1.3.6.1.4.1.1466.115.121.1.27" )
					.put( Short.class, "1.3.6.1.4.1.1466.115.121.1.27" )
					.put( Byte.TYPE, "1.3.6.1.4.1.1466.115.121.1.27" )
					.put( Byte.class, "1.3.6.1.4.1.1466.115.121.1.27" )
					.put( Boolean.TYPE, "1.3.6.1.4.1.1466.115.121.1.7" )
					.put( Boolean.class, "1.3.6.1.4.1.1466.115.121.1.7" )
					.put( String.class, "1.3.6.1.4.1.1466.115.121.1.15" )
					.put( byte[].class, "1.3.6.1.4.1.1466.115.121.1.5" )
					.put( Name.class, "1.3.6.1.4.1.1466.115.121.1.12" )
					.put( Date.class, "1.3.6.1.4.1.1466.115.121.1.24" )
					.put( Long.TYPE, "1.3.6.1.4.1.1466.115.121.1.36" )
					.put( Long.class, "1.3.6.1.4.1.1466.115.121.1.36" )
					.build();

	/**
	 * 
	 */
	private static final long serialVersionUID = -7657747737269147279L;

	private final String propertyName;

	private final String attributeName;

	private final String[] attributeAliases;

	private final Type objectType;

	private final Type directoryType;

	private final String syntax;

	private final Class<? extends Collection<?>> collectionType;

	@SuppressWarnings( "unchecked" )
	AttributeMetadata( final Class<T> persistentClass, final Field f ) throws MappingException
	{
		String name = null;
		Attribute attribute;
		Class<?> c;

		this.propertyName = checkNotNull( f ).getName();

		c = f.getType();

		attribute = f.getAnnotation( Attribute.class );

		if ( attribute != null )
		{
			name = attribute.name();
			
			if ( name != null )
			{
				name = Strings.emptyToNull( name.trim() );
			}
			
			attributeAliases = attribute.aliases();
		}
		else
		{
			attributeAliases = new String[] {};
		}
		
		attributeName = (name != null) ? name : f.getName();
		
		assert attributeName != null && attributeName.length() > 0;

		if ( Arrays.asList( c.getInterfaces() ).contains( Collection.class ) || c.equals( Collection.class ) )
		{
			collectionType = (Class<? extends Collection<?>>)c;

			Type genericFieldType = f.getGenericType();

			if ( genericFieldType instanceof ParameterizedType )
			{
				ParameterizedType pType = (ParameterizedType)genericFieldType;

				Type[] fieldArgTypes = pType.getActualTypeArguments();

				// Collection<?>, un seul paramètre générique
				if ( fieldArgTypes.length > 1 )
					throw new UnsupportedOperationException( "multivlaued attributes are persisted only as List or Set" );

				objectType = inferGenericType( persistentClass, fieldArgTypes[ 0 ] );
			}
			else
			{
				// raw Collection

				objectType = null;
			}
		}
		else
		{
			collectionType = null;

			objectType = inferGenericType( persistentClass, c );
		}

		if ( attribute != null && !Strings.isNullOrEmpty( attribute.syntax() ) )
		{
			syntax = attribute.syntax();
		}
		else
		{
			syntax = guessSyntaxFromType( objectType );
		}

		if ( attribute != null )
		{
			switch ( attribute.type() )
			{
			case BINARY:
				directoryType = byte[].class;
				break;
			case STRING:
				directoryType = String.class;
				break;
			default:
				directoryType = null;
			}
		}
		else
			directoryType = String.class; // default
	}

	private Type inferGenericType( Class<?> persistentClass, Type type )
	{

		if ( type instanceof ParameterizedType )
		{
			ParameterizedType pType = (ParameterizedType)type;

			Type[] argumentTypes = pType.getActualTypeArguments();

			if ( argumentTypes.length == 1 )
				return argumentTypes[ 0 ];

			return null;
		}
		else if ( type instanceof TypeVariable<?> )
		{
			ParameterizedType stype = (ParameterizedType)persistentClass.getGenericSuperclass();

			Type[] actualTypeArguments = stype.getActualTypeArguments();

			// XXX:

			return actualTypeArguments[ 0 ];
			// actualTypeArguments[0]
			// for ( Type btype : vType.getBounds() )
			// {
			// if ( argumentTypes.length == 1 )
			// {
			// for (TypeVariable<?> tv : persistentClass.getA )
			// {
			// if (tv.equals( argumentTypes[ 0 ] ))
			// return tv;
			// }
			//
			// return argumentTypes[ 0 ];
			// }

			// return null;
		}

		return type;
	}

	public AttributeMetadata( final Class<T> persistentClass, String propertyName ) throws MappingException, SecurityException, NoSuchFieldException
	{
		this( persistentClass, persistentClass.getDeclaredField( propertyName ) );
	}

	private String guessSyntaxFromType( Type type )
	{
		return defaultSyntaxes.get( type );
	}

	public String getAttirbuteName()
	{
		return attributeName;
	}

	public String getPropertyName()
	{
		return propertyName;
	}

	public Type getObjectType()
	{
		return objectType;
	}

	public Type getDirectoryType()
	{
		return directoryType;
	}

	public String getSyntax()
	{
		return syntax;
	}

	public boolean isMultivalued()
	{
		return collectionType != null;
	}

	public Class<? extends Collection<?>> getCollectionType()
	{
		return collectionType;
	}

	public String[] getAttributeAliases()
	{
		return attributeAliases;
	}
	
	public Collection<?> newCollectionInstance()
	{
		Class<? extends Collection<?>> ctype = getCollectionType();
		
		if ( ctype.isAssignableFrom( ArrayList.class ) )
		{
			return new ArrayList<Object>();
		}
		else if ( ctype.isAssignableFrom( HashSet.class ) )
		{
			return new HashSet<Object>();
		}
		
		return null;
	}
}
