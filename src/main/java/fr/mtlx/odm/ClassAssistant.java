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
import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.InvocationTargetException;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.apache.commons.beanutils.PropertyUtils;

public class ClassAssistant<T> {

    private final ClassMetadata<T> metadata;

    public ClassAssistant(final ClassMetadata<T> metadata) {
        this.metadata = checkNotNull(metadata);
    }

    public LdapName getIdentifier(Object object) {
        String identifier = metadata.getIdentifierPropertyName();

        if (object instanceof ProxyObject) {
            return (LdapName) ((ProxyObject) object).getProxyContext().getDn();
        } else {
            try {
                return (LdapName) metadata.getIdentifier().get(object);
            } catch (IllegalAccessException e) {
                try {
                    return new LdapName((String) PropertyUtils.getSimpleProperty(object, identifier));
                } catch (InvalidNameException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e1) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public Object getValue(Object object, String propertyName) {
        try {
            return PropertyUtils.getSimpleProperty(object, propertyName);
        } catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
    }
}