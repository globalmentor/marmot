/*
 * Copyright © 1996-2009 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

import java.io.*;
import java.net.URI;

import static java.util.Objects.*;

import static com.globalmentor.io.Files.*;
import com.globalmentor.java.OperatingSystem;
import com.globalmentor.marmot.repository.Repository;

/**
 * The default implementation of a manager of cached Marmot resources.
 * @author Garret Wilson
 */
public class DefaultMarmotResourceCache extends
		AbstractMarmotResourceCache<AbstractMarmotResourceCache.MarmotResourceCacheKey, DefaultMarmotResourceCache.DefaultMarmotResourceCacheQuery> {

	/** The directory in which cached information will be stored. */
	private final File cacheDirectory;

	/** @return The directory in which cached information will be stored. */
	public File getCacheDirectory() {
		return cacheDirectory;
	}

	/**
	 * Default constructor that uses the operating system temporary directory for the cache directory. Fetching is performed synchronously and objects have the
	 * maximum expiration.
	 * @see OperatingSystem#getTempDirectory()
	 */
	public DefaultMarmotResourceCache() {
		this(true, Long.MAX_VALUE);
	}

	/**
	 * Constructor that uses the operating system temporary directory for the cache directory.
	 * @param fetchSynchronous Whether fetches for new values should occur synchronously.
	 * @param expiration The length of time, in milliseconds, to keep cached information.
	 * @see OperatingSystem#getTempDirectory()
	 */
	public DefaultMarmotResourceCache(final boolean fetchSynchronous, final long expiration) {
		this(OperatingSystem.getTempDirectory(), fetchSynchronous, expiration);
	}

	/**
	 * Cache directory constructor.
	 * @param cacheDirectory The directory in which cached information will be stored.
	 * @param fetchSynchronous Whether fetches for new values should occur synchronously.
	 * @param expiration The length of time, in milliseconds, to keep cached information.
	 * @throws NullPointerException if the given cache directory is <code>null</code>.
	 */
	public DefaultMarmotResourceCache(final File cacheDirectory, final boolean fetchSynchronous, final long expiration) {
		super(fetchSynchronous, expiration);
		this.cacheDirectory = requireNonNull(cacheDirectory, "Cache directory cannot be null.");
	}

	/**
	 * Creates a query from the given repository and resource URI.
	 * @param repository The repository in which the resource is stored.
	 * @param resourceURI The URI of the resource.
	 * @return A query for for requesting the resource from the cache.
	 * @throws NullPointerException if the given repository and/or resource URI is <code>null</code>.
	 */
	protected DefaultMarmotResourceCacheQuery createQuery(final Repository repository, final URI resourceURI) {
		return new DefaultMarmotResourceCacheQuery(repository, resourceURI);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation returns {@link #getCacheDirectory()}.
	 * </p>
	 */
	@Override
	protected File getCacheDirectory(final DefaultMarmotResourceCacheQuery query) throws IOException {
		return ensureDirectoryExists(getCacheDirectory()); //return the cache directory, making sure it exists
	}

	/**
	 * A query for cached Marmot resources.
	 * @author Garret Wilson
	 */
	public static class DefaultMarmotResourceCacheQuery extends
			AbstractMarmotResourceCache.AbstractMarmotResourceCacheQuery<AbstractMarmotResourceCache.MarmotResourceCacheKey> {

		/**
		 * Repository and resource URI constructor.
		 * @param repository The repository in which the resource is stored.
		 * @param resourceURI The URI of the resource.
		 * @throws NullPointerException if the given repository and/or resource URI is <code>null</code>.
		 */
		public DefaultMarmotResourceCacheQuery(final Repository repository, final URI resourceURI) {
			super(repository, resourceURI, new MarmotResourceCacheKey(repository, resourceURI));
		}
	}

}
