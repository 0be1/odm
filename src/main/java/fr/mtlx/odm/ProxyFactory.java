/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.mtlx.odm;

import javax.naming.Context;
import javax.naming.InvalidNameException;

/**
 *
 * @author alex
 */
public interface ProxyFactory<T, CTX extends Context> {
     Class<?>[] getInterfaces();
     
     T getProxy(final Session session,
            final CTX context)
            throws InstantiationException, IllegalAccessException,
            InvalidNameException;
     
     Class<?> getProxyClass();
     
     Class<T> getSuperClass();
     
     boolean isImplementing(final Class<?> clazz);
}
