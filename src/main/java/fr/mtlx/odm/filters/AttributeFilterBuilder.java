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

import java.util.Arrays;

import javax.annotation.Nullable;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;

class AttributeFilterBuilder<T> implements CompareCriterion<T> {

    public final static char WILDCARD = '*';

    protected final String attribute;

    interface Encoder {

	String encode(final Object value);

    }

    public AttributeFilterBuilder(final String attribute) {
	this.attribute = checkNotNull(attribute);
    }

    protected Encoder build(final Comparison op) {
	return (final Object value) -> {
	    StringBuilder sb = new StringBuilder("(");

	    sb.append(attribute).append(op.getOperator()).append(value).append(")");

	    return sb.toString();
	};
    }

    @Override
    public Filter equalsTo(Object value) {
	return () -> build(Comparison.equals).encode(escapeSpecialChars(value));
    }

    @Override
    public Filter approx(Object value) {
	return () -> build(Comparison.approx).encode(escapeSpecialChars(value));
    }

    @Override
    public Filter greaterOrEquals(Object value) {
	return () -> build(Comparison.greater).encode(escapeSpecialChars(value));
    }

    @Override
    public Filter lessOrEquals(Object value) {
	return () -> build(Comparison.less).encode(escapeSpecialChars(value));
    }

    @Override
    public Filter present() {
	return () -> build(Comparison.equals).encode("*");
    }

    @Override
    public Filter substrings(final Object... values) {
	return () -> build(Comparison.equals)
		.encode(WILDCARD
			+ Joiner.on(WILDCARD)
				.skipNulls()
				.join(Arrays.asList(values).stream().map(AttributeFilterBuilder::escapeSpecialChars).iterator())
			+ WILDCARD);
    }

    @Override
    public Filter startsWith(Object value) {
	return () -> build(Comparison.equals).encode(escapeSpecialChars(value) + "*");
    }

    @Override
    public Filter endsWith(Object value) {
	return () -> build(Comparison.equals).encode("*" + escapeSpecialChars(value));
    }

    public static Filter objectClass(final String objectClass) {
	return new AttributeFilterBuilder<>("objectClass").equalsTo(objectClass);
    }

    
    public static Encoder specialCharsEncoder  = (Object value) -> escapeSpecialChars(value);
    
    
    private static String escapeSpecialChars(@Nullable Object value) {
	if (value == null)
	    return null;

	String strValue = value.toString();

	strValue = CharMatcher.is('*').replaceFrom(strValue, "\2a");

	strValue = CharMatcher.is('(').replaceFrom(strValue, "\28");

	strValue = CharMatcher.is(')').replaceFrom(strValue, "\29");

	strValue = CharMatcher.is('\\').replaceFrom(strValue, "\5c");

	strValue = CharMatcher.is('\0').replaceFrom(strValue, "\00");

	return strValue;
    }
}
