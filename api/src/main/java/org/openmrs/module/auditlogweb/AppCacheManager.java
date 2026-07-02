/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class AppCacheManager {
	
	private final Cache<String, Boolean> cache;
	
	public AppCacheManager() {
		this.cache=CacheBuilder.newBuilder().expireAfterWrite(2,TimeUnit.MINUTES).maximumSize(10_000).build();
	}
	
	public void set(String key, Boolean value) {
		cache.put(key, value);
	}
	
	public Boolean get(String key) {
		return cache.getIfPresent(key);
	}
}
