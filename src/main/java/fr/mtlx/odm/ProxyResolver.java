package fr.mtlx.odm;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DirContextOperations;

import com.google.common.collect.Sets;

import fr.mtlx.odm.converters.Converter;

public class ProxyResolver<T>
{
	private final Logger log = LoggerFactory.getLogger( ProxyResolver.class );

	private final ClassMetadata<T> metadata;
	private final Session session;
	private final Attributes attributes;

	private final DirContextOperations context;

	public ProxyResolver( final DirContextOperations context, final ClassMetadata<T> metadata, final Session session )
	{
		this.metadata = checkNotNull( metadata, "metadata is null" );

		this.session = checkNotNull( session, "session is null" );

		this.context = checkNotNull( context, "context is null" );

		attributes = this.context.getAttributes();
	}

	private Attribute getAttribute( final AttributeMetadata<T> metadata )
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
		final AttributeMetadata<T> attributeMetadata = metadata.getAttributeMetadataByPropertyName( name );

		if ( attributeMetadata == null ) { throw new MappingException( "the property " + name + " is not mapped to a directory attribute" ); }

		final Converter converter = session.getConverter( attributeMetadata.getSyntax(), attributeMetadata.getObjectType(), attributeMetadata.getDirectoryType() );

		if ( log.isDebugEnabled() )
		{
			log.debug( "attribute {} matching type {}", attributeMetadata.getAttirbuteName(), attributeMetadata.getObjectType() );
		}

		attr = getAttribute( attributeMetadata );
		
		if ( attr == null )
		{
			if ( log.isDebugEnabled() )
			{
				log.debug( "attribute {} matching type {}", attributeMetadata.getAttirbuteName(), attributeMetadata.getObjectType() );
			}

			return null;
		}

		if ( attributeMetadata.isMultivalued() )
		{
			final Collection convertedValues = attributeMetadata.newCollectionInstance();

			final NamingEnumeration<?> values = attr.getAll();

			while ( values.hasMoreElements() )
			{
				final Object value = converter.fromDirectory( values.nextElement() );

				if ( value != null )
					convertedValues.add( value );
			}

			return convertedValues;
		}
		else
		{
			if ( attr.size() > 1 )
				throw new RuntimeException( String.format( "multiple values found for single valued attribute %s",
						attributeMetadata.getAttirbuteName() ) );

			final Object value = converter.fromDirectory( attr.get() );

			return value;
		}
	}
}
