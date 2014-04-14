package fr.mtlx.odm.converters;

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

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.ldap.LdapName;


public class LdapNameConverter extends AbstractConverter
{
	public LdapNameConverter( )
	{}

	@Override
	public String getSyntax()
	{
		return "1.3.6.1.4.1.1466.115.121.1.12"; // Distinguished Name
	}

	@Override
	public Object toDirectory( final Object object ) throws ConvertionException
	{
		final Name retval = (Name)super.toDirectory( object );

		return retval != null ? retval.toString() : retval;
	}

	@Override
	public Object fromDirectory( final Object value ) throws ConvertionException
	{
		final Name dn;
		final String retval = (String)super.fromDirectory( value );
		
		if ( retval == null)
			return retval;
		
		try
		{
			dn = new LdapName( retval );
		}
		catch ( InvalidNameException e )
		{
			throw new ConvertionException( e );
		}

		return dn;
	}

	@Override
	public Class<?> directoryType()
	{
		return String.class;
	}

	@Override
	public Class<?> objectType()
	{
		return Name.class;
	}
}
