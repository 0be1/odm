/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.mtlx.odm;

import javax.naming.NamingException;

/**
 *
 * @author alex
 */
public interface ContextResolver {

    Object getProperty(final String name) throws NamingException, InstantiationException, IllegalAccessException;
    
    void setProperty(final String name) throws NamingException, InstantiationException, IllegalAccessException;
}
