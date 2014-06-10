package fr.mtlx.odm.filters;

import static com.google.common.base.Preconditions.checkNotNull;
import fr.mtlx.odm.MappingException;
import fr.mtlx.odm.SessionFactoryImpl;

public class FilterBuilderImpl<T> implements FilterBuilder<T> {

    private final Class<T> persistentClass;

    private final SessionFactoryImpl sessionFactory;

    public FilterBuilderImpl(final Class<T> persistentClass, final SessionFactoryImpl sessionFactory) throws MappingException {

	this.sessionFactory = checkNotNull(sessionFactory);

	if (!sessionFactory.isPersistentClass(persistentClass)) {
	    throw new MappingException(persistentClass + " is not a persistent class.");
	}

	this.persistentClass = checkNotNull(persistentClass);
    }

    @Override
    public OrFilter or(Filter... filters) {
	return new OrFilter(filters);
    }

    @Override
    public AndFilter and(Filter... filters) {
	return new AndFilter(filters);
    }

    @Override
    public CompareCriterion<T> property(String propertyName) throws MappingException {
	return new PropertyFilterBuilder<>(sessionFactory, persistentClass, propertyName);
    }

    @Override
    public Filter objectClass(String objectClass) {
	return AttributeFilterBuilder.objectClass(objectClass);
    }

    @Override
    public Filter not(Filter filter) {
	return new NotFilter(filter);
    }

    @Override
    public CompareCriterion<T> attribute(String attributeName) {
	return new AttributeFilterBuilder<>(attributeName);
    }
}
