package fr.mtlx.odm.converters;

import java.net.URI;
import java.util.Iterator;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import fr.mtlx.odm.attributes.LabeledURI;
import java.net.URISyntaxException;

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

public class LabeledURIConverter extends AttributeConverter<String, LabeledURI> {
	protected LabeledURIConverter(final Class<String> directoryType,
			final Class<LabeledURI> objectType) {
		super(directoryType, objectType);
	}

	public LabeledURIConverter() {
		this(String.class, LabeledURI.class);
	}

	@Override
	public String to(final LabeledURI object) throws ConvertionException {
		return object.toString();
	}

	@Override
	public LabeledURI from(final String value) throws ConvertionException {
		final Iterator<String> iterator = Splitter.on(' ').limit(1)
				.trimResults().split(value).iterator();

		URI uri;
		try {
			uri = new URI(iterator.next());
		} catch (URISyntaxException e) {
			throw new ConvertionException(e);
		}

		final String label;

		if (iterator.hasNext())
			label = Strings.emptyToNull(iterator.next());
		else
			label = null;

		return new LabeledURI(uri, label);
	}
}
