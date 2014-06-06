package fr.mtlx.odm.attributes;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;

import javax.annotation.Nullable;

import com.google.common.base.Strings;

public class LabeledURI implements Attribute
{
	private URI uri;

	private String label;

	public LabeledURI( final URI uri, @Nullable final String label )
	{
		this.setUri( uri ); 
		this.setLabel( label );
	}

	public LabeledURI( final URI uri )
	{
		this( uri, null );
	}

	public URI getUri()
	{
		return uri;
	}

	public void setUri( final URI uri )
	{
		this.uri = checkNotNull( uri, "uri is null" );
	}

	public String getLabel()
	{
		return label;
	}

	public void setLabel( @Nullable final String label )
	{
		this.label = label;
	}

	@Override
	public String getOID()
	{
		return "1.3.6.1.4.1.250.1.57";
	}

	@Override
	public String toString()
	{
		if ( getUri() == null )
			return "";

		final StringBuilder retval = new StringBuilder();

		if ( !Strings.isNullOrEmpty( getLabel() ) )
			retval.append( getLabel() ).append( ':' );

		retval.append( getUri() );

		return retval.toString();
	}

	@Override
	public String getSyntax()
	{
		return "1.3.6.1.4.1.1466.115.121.1.15";
	}
}
