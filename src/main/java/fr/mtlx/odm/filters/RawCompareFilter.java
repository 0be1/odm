package fr.mtlx.odm.filters;

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

class RawCompareFilter extends FilterImpl {
	private final Comparison op;

	private final String attribute;

	private final Object value;

	RawCompareFilter(Comparison op, final String attribute, final Object value) {
		this.attribute = checkNotNull(attribute);
		this.value = value;
		this.op = op;
	}

	@Override
	public String encode() {
		StringBuilder sb = new StringBuilder("(");

		sb.append(attribute).append(op.getOperator()).append(value).append(")");

		return sb.toString();
	}
}
