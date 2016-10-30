/*
 * Copyright © 2009 GlobalMentor, Inc. <http://www.globalmentor.com/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.globalmentor.marmot;

import static java.util.Objects.*;

/**
 * Default configuration for Marmot.
 * @author Garret Wilson
 */
public class DefaultMarmotConfiguration implements MarmotConfiguration {

	/** The cache configured for use by Marmot. */
	private final MarmotResourceCache<?> resourceCache;

	/** @return The cache configured for use by Marmot. */
	public MarmotResourceCache<?> getResourceCache() {
		return resourceCache;
	}

	/** Default constructor configured to use a {@link DefaultMarmotResourceCache}. */
	public DefaultMarmotConfiguration() {
		this(new DefaultMarmotResourceCache());
	}

	/**
	 * Resource cache constructor.
	 * @param resourceCache The cache configured for use by Marmot.
	 * @throws NullPointerException if the given resource cache is <code>null</code>.
	 */
	public DefaultMarmotConfiguration(final MarmotResourceCache<?> resourceCache) {
		this.resourceCache = requireNonNull(resourceCache, "Resource cache cannot be null.");
	}
}
