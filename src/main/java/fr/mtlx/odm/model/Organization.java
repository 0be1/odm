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


import javax.persistence.Cacheable;

import fr.mtlx.odm.Attribute;
import fr.mtlx.odm.Entry;

@Entry(objectClasses =
{ "organization" }, ignoreNonMatched = true )
@Cacheable
public class Organization extends Top
{
	private static final long serialVersionUID = -1709060834240017575L;

	@Attribute(aliases="o")
	private String organizationName;
	
	@Attribute
	private String description;

	public String getOrganizationName()
	{
		return organizationName;
	}

	public void setOrganizationName( final String organizationName )
	{
		this.organizationName = organizationName;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription( final String description )
	{
		this.description = description;
	}
}
