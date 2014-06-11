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
import com.google.common.base.CharMatcher;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

class SimpleFilterBuilder<T> implements CompareCriterion<T> {

    public final static char Any = '*';
    
    interface Encoder {

        String encode(Object value);
    }


    protected final String attribute;

    public SimpleFilterBuilder(final String attribute) {
	this.attribute = checkNotNull(attribute);
    }

    protected Filter compareFilter(final Comparison op, final Filter valueFilter) {
	return filter(new CompositeFilter(filterComp(op), valueFilter));
    }

    protected Filter filterComp(final Comparison op) {
	return (StringBuilder sb) -> sb.append(attribute).append(op.getOperator());
    }

    protected Filter filter(Filter filter) {

	return sb -> {
	    sb.append('(');

	    try {
		filter.encode(sb);
	    } finally {
		sb.append(')');
	    }
	};

    }

    @Override
    public Filter equalsTo(Object value) {
	return compareFilter(Comparison.equals, valueFilter(value));
    }

    @Override
    public Filter approx(Object value) {
	return compareFilter(Comparison.approx, valueFilter(value));
    }

    @Override
    public Filter greaterOrEquals(Object value) {
	return compareFilter(Comparison.greater, valueFilter(value));
    }

    @Override
    public Filter lessOrEquals(Object value) {
	return compareFilter(Comparison.less, valueFilter(value));
    }

    @Override
    public Filter present() {
	return compareFilter(Comparison.equals, sb -> sb.append('*'));
    }

    public Filter substrings(@Nullable final String startsWith, final List<String> substrings, @Nullable final String endsWith) {
	final Filter startsWithFilter = sb -> sb.append(escapeSpecialChars(startsWith));

	final Filter endsWithFilter = sb -> sb.append(escapeSpecialChars(endsWith));
	
	final Filter substringsFilter = new Filter() {
	    @Override
	    public void encode(StringBuilder sb) {
		for (String substring : substrings) {
		    String s = escapeSpecialChars(substring);
		    
		    if (!Strings.isNullOrEmpty(s)) {
			sb.append(s).append(Any);
		    }
		}
	    }
	}; 
		
	return compareFilter(
		Comparison.equals,
		new CompositeFilter(startsWithFilter, substringsFilter, endsWithFilter));
    }

    @Override
    public Filter substrings(final String... values) {
	if (values.length == 0) {
	    return noOpFilter;
	}

	return substrings(null, Arrays.asList(values), null);
    }

    @Override
    public Filter startsWith(Object value) {
	return compareFilter(Comparison.equals, new CompositeFilter(valueFilter(value), sb -> sb.append('*')));
    }

    @Override
    public Filter endsWith(Object value) {
	return compareFilter(Comparison.equals, new CompositeFilter(sb -> sb.append('*'), valueFilter(value)));
    }

    public static Filter objectClass(final String objectClass) {
	return new SimpleFilterBuilder<>("objectClass").equalsTo(objectClass);
    }

    public static final Filter noOpFilter = sb -> {
    };

    private Filter valueFilter(@Nullable final Object value) {
	return sb -> sb.append(valueEncoder().encode(value));
    }

    //hook
    protected Encoder valueEncoder() {
	return SimpleFilterBuilder::escapeSpecialChars;
    }
    
    
    protected final static String escapeSpecialChars(@Nullable Object value) {
	if (value == null) {
	    return null;
	}

	String strValue = value.toString();

	strValue = CharMatcher.is('*').replaceFrom(strValue, "\2a");

	strValue = CharMatcher.is('(').replaceFrom(strValue, "\28");

	strValue = CharMatcher.is(')').replaceFrom(strValue, "\29");

	strValue = CharMatcher.is('\\').replaceFrom(strValue, "\5c");

	strValue = CharMatcher.is('\0').replaceFrom(strValue, "\00");

	return strValue;
    }
}
