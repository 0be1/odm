package fr.mtlx.odm.cache;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

import javax.naming.Name;

import fr.mtlx.odm.utils.TypeCheckConverter;

public class TypeSafeCache<T> implements Cache<Name, T> {

    private final PersistentCache innerCache;
    
    private final TypeCheckConverter<T> typeChecker;

    public TypeSafeCache(final Class<T> clazz, final PersistentCache cache) {
	this.innerCache = checkNotNull(cache);
	
	this.typeChecker = new TypeCheckConverter<>(checkNotNull(clazz));
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
	return obj.map(o -> typeChecker.convert(o));
    }
}
