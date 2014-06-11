package fr.mtlx.odm.filters;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import fr.mtlx.odm.ClassMetadata;
import fr.mtlx.odm.MappingException;
import fr.mtlx.odm.SessionFactoryImpl;

import java.util.Arrays;
import java.util.Collection;

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

	filters.addAll(FluentIterable.from(metadata.getObjectClassHierarchy()).transform(SimpleFilterBuilder::objectClass))
	.addAll(FluentIterable.from(metadata.getAuxiliaryClasses()).transform(SimpleFilterBuilder::objectClass));
    }

    @Override
    public CompareCriterion<T> property(String propertyName) throws MappingException {
	return new PropertyFilterBuilder<>(sessionFactory, persistentClass, propertyName);
    }

    @Override
    public FilterBuilder<T> or(Collection<Filter> filters) {
	this.filters.add(new OrFilter(filters));

	return this;
    }

    @Override
    public FilterBuilder<T> or(Filter... filters) {
	return or(Arrays.asList(filters));
    }

    @Override
    public FilterBuilder<T> and(Collection<Filter> filters) {
	this.filters.add(new AndFilter(filters));

	return this;
    }

    @Override
    public FilterBuilder<T> and(Filter... filters) {
	return and(Arrays.asList(filters));
    }

    @Override
    public Filter build() {
	return new AndFilter(this.filters.build());
    }

    @Override
    public String toString() {
	StringBuilder sb = new StringBuilder();

	AndFilter filter = new AndFilter(this.filters.build());

	filter.encode(sb);

	return sb.toString();
    }

    @Override
    public Filter not(Filter filter) {
	return new NotFilter(filter);
    }

    @Override
    public Filter objectClass(String objectClass) {
	return SimpleFilterBuilder.objectClass(objectClass);
    }

    @Override
    public CompareCriterion<T> attribute(String attributeName) {
	return new SimpleFilterBuilder<>(attributeName);
    }

}
