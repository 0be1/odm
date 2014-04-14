package fr.mtlx.odm.utils;

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

import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

public class LdapUtils
{
	private static LdapName buildLdapName( final Name dn )
	{
		final LdapName name;

		if ( dn instanceof LdapName )
		{
			name = (LdapName)dn;
		}
		else
		{
			try
			{
				name = new LdapName( dn.toString() );
			}
			catch ( InvalidNameException e )
			{
				return null;
			}
		}

		return name;
	}

	public static Name getDistinguishedNameParent( final Name dn )
	{
		final LdapName name = buildLdapName( dn );

		return name.getPrefix( name.size() - 1 );
	}

	public static Name getDistinguishedNamePrefix( final Name dn, String from )
	{
		int i = 0;
		
		LdapName name = buildLdapName( dn );
		
		List<Rdn> rdns = name.getRdns();
		
		for ( ; i < rdns.size(); i++  )
		{
			if ( from.equalsIgnoreCase( rdns.get(i).getType() ) )
			{
				return name.getPrefix( i + 1 );
			}
		}

		return null;
	}
}
