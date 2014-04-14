package fr.mtlx.odm.model;

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

import static fr.mtlx.odm.Attribute.Type.*;

import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.google.common.collect.Iterables;

import fr.mtlx.odm.Attribute;
import fr.mtlx.odm.Entry;

@Entry(objectClasses =
{ "person" } )
public class Person extends Top
{
	private static final long serialVersionUID = 7030104392521108718L;

	@Attribute(aliases="sn")
	@NotNull
	private String surname;
	
	@Attribute(aliases="cn")
	@NotNull
	private String commonName;
	
	@Attribute
	private Set<String> description;
	
	@Attribute
	private String seeAlso;
	
	@Attribute(type = BINARY)
	private byte[] userPassword;
	
	@Attribute
	private List<String> telephoneNumber;
	
		
	public String getSurname()
	{
		return surname;
	}

	public void setSurname( String surname )
	{
		this.surname = surname;
	}
	
	public String getSn()
	{
		return surname;
	}

	public void setSn( String sn )
	{
		this.surname = sn;
	}

	public String getCommonName()
	{
		return commonName;
	}

	public void setCommonName( String commonName )
	{
		this.commonName = commonName;
	}
	
	public String getCn()
	{
		return commonName;
	}

	public void setCn( String cn )
	{
		this.commonName = cn;
	}

	public List<String> getTelephoneNumber()
	{
		return telephoneNumber;
	}

	public String getFirstTelephoneNumber()
	{
		if ( telephoneNumber != null )
			return Iterables.getFirst( telephoneNumber, null );
		
		return null;
	}
	
	public void setTelephoneNumber( List<String> telephoneNumber )
	{
		this.telephoneNumber = telephoneNumber;
	}

	public Set<String> getDescription()
	{
		return description;
	}

	public void setDescription( Set<String> description )
	{
		this.description = description;
	}

	public String getSeeAlso()
	{
		return seeAlso;
	}

	public void setSeeAlso( String seeAlso )
	{
		this.seeAlso = seeAlso;
	}

	public byte[] getUserPassword()
	{
		return userPassword;
	}

	public void setUserPassword( byte[] userPassword )
	{
		this.userPassword = userPassword;
	}
}
