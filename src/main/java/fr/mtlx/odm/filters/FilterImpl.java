package fr.mtlx.odm.filters;

import static com.google.common.base.Preconditions.checkNotNull;
import fr.mtlx.odm.SessionFactoryImpl;

public abstract class FilterImpl implements Filter {
	private SessionFactoryImpl sessionFactory;

	protected SessionFactoryImpl getSessionFactory() {
		return checkNotNull(sessionFactory);
	}

	protected void setSessionFactory(SessionFactoryImpl sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
}
