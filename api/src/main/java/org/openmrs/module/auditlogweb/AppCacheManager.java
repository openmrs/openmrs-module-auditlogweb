package org.openmrs.module.auditlogweb;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public  class AppCacheManager {

    private final Cache<String, Boolean> cache;

    public AppCacheManager() {
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
    }

    public void set(String key, Boolean value) {
        cache.put(key, value);
    }

    public Boolean get(String key) {
        return cache.getIfPresent(key);
    }
}
