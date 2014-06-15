package fr.mtlx.odm;

/*
 * #%L
 * fr.mtlx.odm
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2012 - 2013 Alexandre Mathieu <me@mtlx.fr>
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import static com.google.common.base.Preconditions.checkNotNull;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import fr.mtlx.odm.cache.EhCacheCache;
import fr.mtlx.odm.cache.PersistentCache;

public class EhCacheFactory implements CacheFactory {

    private final CacheManager cacheManager;
    
    private final String cacheName;

    public EhCacheFactory(final String name, final CacheManager cacheManager) {
        this.cacheManager = checkNotNull(cacheManager, "cacheManager is null");
        
        this.cacheName = name;
    }

    @Override
    public PersistentCache getCache() {
	if (!cacheManager.cacheExists(cacheName) )
	    cacheManager.addCache(cacheName); 

	Ehcache cache = cacheManager.getEhcache(cacheName);
	
	assert cache != null;
	
	return new EhCacheCache(cache); 
    }
}
