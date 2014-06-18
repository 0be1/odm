package fr.mtlx.odm.utils;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;

public class TypeSafeConverter<T> implements Converter<Object, T> {

    private final Class<?> clazz;

    public TypeSafeConverter(final Class<?> clazz) {
	this.clazz = checkNotNull(clazz);
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable T convert(@Nullable Object object) {
	if (object == null)
	    return null;
	
	if (!clazz.isInstance(object))
	    throw new ClassCastException(String.format("Cannot cast %s to %s", object.getClass(), clazz));

	return (T) object;
    }
}
