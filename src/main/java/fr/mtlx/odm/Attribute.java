package fr.mtlx.odm;

/*
 * #%L
 * fr.mtlx.odm
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * This annotation describes the mapping of a Java field to an LDAP attribute.
 * <p>
 * The containing class must be annotated with {@link Entry}.
 * 
 * @author Paul Harvey &lt;paul.at.pauls-place.me.uk>
 * @see Entry
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Attribute {
    /**
     * The Type attribute indicates whether a field is regarded as binary based 
     * or string based by the LDAP JNDI provider.
     */
    enum Type {
        /**
         * A string field - returned by the JNDI LDAP provider as a {@link java.lang.String}.
         */
        STRING, /**
         * A binary field - returned by the JNDI LDAP provider as a <code>byte[]</code>.
         */
        BINARY
    }

    /**
     * The LDAP attribute name that this field represents.
     * <p>
     * Defaults to "" in which case the Java field name is used as the LDAP attribute name.
     * 
     * @return The LDAP attribute name.
     * 
     */
    String name() default "";

    /**
     * Indicates whether this field is returned by the LDAP JNDI provider as a
     * <code>String</code> (<code>Type.STRING</code>) or as a
     * <code>byte[]</code> (<code>Type.BINARY</code>).
     * 
     * @return Either <code>Type.STRING</code> to indicate a string attribute 
     * or <code>Type.BINARY</code> to indicate a binary attribute.
     */
    Type type() default Type.STRING;

    /**
     * The LDAP syntax of the attribute that this field represents.
     * <p>
     * This optional value is typically used to affect the precision of conversion 
     * of values between LDAP and Java, 
     * see {@link org.springframework.ldap.odm.typeconversion.ConverterManager} 
     * and {@link org.springframework.ldap.odm.typeconversion.impl.ConverterManagerImpl}.
     * 
     * @return The LDAP syntax of this attribute.
     */
    String syntax() default "";
    
    String[] aliases() default {};
}
