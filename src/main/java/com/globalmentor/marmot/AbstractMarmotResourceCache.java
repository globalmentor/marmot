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
import java.util.Date;

import static java.util.Objects.*;

import org.urframework.*;

import static com.globalmentor.io.Files.*;
import static com.globalmentor.net.URIs.*;
import static org.urframework.content.Content.*;

import com.globalmentor.cache.AbstractFileCache;
import com.globalmentor.iso.datetime.ISODateTime;
import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.model.AbstractHashObject;
import com.globalmentor.net.URIPath;
import com.globalmentor.net.URIs;
import com.globalmentor.util.*;

/**
 * An abstract implementation of a manager of cached Marmot resources.
 * @param <Q> The type of query used to request data from the cache.
 * @param <K> The type of key used to look up data in the cache.
 * @author Garret Wilson
 */
public abstract class AbstractMarmotResourceCache<K extends AbstractMarmotResourceCache.MarmotResourceCacheKey, Q extends AbstractMarmotResourceCache.AbstractMarmotResourceCacheQuery<K>>
		extends AbstractFileCache<K, Q> implements MarmotResourceCache<Q> {

	/**
	 * Constructor.
	 * @param fetchSynchronous Whether fetches for new values should occur synchronously.
	 * @param expiration The length of time, in milliseconds, to keep cached information.
	 */
	public AbstractMarmotResourceCache(final boolean fetchSynchronous, final long expiration) {
		super(fetchSynchronous, expiration);
	}

	/**
	 * Retrieves a value from the cache. Values are fetched from the backing store if needed, and this method blocks until the data is fetched.
	 * @param repository The repository in which the resource is stored.
	 * @param resourceURI The URI of the resource.
	 * @return The cached value.
	 * @throws NullPointerException if the given repository and/or resource URI is <code>null</code>.
	 * @throws IOException if there was an error fetching the value from the backing store.
	 * @see #get(Object)
	 */
	public final File get(final Repository repository, final URI resourceURI) throws IOException {
		return get(repository, resourceURI, false); //get without deferring fetching
	}

	/**
	 * Retrieves a value from the cache. Values are fetched from the backing store if needed, with fetching optionally deferred until later.
	 * @param repository The repository in which the resource is stored.
	 * @param resourceURI The URI of the resource.
	 * @param deferFetch Whether fetching, if needed, should be deferred and performed in an asynchronous thread.
	 * @return The cached value, or <code>null</code> if fetching was deferred.
	 * @throws NullPointerException if the given repository and/or resource URI is <code>null</code>.
	 * @throws IOException if there was an error fetching the value from the backing store.
	 * @see #get(Object, boolean)
	 */
	public final File get(final Repository repository, final URI resourceURI, final boolean deferFetch) throws IOException {
		return get(createQuery(repository, resourceURI), deferFetch); //create a query and perform the fetch
	}

	/**
	 * Retrieves data from the cache. Data is fetched from the backing store if needed, and this method blocks until the data is fetched.
	 * @param repository The repository in which the resource is stored.
	 * @param resourceURI The URI of the resource.
	 * @return The cached data.
	 * @throws NullPointerException if the given repository and/or resource URI is <code>null</code>.
	 * @throws IOException if there was an error fetching the data from the backing store.
	 * @see #getData(Object)
	 */
	public final Data<File> getData(final Repository repository, final URI resourceURI) throws IOException {
		return getData(repository, resourceURI, false); //get without deferring fetching
	}

	/**
	 * Retrieves data from the cache. Data is fetched from the backing store if needed, with fetching optionally deferred until later.
	 * @param repository The repository in which the resource is stored.
	 * @param resourceURI The URI of the resource.
	 * @param deferFetch Whether fetching, if needed, should be deffered and performed in an asynchronous thread.
	 * @return The cached data, or <code>null</code> if fetching was deferred.
	 * @throws NullPointerException if the given repository and/or resource URI is <code>null</code>.
	 * @throws IOException if there was an error fetching the data from the backing store.
	 * @see #getData(Object, boolean)
	 */
	public final Data<File> getData(final Repository repository, final URI resourceURI, final boolean deferFetch) throws IOException {
		return getData(createQuery(repository, resourceURI), deferFetch); //create a query and perform the fetch
	}

	/**
	 * Creates a query from the given repository and resource URI.
	 * @param repository The repository in which the resource is stored.
	 * @param resourceURI The URI of the resource.
	 * @return A query for for requesting the resource from the cache.
	 * @throws NullPointerException if the given repository and/or resource URI is <code>null</code>.
	 */
	protected abstract Q createQuery(final Repository repository, final URI resourceURI);

	/**
	 * Determines if a given cached value is stale. This version checks to see if the last modified time of the resource has changed.
	 * @param query The query for requesting a value from the cache.
	 * @param cachedInfo The information that is cached.
	 * @return <code>true</code> if the cached information has become stale.
	 * @throws IOException if there was an error checking the cached information for staleness.
	 */
	public boolean isStaleData(final Q query, final FileData cachedInfo) throws IOException {
		if(super.isStaleData(query, cachedInfo)) { //if the default stale checks think the information is stale
			return true; //the information is stale
		}
		final Repository repository = query.getRepository(); //get the repository
		final URI resourceURI = query.getResourceURI(); //get the resource URI				
		final URFResource resource = repository.getResourceDescription(resourceURI); //get a description of the resource
		final Date cachedModifiedTime = cachedInfo.getModifiedTime(); //get the cached modified time of the resource
		if(cachedModifiedTime != null) { //if we know the modified time of the cached resource
			final ISODateTime modifiedDateTime = getModified(resource); //get the current modified date time of the resource
			//Log.trace("cache: is stale?", resourceURI, "cached modified time", new URFDateTime(cachedModifiedTime), "resource modified time", modifiedDateTime, !cachedModifiedTime.equals(modifiedDateTime));
			return !cachedModifiedTime.equals(modifiedDateTime); //if the modified time doesn't match our record, the cache is stale; we don't have to worry about whether there is millisecond precision, as both values being compared should be coming from the same resource in the same repository
		}
		return false; //we couldn't find a reason that the cached information is stale 
	}

	/**
	 * Fetches data from the backing store.
	 * @param query The query for requesting a value from the cache.
	 * @return New information to cache.
	 * @throws IOException if there was an error fetching the value from the backing store.
	 * @see #getCacheDirectory(AbstractMarmotResourceCacheQuery)
	 * @see #getCacheBaseName(AbstractMarmotResourceCacheQuery)
	 */
	public final FileData fetchData(final Q query) throws IOException {
		//Log.info("Starting to fetch resource", key.getResourceURI());
		final Repository repository = query.getRepository(); //get the repository
		final URI resourceURI = query.getResourceURI(); //get the resource URI				
		final URFResource resource = repository.getResourceDescription(resourceURI); //get a description of the resource
		return fetch(query, resource, getCacheDirectory(query), getCacheBaseName(query));
	}

	/**
	 * Determines the directory in which to store cached information for the given query. The returned directory will already be created if needed.
	 * @param query The query for requesting a value from the cache.
	 * @return The directory to use for for storing cached information for the query.
	 * @throws IOException If there was an error retrieving the cache directory.
	 */
	protected abstract File getCacheDirectory(final Q query) throws IOException;

	/**
	 * Determines the base name to use for storing the resource specified by the query.
	 * <p>
	 * This implementation removes the resource URI extension and encodes the entire URI for use as a filename.
	 * </p>
	 * @param query The query for requesting a value from the cache.
	 * @return The base name to use for the cached file.
	 */
	protected String getCacheBaseName(final Q query) {
		return encodeCrossPlatformFilename(removeRawNameExtension(query.getResourceURI()).toString()); //remove the extension and encode the entire URI as a filename
	}

	/**
	 * Fetches data from the backing store.
	 * @param query The query for requesting a value from the cache.
	 * @param resource The description of the resource to fetch.
	 * @param cacheDirectory The directory in which to store the cached file.
	 * @param cacheBaseName The base name to use for the cached file.
	 * @return New information to cache.
	 * @throws IOException if there was an error fetching the value from the backing store.
	 */
	protected FileData fetch(final Q query, final URFResource resource, final File cacheDirectory, final String cacheBaseName) throws IOException {
		final Repository repository = query.getRepository(); //get the repository
		final URI resourceURI = query.getResourceURI(); //get the resource URI				
		final ISODateTime modifiedDateTime = getModified(resource); //get the last modified time of the resource before it is filtered
		final String extension = URIs.getNameExtension(resourceURI); //get the extension to use TODO important: check for a collection, as it may be possible to cache collection content in the future
		//TODO important: check for null extension
		File cacheFile = new File(cacheDirectory, cacheBaseName + FILENAME_EXTENSION_SEPARATOR + extension); //create a filename in the form cacheDir/cacheBaseName.ext
		//Log.trace("cache: fetching resource", resourceURI, "resource modified", modifiedDateTime, "cache file", cacheFile, "cache file exists", cacheFile.exists(), "cache file modified", new URFDateTime(cacheFile.lastModified()));
		//Log.trace("modifiedTime", modifiedDateTime.getTime(), "cacheModifiedTime", cacheFile.lastModified(), "delta", modifiedDateTime.getTime()-cacheFile.lastModified());
		if(modifiedDateTime == null || !cacheFile.exists() || modifiedDateTime.getTime() - cacheFile.lastModified() >= 1000) { //if we don't know when the resource was modified, or if there is no such cached file, or if the real resource was modified after the cached version (some file systems only have second precision, so ignore milliseconds)
		//Log.trace("cache: don't have existing cache");
			final InputStream inputStream = new BufferedInputStream(repository.getResourceInputStream(resourceURI)); //get a stream to the resource
			try {
				copy(inputStream, cacheFile); //copy the resource to the file, replacing it if there already is such a file TODO solve the race condition of another file trying to load the old file while we're trying to replace it---or maybe find some way to recover, or use a different file, although that would complicate this algorithm
			} finally {
				inputStream.close(); //always close the stream to the resource
			}
			if(modifiedDateTime != null) { //if know when the resource was modified
				cacheFile.setLastModified(modifiedDateTime.getTime()); //update the cached file's modified time so that we will know when the resource was modified
			}
		}
		return new FileData(cacheFile, modifiedDateTime); //return the cached file, which may have been filtered
	}

	/**
	 * Performs any operations that need to be done when cached information is discarded (for example, if the cached information is stale). This version deletes
	 * the file used for caching.
	 * @param key The key for the cached information.
	 * @param cachedInfo The information that is cached.
	 * @throws IOException if there was an error discarding the cached information.
	 */
	/*TODO fix, maybe; but how will we know whether the file can be deleted? maybe it's still being used 
		public void discard(final ResourceCacheKey key, final CachedResourceInfo cachedInfo) throws IOException
		{
			super.discard(key, cachedInfo);	//do the default discarding
		}
	*/

	/**
	 * A query for cached Marmot resources.
	 * @author Garret Wilson
	 */
	public abstract static class AbstractMarmotResourceCacheQuery<KK extends MarmotResourceCacheKey> implements Query<KK> {

		/** The repository in which the resource is located. */
		private final Repository repository;

		/** @return The repository in which the resource is located. */
		public Repository getRepository() {
			return repository;
		}

		/** The URI of the resource. */
		private final URI resourceURI;

		/** @return The URI of the resource. */
		public URI getResourceURI() {
			return resourceURI;
		}

		/** A key for looking up data for the query. */
		private final KK key;

		/** @return A key for looking up data for the query. */
		public KK getKey() {
			return key;
		}

		/**
		 * Repository, resource URI, and key constructor.
		 * @param repository The repository in which the resource is stored.
		 * @param resourceURI The URI of the resource.
		 * @param key The key for looking up data for the query.
		 * @throws NullPointerException if the given repository, resource URI, and/or key is <code>null</code>.
		 */
		public AbstractMarmotResourceCacheQuery(final Repository repository, final URI resourceURI, final KK key) {
			this.repository = requireNonNull(repository, "Repository cannot be null.");
			this.resourceURI = resourceURI; //save the resource URI
			this.key = requireNonNull(key, "Key cannot be null.");
		}
	}

	/**
	 * A key for cached Marmot resources.
	 * @author Garret Wilson
	 */
	public static class MarmotResourceCacheKey extends AbstractHashObject {

		/** The URI of the repository in which the resource is located. */
		private final URI repositoryURI;

		/** @return The URI of the repository in which the resource is located. */
		public URI getRepositoryURI() {
			return repositoryURI;
		}

		/** The URI of the resource. */
		private final URI resourceURI;

		/** @return The URI of the resource. */
		public URI getResourceURI() {
			return resourceURI;
		}

		/**
		 * Repository and resource URI constructor.
		 * @param repository The repository in which the resource is stored.
		 * @param resourceURI The URI of the resource.
		 * @throws NullPointerException if the given repository and/or resource URI is <code>null</code>.
		 */
		public MarmotResourceCacheKey(final Repository repository, final URI resourceURI) {
			this(repository, resourceURI, repository.getRootURI(), resourceURI);
		}

		/**
		 * Repository, resource URI, and hash objects constructor. The objects should include the repository and resource URI.
		 * @param repository The repository in which the resource is stored.
		 * @param resourceURI The URI of the resource.
		 * @param objects The objects for hashing and equality, any or all of which can be <code>null</code>.
		 * @throws NullPointerException if the given repository, resource URI, and/or objects is <code>null</code>.
		 */
		protected MarmotResourceCacheKey(final Repository repository, final URI resourceURI, final Object... objects) {
			super(objects);
			this.repositoryURI = repository.getRootURI(); //TODO ensure not null
			this.resourceURI = requireNonNull(resourceURI, "Resource URI cannot be null."); //save the resource URI
		}

	}

}
