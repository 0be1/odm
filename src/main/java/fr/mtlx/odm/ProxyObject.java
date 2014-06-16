package fr.mtlx.odm;

import org.springframework.ldap.core.DirContextOperations;

public interface ProxyObject {

    
    DirContextOperations getProxyContext();
    
}
