/*
 * Copyright © 1996-2008 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

import java.net.URI;


import static com.globalmentor.java.Objects.*;
import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.net.URIPath;

/**Encapsulates a resource and the repository within which it exists.
@author Garret Wilson
*/
public class ResourceLocator
{

	/**The repository in which the resource exists.*/
	private final Repository repository;

		/**@return The repository in which the resource exists.*/
		public Repository getRepository() {return repository;}

	/**The full URI to the resource within the respository.*/
	private final URI resourceURI;

		/**@return The full URI to the resource within the respository.*/
		public URI getResourceURI() {return resourceURI;}

	/**The path to the resource, relative to the repository.*/
	private URIPath resourcePath;

		/**@return The path to the resource, relative to the repository.*/
		public URIPath getResourcePath() {return resourcePath;}

	/**Repository and resource path constructor.
	@param repository The respository in which the resource exists
	@param resourcePath The path to the resource, relative to the repository.
	@exception NullPointerException if the given repository and/or path is <code>null</code>.
	@exception IllegalArgumentException if the given path is not a relative path.
	*/
	public ResourceLocator(final Repository repository, final URIPath resourcePath)
	{
		this.repository=checkInstance(repository, "Repository cannot be null.");
		this.resourcePath=resourcePath.checkRelative();
		this.resourceURI=repository.getURI().resolve(resourcePath.toURI());	//resolve the resource path to the repository
	}

}
