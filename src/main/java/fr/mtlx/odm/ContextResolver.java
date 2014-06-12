/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.mtlx.odm;

import javax.annotation.Nullable;
import javax.naming.NamingException;

public interface ContextResolver {

    @Nullable Object getProperty(final String name) throws NamingException, InstantiationException, IllegalAccessException;
    
    void setProperty(final String name, @Nullable final Object value) throws NamingException, InstantiationException, IllegalAccessException;
}
