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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.mtlx.odm.AttributeMetadata;
import fr.mtlx.odm.ClassMetadata;
import fr.mtlx.odm.ContextResolver;
import fr.mtlx.odm.ProxyFactory;
import fr.mtlx.odm.Session;
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

@SuppressWarnings("rawtypes")
public class SpringProxyFactory<T> implements ProxyFactory<T, DirContextOperations> {

    private static final MethodFilter FINALIZE_FILTER = (Method m) -> !(m.getParameterTypes().length == 0 && m.getName()
	    .equals("finalize"));

    private final Set<Class<?>> interfaces;

    private final Class<?> proxyClass;

    private final Class<T> superClass;

    public SpringProxyFactory(final Class<T> superClass, final Class<?>[] interfaces) {
	this.superClass = checkNotNull(superClass);

	this.interfaces = Sets.newHashSet(interfaces);

	javassist.util.proxy.ProxyFactory factory = new javassist.util.proxy.ProxyFactory();

	factory.setFilter(FINALIZE_FILTER);

	factory.setSuperclass(superClass);

	if (interfaces.length > 0) {
	    factory.setInterfaces(interfaces);
	}

	proxyClass = factory.createClass();
    }

    @Override
    public Class[] getInterfaces() {
	return interfaces.toArray(new Class[] {});
    }

    @Override
    public T getProxy(final Session session, final DirContextOperations context) throws InstantiationException,
	    IllegalAccessException, InvalidNameException {

	class DirContextHandler<T> implements MethodHandler {

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

	    private String capitalizeFirstLetter(String original) {
		if (original.length() == 0) {
		    return original;
		}

		return original.substring(0, 1).toUpperCase() + original.substring(1);
	    }

	    private String getPropertyName(final Method method) {
		final String name = method.getName();

		if (name.startsWith("get") || name.startsWith("set")) {
		    return name.substring(3);
		} else if (name.startsWith("is")) {
		    return name.substring(2);
		}

		return null;
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
		} else if (!isHandled(method)) {
		    return method1.invoke(object, args);
		}
		final String property = getPropertyName(method);

		assert property != null;

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
		    final String property = getPropertyName(method);

		    if (property != null) {
			if (property.equals(metadata.getIdentifierPropertyName())) {
			    return false;
			}

			return metadata.getAttributeMetadata(property) != null;
		    }
		}

		return false;
	    }
	}

	ProxyObject proxy = (ProxyObject) proxyClass.newInstance();

	proxy.setHandler(new DirContextHandler<>(proxy, superClass));

	return (T) proxy;
    }

    @Override
    public Class getProxyClass() {
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
