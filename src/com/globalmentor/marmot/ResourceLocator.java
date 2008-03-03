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
	}

	/**@return A full URI to the resource, resolved to the repository.*/
	public URI getResourceURI()
	{
		return getRepository().getURI().resolve(getResourcePath().toURI());	//resolve the mound path to the repository		
	}

}
