package fr.mtlx.odm.converters;

public class IdentityConverter<T> extends AttributeConverter<T, T> {
	public IdentityConverter(Class<T> type) {
		super(type, type);
	}

	@Override
	public T to(T object) throws ConvertionException {
		return object;
	}

	@Override
	public T from(T value) throws ConvertionException {
		return value;
	}
}
