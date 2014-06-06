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

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import fr.mtlx.odm.AttributeMetadata;

public class PropertySubstringsWithFilter<T> extends PropertyCompareFilter<T> {
	public final static char WILDCARD = '*';

	PropertySubstringsWithFilter(final Class<T> persitentClass,
			final String property, final Object... values) {
		super(persitentClass, Comparison.equals, property, values);
	}

	@Override
	protected String formatValue(final String encodedValue) {
		return WILDCARD + encodedValue + WILDCARD;
	}

	@Override
	protected String encodeValue(final Object value,
			final AttributeMetadata attribute) {
		List<String> encodedValues = Lists.newArrayList();

		Object[] values = (Object[]) value;

		for (Object val : values) {
			encodedValues.add(super.encodeValue(val, attribute));
		}

		return Joiner.on(WILDCARD).skipNulls().join(encodedValues);
	}
}
