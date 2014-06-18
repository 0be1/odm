package fr.mtlx.odm.cache;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.naming.Name;

import com.google.common.base.Optional;

import fr.mtlx.odm.utils.TypeSafeConverter;

public class TypeSafeCache<T> implements Cache<Name, T> {

    private final PersistentCache innerCache;
    
    private final TypeSafeConverter<T> typeChecker;

    public TypeSafeCache(final Class<T> clazz, final PersistentCache cache) {
	this.innerCache = checkNotNull(cache);
	
	this.typeChecker = new TypeSafeConverter<>(checkNotNull(clazz));
    }

    @Override
    public Optional<T> store(Name key, Object value) {
	return cast( innerCache.store(key, value) );
    }

    @Override
    public Optional<T> retrieve(Name key) {
	return cast( innerCache.retrieve(key) );
    }

    @Override
    public boolean remove(Name key) {
	return innerCache.remove(key);
    }

    @Override
    public void clear() {
	innerCache.clear();
    }

    @Override
    public boolean contains(Name key) {
	return innerCache.contains(key);
    }
    
    private Optional<T> cast(Optional<Object> obj) {
	return Optional.fromNullable(typeChecker.convert(obj.orNull()));
    }
}
