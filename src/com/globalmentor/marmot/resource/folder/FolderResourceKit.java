package com.globalmentor.marmot.resource.folder;

import java.net.URI;

import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.resource.AbstractResourceKit;
import com.globalmentor.net.URIPath;
import com.globalmentor.net.URIs;

/**Resource kit for handling collections as folder resources that have no content but can contain children.
@author Garret Wilson
*/
public class FolderResourceKit extends AbstractResourceKit
{

	/**Default constructor.*/
	public FolderResourceKit()
	{
		super(Capability.CREATE);
	}

	/**Returns the URI of a child resource with the given simple name within a parent resource.
	This is normally the simple name resolved against the parent resource URI, although a resource kit for collections may append an ending path separator.
	The simple name will be encoded before being used to construct the URI.
	This version first appends an ending path separator before resolving the name against the parent resource URI.
	@param repository The repository that contains the resource.
	@param parentResourceURI The URI to of the parent resource.
	@param resourceName The unencoded simple name of the child resource.
	@return The URI of the child resource
	@exception NullPointerException if the given repository and/or resource URI is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	*/
	public URI getChildResourceURI(final Repository repository, final URI parentResourceURI, final String resourceName)
	{
		//TODO fix IllegalArgumentException by checking to ensure that the parent resource is within the repository
		return parentResourceURI.resolve(URIPath.createURIPathURI(URIPath.encodeSegment(resourceName)+URIs.PATH_SEPARATOR));	//encode the resource name, append a path separator, and resolve it against the parent resource URI; use the special URIPath method in case the name contains a colon character
	}

}
