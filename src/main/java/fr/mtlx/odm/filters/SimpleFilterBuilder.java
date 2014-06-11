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
import java.util.stream.Stream;
import javax.annotation.Nullable;

class SimpleFilterBuilder<T> implements CompareCriterion<T> {

    public final static char Any = '*';

    protected final String attribute;

    public SimpleFilterBuilder(final String attribute) {
        this.attribute = checkNotNull(attribute);
    }

    protected Filter filterType(final Comparison op, final Object value) {
        return (StringBuilder sb)
                -> sb.append('(').append(attribute).append(op.getOperator()).append(value).append(")");
    }

    @Override
    public Filter equalsTo(Object value) {
        return (StringBuilder sb) -> filterType(Comparison.equals, escapeSpecialChars(value)).encode(sb);
    }

    @Override
    public Filter approx(Object value) {
        return (StringBuilder sb) -> filterType(Comparison.approx, escapeSpecialChars(value)).encode(sb);
    }

    @Override
    public Filter greaterOrEquals(Object value) {
        return (StringBuilder sb) -> filterType(Comparison.greater, escapeSpecialChars(value)).encode(sb);
    }

    @Override
    public Filter lessOrEquals(Object value) {
        return (StringBuilder sb) -> filterType(Comparison.less, escapeSpecialChars(value)).encode(sb);
    }

    @Override
    public Filter present() {
        return (StringBuilder sb) -> filterType(Comparison.equals, "*").encode(sb);
    }

    public Filter substrings(@Nullable final String startsWith, final List<String> substrings, @Nullable final String endsWith) {
        final String escapedStartsWith = escapeSpecialChars(startsWith);

        final String escapedEndsWith = escapeSpecialChars(endsWith);

        final Stream<String> substreams = substrings.stream()
                .map(SimpleFilterBuilder::escapeSpecialChars)
                .filter(s -> !Strings.isNullOrEmpty(s));

        return sb -> {
            sb.append('(').append(attribute).append(Comparison.equals.getOperator());

            if (!Strings.isNullOrEmpty(escapedStartsWith)) {
                sb.append(escapedStartsWith);
            }

            substreams.collect(() -> sb,
                    (builder, value) -> builder.append(value).append(Any),
                    (builder1, builder2) -> builder1.append(builder2));

            if (!Strings.isNullOrEmpty(escapedEndsWith)) {
                sb.append(escapedEndsWith);
            }

            sb.append(')');
        };
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
        return (StringBuilder sb) -> filterType(Comparison.equals, escapeSpecialChars(value) + "*").encode(sb);
    }

    @Override
    public Filter endsWith(Object value) {
        return (StringBuilder sb) -> filterType(Comparison.equals, "*" + escapeSpecialChars(value)).encode(sb);
    }

    public static Filter objectClass(final String objectClass) {
        return new SimpleFilterBuilder<>("objectClass").equalsTo(objectClass);
    }

    public static final Filter noOpFilter = sb -> {
    };

    protected static String escapeSpecialChars(@Nullable Object value) {
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
