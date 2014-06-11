package fr.mtlx.odm.filters;

import java.util.Arrays;
import java.util.Collection;


public class CompositeFilter implements Filter {

    protected final Collection<Filter> filters;

    
    CompositeFilter(Collection<Filter> filters) {
	this.filters  = filters; 
    }
    
    CompositeFilter(Filter... filters) {
        this(Arrays.asList(filters));
    }

    @Override
    public void encode(final StringBuilder sb) {
        for(final Filter filter : filters) {
            filter.encode(sb);
        }
    }
}
