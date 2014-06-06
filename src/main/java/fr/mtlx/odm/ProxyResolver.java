package fr.mtlx.odm;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.commons.beanutils.ConversionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DirContextOperations;

import com.google.common.collect.Sets;

import fr.mtlx.odm.converters.Converter;
import fr.mtlx.odm.converters.EntryResolverConverter;

class ProxyResolver<T>
{
	private final Logger log = LoggerFactory.getLogger( ProxyResolver.class );

	private final ClassMetadata<T> metadata;

	private final Attributes attributes;

	private final DirContextOperations context;

	private final Session session;

	ProxyResolver( final DirContextOperations context, final ClassMetadata<T> metadata, final Session session )
	{
		this.metadata = checkNotNull( metadata, "metadata is null" );

		this.session = checkNotNull( session, "session is null" );

		this.context = checkNotNull( context, "context is null" );

		attributes = this.context.getAttributes();
	}

	private Attribute getAttribute( final AttributeMetadata metadata )
	{
		final Set<String> names = Sets.newLinkedHashSet();

		names.add( metadata.getAttirbuteName() );

		names.addAll( Arrays.asList( metadata.getAttributeAliases() ) );

		for ( final String alias : names )
		{
			final Attribute attr = attributes.get( alias );

			if ( attr != null ) { return attr; }
		}

		return null;
	}

	Object getProperty( final String name ) throws NamingException, InstantiationException, IllegalAccessException
	{
		if ( metadata.getIdentifierPropertyName().equals( name ) ) { return context.getDn(); }

		final Attribute attr;
		final AttributeMetadata attributeMetadata = metadata.getAttributeMetadataByPropertyName( name );

		if ( attributeMetadata == null ) { throw new InstantiationException( "the property " + name + " is not mapped to a directory attribute" ); }

		final Converter converter = attributeMetadata.getSyntaxConverter();

		if ( log.isDebugEnabled() )
		{
			log.debug( "attribute {} with syntax {} matching type {}", attributeMetadata.getAttirbuteName(), attributeMetadata.getSyntax(), converter.objectType() );
		}

		attr = getAttribute( attributeMetadata );

		if ( attr == null )
		{
			if ( log.isDebugEnabled() )
			{
				log.debug( "attribute {} not found", attributeMetadata.getAttirbuteName() );
			}

			return null;
		}

		if ( attributeMetadata.isMultivalued() )
		{
			final Collection internalValues = attributeMetadata.newCollectionInstance();

			final NamingEnumeration<?> values = attr.getAll();

			while ( values.hasMoreElements() )
			{
				final Object internalValue = converter.fromDirectory( values.nextElement() );

				internalValues.add( convert( internalValue, attributeMetadata ) );
			}

			return internalValues;
		}
		else
		{
			if ( attr.size() > 1 )
				throw new ConversionException( String.format( "multiple values found for single valued attribute %s",
						attributeMetadata.getAttirbuteName() ) );

			final Object internalValue = converter.fromDirectory( attr.get() );

			return convert( internalValue, attributeMetadata );
		}
	}

	private Object convert( Object from, AttributeMetadata metadata )
	{
		if ( from == null ) { return null; } // XXX

		Class objectClass = from.getClass();
		
		if ( metadata.getObjectType().equals( objectClass ) ) { return from; }

		Converter converter = metadata.getAttributeConverter();
		
		if ( converter == null && session.getSessionFactory().isPersistentClass( objectClass ) )
		{
			return new EntryResolverConverter<Object>( objectClass, session );
		}
		
		return converter.fromDirectory( from );
	}
}
