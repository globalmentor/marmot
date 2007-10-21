package com.globalmentor.marmot.resource.folder;

import java.net.URI;

import static com.garretwilson.net.URIConstants.*;
import com.garretwilson.net.ResourceIOException;
import com.garretwilson.net.URIPath;
import com.garretwilson.urf.*;
import static com.garretwilson.urf.URF.*;

import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.resource.AbstractResourceKit;

/**Resource kit for handling folder resources that have no content but can contain children.
<p>Supported URF resource types:</p>
<ul>
<li>{@value URF#LIST_CLASS_URI}</li>
</ul>
@author Garret Wilson
*/
public class FolderResourceKit extends AbstractResourceKit
{

	/**Default constructor.*/
	public FolderResourceKit()
	{
		super(LIST_CLASS_URI, Capability.CREATE);
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
		return parentResourceURI.resolve(URIPath.encodeSegment(resourceName)+PATH_SEPARATOR);	//encode the resource name, append a path separator, and resolve it against the parent resource URI
	}

	/**Creates a new resource with the appropriate default contents for this resource type.
	If a resource already exists at the given URI it will be replaced.
	This implementation creates a new collection.
	@param repository The repository that will contain the resource.
	@param resourceURI The reference URI to use to identify the resource.
	@return A description of the resource that was created.
	@exception NullPointerException if the given repository and/or resource URI is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception ResourceIOException if the resource could not be created.
	*/
	public URFResource createResource(final Repository repository, final URI resourceURI) throws ResourceIOException
	{
		return repository.createCollection(resourceURI);	//create a new collection
	}
}
