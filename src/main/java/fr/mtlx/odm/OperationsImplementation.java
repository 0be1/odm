package fr.mtlx.odm;

import java.util.List;

import javax.annotation.Nonnull;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.SearchControls;

public interface OperationsImplementation<T> {
    
    @Nonnull Name doBind(@Nonnull T transientObject) throws NameNotFoundException;
    
    @Nonnull T doLookup(@Nonnull final Name dn) throws NameNotFoundException;
    
    void doUnbind(@Nonnull Name dn) throws NameNotFoundException;

    long count(Name base, SearchControls controls, String filter) throws SizeLimitExceededException;

    Iterable<List<T>> pages(int pageSize, String filter, Name base, SearchControls controls) throws SizeLimitExceededException;

    Iterable<T> doSearch(Name base, SearchControls controls, String encodeFilter) throws SizeLimitExceededException;
}
