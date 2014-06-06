package fr.mtlx.odm;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import javax.naming.Name;
import javax.naming.ldap.LdapName;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import fr.mtlx.odm.attributes.LabeledURI;
import fr.mtlx.odm.converters.AttributeConverter;
import fr.mtlx.odm.converters.Converter;
import fr.mtlx.odm.converters.DefaultConverters;
import fr.mtlx.odm.converters.EntryResolverConverter;
import fr.mtlx.odm.converters.IdentityConverter;
import fr.mtlx.odm.converters.LabeledURIConverter;

public class AttributeMetadataFactory<T>
{
	private final SessionFactoryImpl sessionFactory;

	private final Class<T> persistentClass;

	public AttributeMetadataFactory( final Class<T> persistantClass, final SessionFactoryImpl sessionFactory )
	{
		this.sessionFactory = checkNotNull( sessionFactory );

		this.persistentClass = checkNotNull( persistantClass );
	}

	class Builder
	{
		private final Attribute attribute;

		private final Field f;

		private final Class<?> c;

		public Builder( Field f )
		{
			this.f = checkNotNull( f );

			this.attribute = f.getAnnotation( Attribute.class );

			this.c = f.getType();
		}

		public AttributeMetadata build() throws MappingException
		{
			final AttributeMetadata meta = new AttributeMetadata();

			meta.setPersistentClass( persistentClass );

			meta.setPropertyName( f.getName() );

			final String name = getName();

			meta.setAttributeName( name );

			meta.setAttributeAliases( getAliases() );

			final Type objectType = getObjectType( c );

			if ( objectType == null )
				throw new MappingException( String.format( "Could not determine type for attibute %s", name ) );

			meta.setObjectType( objectType );

			String syntax = getSyntax( objectType );

			if ( syntax == null )
				throw new MappingException( String.format( "No syntax specified  for attribute %s", name ) );

			meta.setSyntax( syntax );

			meta.setSyntaxConverter( getSyntaxConverter( syntax ) );

			meta.setAttributeConverter( getAttributeConverter( objectType ) );

			meta.setCollectionType( getCollectionType() );

			meta.setDirectoryType( getDirectoryType() );

			return meta;
		}

		private Converter getSyntaxConverter( final String syntax ) throws MappingException
		{
			final Converter retval = sessionFactory.getConverter( syntax );

			if ( retval == null )
				throw new MappingException( String.format( "Syntax %s not supported", syntax ) );

			return retval;
		}

		private Converter getAttributeConverter( final Type objectType ) throws MappingException
		{
			final Converter retval = sessionFactory.getConverter( objectType );

			if ( retval == null && ! sessionFactory.isPersistentClass( (Class<?>)objectType ) ) { throw new MappingException( String.format( "No converter found for type %s", objectType ) ); }

			return retval;
		}

		private String getName()
		{
			String name = null;

			if ( attribute != null && !Strings.isNullOrEmpty( attribute.name() ) )
			{
				name = attribute.name().trim();
			}

			if ( Strings.isNullOrEmpty( name ) )
			{
				name = f.getName();
			}

			assert !Strings.isNullOrEmpty( name );

			return name;
		}

		private String[] getAliases()
		{
			return attribute != null ? attribute.aliases() : new String[] {};
		}

		private String getSyntax( final Type objectType ) throws MappingException
		{
			String retval = null;

			if ( attribute != null && !Strings.isNullOrEmpty( attribute.syntax() ) )
			{
				retval = Strings.emptyToNull( attribute.syntax() );
			}

			if ( retval == null )
			{
				retval = guessSyntaxFromType( objectType );
			}

			if ( retval == null )
			{
				final Converter attributeConverter = getAttributeConverter( objectType );

				if ( attributeConverter == null && sessionFactory.isPersistentClass( (Class<?>)objectType ) )
				{
					retval = guessSyntaxFromType( LdapName.class );
				}
				else
					retval = guessSyntaxFromType( attributeConverter.directoryType() );
			}

			if ( retval == null )
			{
				throw new MappingException("unsupported attribute type" + objectType );
			}
			
			return retval;
		}

		private final Type getObjectType( Class<?> c )
		{
			Type retval = null;

			if ( Arrays.asList( c.getInterfaces() ).contains( Collection.class ) || c.equals( Collection.class ) )
			{
				final Type genericFieldType = f.getGenericType();

				if ( genericFieldType instanceof ParameterizedType )
				{
					final ParameterizedType pType = (ParameterizedType)genericFieldType;

					Type[] fieldArgTypes = pType.getActualTypeArguments();

					// Collection<?>, un seul paramètre générique
					if ( fieldArgTypes.length > 1 )
						throw new UnsupportedOperationException( "multivlaued attributes are persisted only as List or Set" );

					retval = inferGenericType( fieldArgTypes[ 0 ] );
				}
			}

			if ( retval == null )
			{
				retval = inferGenericType( c );
			}

			if ( retval == null )
			{
				retval = Object.class;
			}

			return retval;
		}

		private Type getDirectoryType()
		{
			if ( attribute != null )
			{
				switch ( attribute.type() )
				{
				case BINARY:
					return byte[].class;
				case STRING:
					return String.class;
				default:
					return null;
				}
			}
			else
			{
				return String.class; // default
			}
		}

		private Class<? extends Collection<?>> getCollectionType()
		{
			if ( Arrays.asList( c.getInterfaces() ).contains( Collection.class ) || c.equals( Collection.class ) )
			{
				return (Class<? extends Collection<?>>)c;
			}
			else
			{
				return null;
			}
		}
	}

	private static String guessSyntaxFromType( Type type )
	{
		return DefaultConverters.defaultSyntaxes.get( type );
	}

	private Type inferGenericType( Type type )
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

	public AttributeMetadata build( final Field f ) throws MappingException
	{
		checkNotNull( f, "f is null" );

		return new Builder( f ).build();
	}
}
