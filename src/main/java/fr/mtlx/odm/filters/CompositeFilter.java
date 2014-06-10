package fr.mtlx.odm.filters;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.google.common.collect.Lists;

public abstract class CompositeFilter implements Filter {

    protected final List<Filter> filters;

    public CompositeFilter(final Filter... filters) {
	this.filters = Lists.newArrayList(filters);
    }

    public CompositeFilter add(Filter filter) {
	filters.add(checkNotNull(filter));

	return this;
    }
}
