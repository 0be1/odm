package fr.mtlx.odm.filters;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class CompositeFilter implements Filter {

    protected final List<Filter> filters;

    CompositeFilter(Stream<Filter> filters) {
        this.filters = filters.collect(Collectors.toList());
    }
    
    CompositeFilter(Collection<Filter> filters) {
	this(filters.stream());
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
