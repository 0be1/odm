package fr.mtlx.odm.filters;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableList;
import fr.mtlx.odm.ClassMetadata;
import fr.mtlx.odm.MappingException;
import fr.mtlx.odm.SessionFactoryImpl;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FilterBuilderImpl<T> implements FilterBuilder<T> {

    private final Class<T> persistentClass;

    private final SessionFactoryImpl sessionFactory;

    private final ImmutableList.Builder<Filter> filters = new ImmutableList.Builder<>();

    public FilterBuilderImpl(final Class<T> persistentClass, final SessionFactoryImpl sessionFactory) throws MappingException {

        this.sessionFactory = checkNotNull(sessionFactory);

        if (!sessionFactory.isPersistentClass(persistentClass)) {
            throw new MappingException(persistentClass + " is not a persistent class.");
        }

        this.persistentClass = checkNotNull(persistentClass);

        final ClassMetadata<T> metadata = sessionFactory.getClassMetadata(persistentClass);

        filters.addAll(metadata.getObjectClassHierarchy().stream().map(SimpleFilterBuilder::objectClass).collect(Collectors.toList()))
                .addAll(metadata.getAuxiliaryClasses().stream().map(SimpleFilterBuilder::objectClass).collect(Collectors.toList()));
    }

    @Override
    public CompareCriterion<T> property(String propertyName) throws MappingException {
        return new PropertyFilterBuilder<>(sessionFactory, persistentClass, propertyName);
    }

    @Override
    public FilterBuilder<T> or(Stream<Filter> filters) {
        this.filters.add(new OrFilter(filters));

        return this;
    }

    @Override
    public FilterBuilder<T> and(Stream<Filter> filters) {
        this.filters.add(new AndFilter(filters));

        return this;
    }

    @Override
    public Filter objectClass(String objectClass) {
        return SimpleFilterBuilder.objectClass(objectClass);
    }

    @Override
    public Filter not(Filter filter) {
        return new NotFilter(filter);
    }

    @Override
    public CompareCriterion<T> attribute(String attributeName) {
        return new SimpleFilterBuilder<>(attributeName);
    }

    @Override
    public Filter build() {
        return new AndFilter(this.filters.build().stream());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        AndFilter filter = new AndFilter(this.filters.build().stream());

        filter.encode(sb);

        return sb.toString();
    }
}
