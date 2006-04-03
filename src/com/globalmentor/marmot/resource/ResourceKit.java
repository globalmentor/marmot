package com.globalmentor.marmot.resource;

import java.io.IOException;
import java.net.URI;

import javax.mail.internet.ContentType;

import com.garretwilson.rdf.*;

import com.globalmentor.marmot.repository.Repository;

/**Support for working with a resource in a repository.
@author Garret Wilson
*/
public interface ResourceKit
{

	/**An empty array of extensions.*/
//TODO del if not needed	public final static String[] NO_EXTENSIONS=new String[] {};

	/**An empty array of content types.*/
	public final static ContentType[] NO_CONTENT_TYPES=new ContentType[] {};

	/**An empty array of resource type URIs.*/
	public final static URI[] NO_RESOURCE_TYPES=new URI[] {};

	/**Returns the default file extensions used for the resource URI.
	@return The default file extension this resource kit uses, or <code>null</code> if by default this resource kit does not use an extension.
	*/
//TODO del if not needed	public String getDefaultExtension();

	/**Returns the file extensions supported for the resource URI.
	@return A non-<code>null</code> array of the extensions this resource kit supports.
	*/
//TODO del if not needed	public String[] getSupportedExtensions();

	/**Returns the content types supported.
	This is the primary method of determining which resource kit to use for a given resource.
	@return A non-<code>null</code> array of the content types this resource kit supports.
	*/
	public ContentType[] getSupportedContentTypes();

	/**Returns the resource types supported.
	This is the secondary method of determining which resource kit to use for a given resource.
	@return A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
	*/
	public URI[] getSupportedResourceTypes();
	
	/**Returns the URI of an open icon representing the given resource.
	@param repository The repository in which the resource resides.
	@param resource The resource for which an icon should be returned.
	@return The URI of an icon to display for an open tree node for this resource, or <code>null</code> if no icon URI is available.
	*/
	public URI getOpenTreeNodeIconURI(final Repository repository, final RDFResource resource);

	/**Returns the resource key of an open icon representing the given resource.
	@param repository The repository in which the resource resides.
	@param resource The resource for which an icon should be returned.
	@return The resource key of an icon to display for an open tree node for this resource, or <code>null</code> if no icon resource key is available.
	*/
	public String getOpenTreeNodeIconResourceKey(final Repository repository, final RDFResource resource);

	/**Returns the URI of a closed icon representing the given resource.
	@param repository The repository in which the resource resides.
	@param resource The resource for which an icon should be returned.
	@return The URI of an icon to display for a closed tree node for this resource, or <code>null</code> if no icon URI is available.
	*/
	public URI getClosedTreeNodeIconURI(final Repository repository, final RDFResource resource);

	/**Returns the resource key of a closed icon representing the given resource.
	@param repository The repository in which the resource resides.
	@param resource The resource for which an icon should be returned.
	@return The resource key of an icon to display for a closed tree node for this resource, or <code>null</code> if no icon resource key is available.
	*/
	public String getClosedTreeNodeIconResourceKey(final Repository repository, final RDFResource resource);

	/**Returns the URI of a leaf icon representing the given resource.
	@param repository The repository in which the resource resides.
	@param resource The resource for which an icon should be returned.
	@return The URI of an icon to display for a leaf node for this resource, or <code>null</code> if no icon URI is available.
	*/
	public URI getLeafTreeNodeIconURI(final Repository repository, final RDFResource resource);

	/**Returns the resource key of a leaf icon representing the given resource.
	@param repository The repository in which the resource resides.
	@param resource The resource for which an icon should be returned.
	@return The resource key of an icon to display for a leaf node for this resource, or <code>null</code> if no icon resource key is available.
	*/
	public String getLeafTreeNodeIconResourceKey(final Repository repository, final RDFResource resource);

	/**Returns the URI of a general icon representing the given resource.
	@param repository The repository in which the resource resides.
	@param resource The resource for which an icon should be returned.
	@return The URI of a general icon representing the given resource, or <code>null</code> if no icon URI is available.
	*/
	public URI getIconURI(final Repository repository, final RDFResource resource);

	/**Returns the resource key of a general icon representing the given resource.
	@param repository The repository in which the resource resides.
	@param resource The resource for which an icon should be returned.
	@return The resource key of a general icon representing the given resource, or <code>null</code> if no icon resource key is available.
	*/
	public String getIconResourceKey(final Repository repository, final RDFResource resource);

	/**Returns the URI of a general icon representing this resource kit.
	@return The URI of a general icon representing this resource kit, or <code>null</code> if no icon URI is available.
	*/
	public URI getIconURI();

	/**Returns the resource key of a general icon representing this resource kit.
	@return The resource key of a general icon representing this resource kit, or <code>null</code> if no icon resource key is available.
	*/
	public String getIconResourceKey();
	
	/**Initializes a resource description, creating whatever properties are appropriate.
	@param repository The repository to use to access the resource content, if needed.
	@param resource The resource description to initialize.
	@exception IOException if there is an error accessing the repository.
	*/
	public void initializeResourceDescription(final Repository repository, final RDFResource resource) throws IOException;
	
}
