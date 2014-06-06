package fr.mtlx.odm.filters;

import static com.google.common.base.Preconditions.checkNotNull;
import fr.mtlx.odm.SessionFactoryImpl;

public class FilterBuilderImpl<T> implements FilterBuilder<T>
{
	private final Class<T> persistentClass;

	private SessionFactoryImpl sessionFactory;

	public FilterBuilderImpl( final Class<T> persistentClass, final SessionFactoryImpl sessionFactory )
	{
		this.sessionFactory = checkNotNull( sessionFactory );

		this.persistentClass = checkNotNull( persistentClass );
	}

	@Override
	public OrFilter or( Filter... filters )
	{
		return new OrFilter( sessionFactory, filters );
	}

	@Override
	public AndFilter and( Filter... filters )
	{
		return new AndFilter( sessionFactory, filters );
	}

	@Override
	public CompareCriterion<T> property( String propertyName )
	{
		return new PropertyCriterion<T>( persistentClass, sessionFactory, propertyName );
	}

	@Override
	public Filter objectClass( String objectClass )
	{
		return new ObjectClassFilter( sessionFactory, objectClass );
	}

	@Override
	public Filter not( Filter filter )
	{
		return new NotFilter( sessionFactory, filter );
	}

	@Override
	public CompareCriterion<T> attribute( String attributeName )
	{
		return new AttributeCriterion<T>( sessionFactory, attributeName );
	}
}
