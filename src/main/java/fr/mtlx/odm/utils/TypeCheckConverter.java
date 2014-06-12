package fr.mtlx.odm.utils;

import static com.google.common.base.Preconditions.checkNotNull;

public class TypeCheckConverter<T> implements Converter<Object, T> {

    private final Class<?> clazz;

    public TypeCheckConverter(final Class<?> clazz) {
	this.clazz = checkNotNull(clazz);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T convert(Object object) {
	if (!clazz.isInstance(object))
	    throw new ClassCastException(String.format("Cannot cast %s to %s", object.getClass(), clazz));

	return (T) object;
    }
}
