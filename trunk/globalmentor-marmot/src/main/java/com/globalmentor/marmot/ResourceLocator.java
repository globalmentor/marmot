/*
 * Copyright © 1996-2012 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

import java.io.IOException;
import java.net.URI;

import org.urframework.URFResource;
import org.urframework.content.Content;

import static com.globalmentor.java.Objects.*;

import com.globalmentor.log.Log;
import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.net.ContentType;
import com.globalmentor.net.URIPath;

import static com.globalmentor.net.URIs.*;

/**
 * Encapsulates a resource and the repository within which it exists.
 * <p>
 * This implementation lazily determines a content type if possible and caches it. This content type value is not intended to be an absolutely certain
 * reflection of current resource properties, but a best-available indication of resource content type for deciding how to work with the resource.
 * </p>
 * @author Garret Wilson
 */
public class ResourceLocator {

	/** Shared dummy content type indicating that no content type could be determined. */
	//TODO del if not needed	private static final ContentType NO_CONTENT_TYPE = ContentType.getInstance("null", "null");

	/** The repository in which the resource exists. */
	private final Repository repository;

	/** @return The repository in which the resource exists. */
	public Repository getRepository() {
		return repository;
	}

	/** The full URI to the resource within the repository. */
	private final URI resourceURI;

	/** @return The full URI to the resource within the repository. */
	public URI getResourceURI() {
		return resourceURI;
	}

	/** The path to the resource, relative to the repository. */
	private URIPath resourcePath;

	/** @return The path to the resource, relative to the repository. */
	public URIPath getResourcePath() {
		return resourcePath;
	}

	/**
	 * The cached content type, which may reference {@link #NO_CONTENT_TYPE} if no content type could be determined, or <code>null</code> if the content type has
	 * not yet been retrieved.
	 */
	//TODO del if not needed	private ContentType cachedContentType = null;

	/**
	 * Retrieves the content type for the resource. Any errors are logged but not returned or thrown.
	 * @return The resource content type, or <code>null</code> if the content type could not be determined.
	 */
	/*TODO del if not needed
		protected ContentType retrieveContentType()
		{
			final URFResource resource;
			try
			{
				resource = getRepository().getResourceDescription(getResourceURI()); //get a description of the resource
				return Content.getContentType(resource); //get the content type if possible
			}
			catch(final IOException ioException)
			{
				Log.warn(ioException);
				return null;
			}
		}
	*/

	/**
	 * Retrieves the determined content type of the resource, which may have been cached.
	 * @return The resource content type, or <code>null</code> if the content type could not be determined.
	 */
	/*TODO del if not needed
		public ContentType getContentType()
		{
			if(cachedContentType == null) {	//if the content type hasn't been cached (the race condition here is benign)
				final ContentType contentType = retrieveContentType(); //retrieve the content type
				cachedContentType = contentType != null ? contentType : NO_CONTENT_TYPE; //cache the content type
			}
			return cachedContentType != NO_CONTENT_TYPE ? cachedContentType : null; //return the cached content type
		}
	*/

	/**
	 * Repository and resource path constructor.
	 * @param repository The repository in which the resource exists
	 * @param resourcePath The path to the resource, relative to the repository.
	 * @throws NullPointerException if the given repository and/or path is <code>null</code>.
	 * @throws IllegalArgumentException if the given path is not a relative path.
	 */
	public ResourceLocator(final Repository repository, final URIPath resourcePath) {
		this.repository = checkInstance(repository, "Repository cannot be null.");
		this.resourcePath = resourcePath.checkRelative();
		this.resourceURI = resolve(repository.getRootURI(), resourcePath.toURI()); //resolve the resource path to the repository
	}

}
