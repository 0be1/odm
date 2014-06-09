package fr.mtlx.odm.filters;

import static com.google.common.base.Preconditions.checkNotNull;
import fr.mtlx.odm.SessionFactoryImpl;

public abstract class FilterImpl implements Filter {
	private final SessionFactoryImpl sessionFactory;

        public FilterImpl(SessionFactoryImpl sessionFactory) {
            this.sessionFactory = checkNotNull(sessionFactory);
        }
        
	protected SessionFactoryImpl getSessionFactory() {
		return checkNotNull(sessionFactory);
	}
}
