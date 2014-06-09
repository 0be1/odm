package fr.mtlx.odm.spring;

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
import fr.mtlx.odm.SessionFactoryImpl;
import fr.mtlx.odm.cache.EntityCache;
import fr.mtlx.odm.converters.DefaultConverters;
import java.util.List;
import javax.naming.directory.DirContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapTemplate;

@SuppressWarnings("serial")
public class SpringSessionFactoryImpl extends SessionFactoryImpl implements
        InitializingBean {

    private final ContextSource contextSource;

    private List<String> mappedClasses;

    private final LdapTemplate ldapTemplate;

    public SpringSessionFactoryImpl(final ContextSource contextSource) {
        this.contextSource = checkNotNull(contextSource);

        this.ldapTemplate = new LdapTemplate(contextSource);
    }

    public ContextSource getContextSource() {
        return contextSource;
    }

    public DirContext getDirContext() {
        return contextSource.getReadWriteContext();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        DefaultConverters.defaultSyntaxConverters
                .entrySet().stream().forEach((entry) -> {
                    addConverter(entry.getKey(), entry.getValue());
                });
        DefaultConverters.defaultAttributeConverters
                .entrySet().stream().forEach((entry) -> {
                    addConverter(entry.getKey(), entry.getValue());
                });

        for (String className : mappedClasses) {
            addClass(className);
        }

        initialize();
    }

    public LdapTemplate getLdapTemplate() {
        return this.ldapTemplate;
    }

    @Override
    public boolean isOperationalAttribute(String attributeId) {
        return super.isOperationalAttribute(attributeId)
                || operationalAttributes.contains(attributeId);
    }

    public void setMappedClasses(List<String> mappedClasses) {
        this.mappedClasses = mappedClasses;
    }

    @Override
    public SpringSessionImpl openSession() {
        return new SpringSessionImpl(this);
    }

    @Override
    public <T> EntityCache<T> getCacheFor(Class<T> persistentClass) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
