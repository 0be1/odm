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
import static java.lang.System.identityHashCode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyObject;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;

import org.springframework.ldap.core.DirContextOperations;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.mtlx.odm.AttributeMetadata;
import fr.mtlx.odm.ClassMetadata;
import fr.mtlx.odm.ContextResolver;
import fr.mtlx.odm.ProxyFactory;
import fr.mtlx.odm.Session;
import fr.mtlx.odm.utils.TypeSafeConverter;

public class SpringProxyFactory<T> implements ProxyFactory<T, DirContextOperations> {

    private static final MethodFilter FINALIZE_FILTER = (Method m) -> !(m.getParameterTypes().length == 0 && m.getName()
            .equals("finalize"));

    private final Set<Class<?>> interfaces;

    private final Class<?> proxyClass;

    private final Class<T> superClass;

    private final TypeSafeConverter<T> typeSafe;

    public SpringProxyFactory(final Class<T> superClass, final Class<?>[] interfaces) {
        this.superClass = checkNotNull(superClass);

        this.interfaces = Sets.newHashSet(interfaces);

        javassist.util.proxy.ProxyFactory factory = new javassist.util.proxy.ProxyFactory();

        factory.setFilter(FINALIZE_FILTER);

        factory.setSuperclass(superClass);

        factory.setInterfaces(new Class[] { fr.mtlx.odm.ProxyObject.class });
        
        if (interfaces.length > 0) {
            factory.setInterfaces(interfaces);
        }

        proxyClass = factory.createClass();

        typeSafe = new TypeSafeConverter<T>(superClass);
    }

    @Override
    public Class<?>[] getInterfaces() {
        return interfaces.toArray(new Class[] {});
    }
    
    public static String capitalizeFirstLetter(final String original)
    {
       if(checkNotNull(original) == "")
	   return original;
       
       // XXX code source encoding ???
       return Character.toUpperCase(original.charAt(0)) + original.substring(1);
    }
    
    private static String uncapitalizeFirstLetter(String original) { 
        if (checkNotNull(original) == "") {
            return original;
        }

        // XXX code source encoding ???
        return Character.toLowerCase(original.charAt(0)) + original.substring(1);
    }
    

    @Override
    public T getProxy(final Session session, final DirContextOperations context) throws InstantiationException,
            IllegalAccessException, InvalidNameException {

        class DirContextHandler implements MethodHandler {

            private final Map<String, Boolean> invoked = Maps.newHashMap();
            private final ClassMetadata<T> metadata;
            private final Class<T> proxiedClass;
            private final Object proxiedObject;
            private final ContextResolver resolver;

            DirContextHandler(ProxyObject proxiedObject, Class<T> proxiedClass) {
                this.proxiedObject = checkNotNull(proxiedObject, "proxiedObject is null");

                this.proxiedClass = checkNotNull(proxiedClass, "proxiedClass is null");

                this.metadata = session.getSessionFactory().getClassMetadata(proxiedClass);

                this.resolver = new DirContextOperationsResolver(context, metadata, session);
            }

            private Optional<String> getPropertyName(final Method method) {
                final String name = method.getName();

                return getPropertyName(name);
            }
            
            private Optional<String> strictAccessorPrefix(final String name) {
        	if (name.startsWith("get") || name.startsWith("set")) {
                    return Optional.of(name.substring(3));
                } else if (name.startsWith("is")) {
                    return Optional.of(name.substring(2));
                }
        	
        	return Optional.absent();
            }
            
            private Optional<String> getPropertyName(String name) {
                Optional<String> retval = strictAccessorPrefix(name);
                
        	if (retval.isPresent()) { //XXX flatMap
        	    return Optional.of(uncapitalizeFirstLetter(retval.get()));
        	} else {
        	    return Optional.absent();
        	}
            }

           
            private Method getSetter(final String property) throws SecurityException, NoSuchMethodException {
                final AttributeMetadata attr = metadata.getAttributeMetadata(property);

                if (attr.isMultivalued()) {
                    return proxiedClass.getMethod(getSetterName(property), attr.getCollectionType());
                } else {
                    return proxiedClass.getMethod(getSetterName(property), (Class<?>) attr.getObjectType());
                }
            }

            private String getSetterName(final String property) {
                return "set" + capitalizeFirstLetter(property);
            }

            private boolean hasGetterSignature(Method method) {
                return method.getParameterTypes().length == 0 && method.getReturnType() != null;
            }

            private boolean hasSetterSignature(Method method) {
                return method.getParameterTypes().length == 1
                        && (method.getReturnType() == null || method.getReturnType() == void.class);
            }

            @Override
            public Object invoke(Object object, final Method method, final Method method1, final Object[] args)
                    throws Exception {
                final String name = method.getName();

                if ("toString".equals(name)) {
                    return proxiedClass.getName() + "@" + identityHashCode(object);
                } else if ("equals".equals(name)) {
                    return proxiedObject == object;
                } else if ("hashCode".equals(name)) {
                    return identityHashCode(object);
                } else if ("getProxyContext".equals(name)) { // TODO choose a method name on runtime in case of collision...
                    return context;
                } else if (Objects.equal(getPropertyName(name).orNull(),metadata.getIdentifierPropertyName())) {
                    return context.getDn(); //TODO convert...
                } 
                else if (!isHandled(method)) {
                    return method1.invoke(object, args);
                }
                
                final String property = getPropertyName(method).get();

                if (!invoked.containsKey(property)) {
                    setProperty(object, property);

                    invoked.put(property, true);
                }

                return method1.invoke(proxiedObject);
            }

            private void setProperty(final Object object, final String property) throws NamingException,
                    InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {

                final Object value = metadata.getIdentifierPropertyName().equals(property) ? context.getDn() : resolver
                        .getProperty(property);

                getSetter(property).invoke(object, value);
            }

            private boolean isHandled(Method method) {
                if (hasGetterSignature(method)) {
                    final String property = getPropertyName(method).orNull();

                    return property != null && metadata.getAttributeMetadata(property) != null;
                }

                return false;
            }
        }

        ProxyObject proxy = (ProxyObject) proxyClass.newInstance();

        proxy.setHandler(new DirContextHandler(proxy, superClass));

        return typeSafe.convert(proxy);
    }

    @Override
    public Class<?> getProxyClass() {
        return proxyClass;
    }

    @Override
    public Class<T> getSuperClass() {
        return superClass;
    }

    @Override
    public boolean isImplementing(final Class<?> clazz) {
        for (Class<?> iface : interfaces)
            if (clazz.isAssignableFrom(iface))
                return true;
        return false;
    }

}
