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

import java.io.Serializable;

import javax.naming.Name;
import javax.validation.constraints.NotNull;

import fr.mtlx.odm.Entry;
import fr.mtlx.odm.Id;

@Entry(objectClasses = { "top" })
public class Top extends IdProvider<Name> implements Serializable {
	private static final long serialVersionUID = -8268393254510637376L;

	@Id
	@NotNull
	private Name dn;

	public Top() {
		super(Name.class);
	}

	@Override
	public Name getDn() {
		return dn;
	}

	@Override
	public void setDn(final Name dn) {
		this.dn = dn;
	}
}
