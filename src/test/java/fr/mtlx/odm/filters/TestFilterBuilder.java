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
import fr.mtlx.odm.ClassMetadata;
import fr.mtlx.odm.MappingException;
import fr.mtlx.odm.SessionFactory;
import fr.mtlx.odm.SessionFactory2;
import fr.mtlx.odm.converters.ConvertionException;
import fr.mtlx.odm.model.Person;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class TestFilterBuilder {

    private SessionFactory sessionFactory;

    
    @Before
    public void init() throws MappingException
    {
        sessionFactory = new SessionFactory2(new Class<?>[] { Person.class, });
    }
    
    private <T> CompositeFilter buildFilterForClass(Class<T> persistentClass) throws MappingException {
        
        ClassMetadata<T> metadata = sessionFactory.getClassMetadata(persistentClass);

        if (metadata == null) {
            throw new MappingException(String.format("%s is not a persistent class", persistentClass));
        }

        FilterBuilder<T> fb = sessionFactory.filterBuilder(persistentClass);

        CompositeFilter filter = fb.and();

        for (String oc : metadata.getObjectClassHierarchy()) {
            filter.add(fb.objectClass(oc));
        }

        for (String oc : metadata.getAuxiliaryClasses()) {
            filter.add(fb.objectClass(oc));
        }

        return filter;
    }

    @Test
    public void persistentFilterNotNull() throws MappingException {
        assertNotNull(sessionFactory.filterBuilder(Person.class));
    }

    @Test(expected = MappingException.class)
    public void transientFilter() throws MappingException {
        assertNull(sessionFactory.filterBuilder(Integer.class));
    }

    @Test
    public void test() throws MappingException {
        FilterBuilder<Person> fb = sessionFactory.filterBuilder(Person.class);

        CompositeFilter filter = fb.and();

        filter.add(fb.or(fb.property("commonName").equalsTo("alex"), fb.property("surname").equalsTo("mathieu")));

        String encodedFilter = filter.encode();

        assertNotNull(encodedFilter);

        assertThat(encodedFilter, startsWith("("));

        assertThat(encodedFilter, endsWith(")"));

        String expected = "(&(objectClass=top)(objectClass=person)(|(cn=alex)(sn=mathieu)))";

        assertThat(encodedFilter, is(expected));
    }

    @Test
    public void testCombineAndFilters() throws MappingException {
        CompositeFilter filter = buildFilterForClass(Person.class);

        FilterBuilder<Person> fb = sessionFactory.filterBuilder(Person.class);

        filter.add(fb.and(fb.property("commonName").equalsTo("alex"), fb.property("surname").equalsTo("mathieu")));

        String encodedFilter = filter.encode();

        assertNotNull(encodedFilter);

        assertThat(CharMatcher.is('&').countIn(encodedFilter), is(1));
    }

    @Test(expected = ConvertionException.class)
    public void testPropertyFilterWrongType() throws InvalidNameException, MappingException {
        Person p = new Person();

        p.setDn(new LdapName("uid=test,dc=foo,dc=bar"));

        CompositeFilter filter = buildFilterForClass(Person.class);

        FilterBuilder<Person> fb = sessionFactory.filterBuilder(Person.class);

        filter.add(fb.property("commonName").equalsTo(1)).add(fb.property("surname").equalsTo("mathieu"));

        String encodedFilter = filter.encode();

        assertNotNull(encodedFilter);

        assertThat(CharMatcher.is('&').countIn(encodedFilter), is(1));
    }
}
