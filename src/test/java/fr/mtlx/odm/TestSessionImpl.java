package fr.mtlx.odm;

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
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import fr.mtlx.odm.filters.FilterBuilder;
import fr.mtlx.odm.model.GroupOfNames;
import fr.mtlx.odm.model.GroupOfPersons;
import fr.mtlx.odm.model.OrganizationalPerson;
import fr.mtlx.odm.model.Person;
import fr.mtlx.odm.spring.SpringSessionFactoryImpl;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.SizeLimitExceededException;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import static org.hamcrest.CoreMatchers.is;
import org.junit.After;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.ldap.core.support.SingleContextSource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
// @ContextConfiguration( locations =
// { "classpath:testContext.xml", "classpath:ldapContext.xml" } )
@Ignore
public class TestSessionImpl {

    private SessionFactory sessionFactory;

    private Session session;

    private LdapContext dirContext;

    @Before
    public void openSession() {
        dirContext = mock(LdapContext.class);

        SingleContextSource contextSource = new SingleContextSource(dirContext);

        sessionFactory = new SpringSessionFactoryImpl(contextSource);

        session = sessionFactory.openSession();
    }

    @After
    public void closeSession() throws IOException {
        if (session != null) {
            session.close();
        }
    }

    @Test
    public void testLookupPerson() throws NamingException {

        Name dn = new LdapName("cn=dummy_person,ou=personnes");

        when(dirContext.lookup(dn)).thenReturn(dirContext);

        Person p = session.getOperations(Person.class).lookup(dn);

        assertNotNull(p);

        assertTrue(p.getDn().equals(dn));

        assertThat(p.getSn(), is("dummy"));
    }

    @Test
    public void testLookupOrganizationalPerson() throws InvalidNameException, javax.naming.NameNotFoundException {
        Name dn = new LdapName("cn=dummy_op,ou=personnes");

        OrganizationalPerson entry = session.getOperations(OrganizationalPerson.class).lookup(dn);

        assertNotNull(entry);

        assertTrue(entry.getDn().equals(dn));

        assertThat(entry.getSn(), is("op"));

        assertThat(entry.getTelephoneNumber().size(), is(2));

        assertTrue(entry.getTelephoneNumber().containsAll(Lists.newArrayList("0491141300", "0491141312")));

        assertTrue(entry.getUserPassword().length > 0);
    }

    @Test(expected = NameNotFoundException.class)
    public void testLookupUnknowDn() throws InvalidNameException, NameNotFoundException {
        Name dn = new LdapName("cn=foo,ou=personnes");

        session.getOperations(Person.class).lookup(dn);
    }

    @Test
    @Ignore
    public void testLookupGroupOfNames() throws InvalidNameException, NameNotFoundException {
        Name dn = new LdapName("cn=prod,ou=groupes");

        GroupOfNames entry = session.getOperations(GroupOfNames.class).lookup(dn);

        assertNotNull(entry);

        assertTrue(entry.getDn().equals(dn));

        assertThat(entry.getCommonName(), is("prod"));

        assertThat(entry.getMembers().size(), is(2));
    }

    @Test
    public void testIsPersistent() throws InvalidNameException, NameNotFoundException {
        Name dn = new LdapName("cn=alex,ou=personnes");

        Object entry = session.getOperations(Person.class).lookup(dn);

        assertTrue(session.isPersistent(entry));
    }

    @Test
    public void testLookupGroupOfPersons() throws InvalidNameException, NameNotFoundException {
        Name dn = new LdapName("cn=alex,ou=personnes");

        session.getOperations(Person.class).lookup(dn);

        dn = new LdapName("cn=prod,ou=groupes");

        GroupOfPersons entry = session.getOperations(GroupOfPersons.class).lookup(dn);

        assertNotNull(entry);

        assertTrue(entry.getDn().equals(dn));

        assertThat(entry.getCommonName(), is("prod"));

        assertFalse(entry.getMembers().isEmpty());

        assertTrue(Iterables.any(entry.getMembers(), new Predicate<Person>() {
            @Override
            public boolean apply(Person entry) {
                return entry != null;
            }

        }));
    }

    private void validate(Person entry) {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();

        Validator validator = factory.getValidator();

        Set<ConstraintViolation<Person>> constraintViolations = validator.validate(entry);

        if (constraintViolations.size() > 0) {
            StringBuilder builder = new StringBuilder("Impossible de valider les donnees du bean : ");

            for (ConstraintViolation<Person> contraintes : constraintViolations) {
                builder.append(contraintes.getRootBeanClass().getSimpleName() + "." + contraintes.getPropertyPath()
                        + " " + contraintes.getMessage());
            }

            throw new IllegalStateException(builder.toString());
        } else {
            System.out.println("Les donnees du bean sont valides");
        }
    }

    @Test
    public void testSearch() throws InvalidNameException, SizeLimitExceededException, MappingException {
        Name dn = new LdapName("ou=personnes");

        FilterBuilder<Person> fb = sessionFactory.filterBuilder(Person.class);
        List<Person> entries = session.getOperations(Person.class).setBase(dn).add(fb.not(fb.objectClass("ENTPerson"))).list();

        assertNotNull(entries);

        assertTrue(entries.size() > 0);
        assertTrue(Iterables.any(entries, new Predicate<Person>() {
            @Override
            public boolean apply(Person entry) {
                return entry != null && session.isPersistent(entry);
            }

        }));
    }

    @Test
    public void testPagedSearch() throws InvalidNameException, MappingException, SizeLimitExceededException {
        Name dn = new LdapName("ou=personnes");
        int n = 0;

        FilterBuilder<Person> fb = sessionFactory.filterBuilder(Person.class);

        Iterable<List<Person>> results = session.getOperations(Person.class).setBase(dn)
                .add(fb.not(fb.objectClass("ENTPerson")))
                .pages(5);

        assertNotNull(results);

        Iterator<List<Person>> iterator = results.iterator();

        assertNotNull(iterator);

        assertTrue(iterator.hasNext());

        for (List<Person> page : results) {
            n += page.size();

            assertTrue(Iterables.any(page, new Predicate<Person>() {
                @Override
                public boolean apply(Person entry) {
                    return entry != null && session.isPersistent(entry);
                }

            }));
        }

        assertTrue(n > 0);
    }

    @Test
    public void testBind() throws InvalidNameException, NameNotFoundException {
        String dn = "cn=fire,ou=personnes";

        Person entry = new Person();

        entry.setDn(new LdapName(dn));

        entry.setCn("fire");

        entry.setSn("fox");

        validate(entry);

        session.getOperations(Person.class).bind(entry);
    }

    @Test
    public void testModify() throws InvalidNameException, NameNotFoundException {
        Name dn = new LdapName("cn=fire,ou=personnes");

        Person entry = session.getOperations(Person.class).lookup(dn);

        entry.setCn("fire");

        entry.setSn("bird");

        validate(entry);

        session.getOperations(Person.class).modify(entry);
    }

    @Test
    public void testUnbind() throws InvalidNameException, NameNotFoundException {
        Name dn = new LdapName("cn=fire,ou=personnes");

        Person entry = session.getOperations(Person.class).lookup(dn);

        session.getOperations(Person.class).unbind(entry);
    }
}
