package fr.mtlx.odm.filters;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableList;

public abstract class CompositeFilter implements Filter {

    protected final ImmutableList<Filter> filters;

    public CompositeFilter(Iterable<Filter> filters) {
        this.filters = ImmutableList.copyOf(checkNotNull(filters));
    }

    @Override
    public void encode(final StringBuilder sb) {
        for(final Filter filter : filters) {
            sb.append('(');
            filter.encode(sb);
            sb.append(')');
        }
    }
}
