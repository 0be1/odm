package fr.mtlx.odm.filters;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.Lists;
import fr.mtlx.odm.SessionFactoryImpl;
import java.util.List;

public abstract class CompositeFilter extends FilterImpl {

    protected final List<Filter> filters;

    public CompositeFilter(SessionFactoryImpl sessionFactory, final Filter... filters) {
        super(sessionFactory);

        this.filters = Lists.newArrayList(filters);
    }

    public CompositeFilter add(Filter filter) {
        filters.add(checkNotNull(filter));

        return this;
    }
}
