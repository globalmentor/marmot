package com.globalmentor.marmot.resource;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;

import javax.mail.internet.ContentType;

import com.garretwilson.net.ResourceIOException;
import com.garretwilson.rdf.*;

import com.globalmentor.marmot.MarmotSession;
import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.security.PermissionType;

/**Support for working with a resource in a repository.
@param <P> The type of presentation supported by this resource kit.
@author Garret Wilson
*/
public interface ResourceKit<P extends Presentation>
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

	/**@return The Marmot instance with which this resource kit is associated, or <code>null</code> if this resource kit has not yet been installed.*/
	public MarmotSession<P, ? extends ResourceKit<P>> getMarmot();

	/**Sets the Marmot instance with which this resource kit is associated.
	@param marmot The Marmot instance with which the resource kit should be assiated, or <code>null</code> if the resource kit is not installed.
	*/
	public void setMarmot(final MarmotSession<P, ? extends ResourceKit<P>> marmot);

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

	/**Returns the URI of a closed icon representing the given resource.
	@param repository The repository in which the resource resides.
	@param resource The resource for which an icon should be returned.
	@return The URI of an icon to display for a closed tree node for this resource, or <code>null</code> if no icon URI is available.
	*/
	public URI getClosedTreeNodeIconURI(final Repository repository, final RDFResource resource);

	/**Returns the URI of a leaf icon representing the given resource.
	@param repository The repository in which the resource resides.
	@param resource The resource for which an icon should be returned.
	@return The URI of an icon to display for a leaf node for this resource, or <code>null</code> if no icon URI is available.
	*/
	public URI getLeafTreeNodeIconURI(final Repository repository, final RDFResource resource);

	/**Returns the URI of a general icon representing the given resource.
	@param repository The repository in which the resource resides.
	@param resource The resource for which an icon should be returned.
	@return The URI of a general icon representing the given resource, or <code>null</code> if no icon URI is available.
	*/
	public URI getIconURI(final Repository repository, final RDFResource resource);

	/**Returns the URI of a general icon representing this resource kit.
	@return The URI of a general icon representing this resource kit, or <code>null</code> if no icon URI is available.
	*/
	public URI getIconURI();

	/**Initializes a resource description, creating whatever properties are appropriate.
	@param repository The repository to use to access the resource content, if needed.
	@param resource The resource description to initialize.
	@exception IOException if there is an error accessing the repository.
	*/
	public void initializeResourceDescription(final Repository repository, final RDFResource resource) throws IOException;

	/**@return The presentation implementation for supported resources.*/
	public P getPresentation();

	/**Returns this resource kit's installed filter based upon its ID.
	@param filterID The ID of the filter to return.
	@return The resource filter identified by the given ID.
	@exception IllegalArgumentException if there is no installed resource filter identified by the given ID.
	*/
//TODO del	public ResourceFilter getFilter(final String filterID) throws IllegalArgumentException;

	/**Determines whether a given user has permission to access a particular aspect of a resource.
	@param owner The principal that owns the repository.
	@param repository The repository that contains the resource.
	@param user The user attempting to access the resource, which may be <code>null</code> if the user is anonymous.
	@param aspectID The ID of the aspect requested.
	@return <code>true</code> if access to the given aspect is allowed for the user in relation to the indicated resource, else <code>false</code>.
	@exception NullPointerException if the given owner, repository, resource URI, and/or permission type is <code>null</code>.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
//TODO fix	public boolean isAllowed(final Principal owner, final Repository repository, final URI resourceURI, final Principal user, final PermissionType permissionType) throws ResourceIOException;

	/**Determines whether the given permission is appropriate for accessing the identified aspect.
	This prevents aspects from being accessed at lower permissions.
	For example, a rogue user may attempt to retrieve a preview-permission aspect such as a high-resolution image
	using a permission such as {@link PermissionType#EXECUTE} when a permission appropriate to the aspect, {@link PermissionType#PREVIEW},
	is not allowed to the user.
	@param aspectID The serialized form of the ID of the aspect to be accessed.
	@param permissionType The type of permission requested.
	@return <code>true</code> if access to the given aspect is allowed using the given permission, else <code>false</code>.
	@exception NullPointerException if the given aspect ID and/or permission type is <code>null</code>.
	*/
	public boolean isAspectAllowed(final String aspectID, final PermissionType permissionType);

	/**Returns the permissions that 
	This prevents aspects from being accessed at lower permissions.
	For example, a rogue user may attempt to access a preview-permission aspect such as a high-resolution image using a permission such as 
	*/
/*TODO fix
	public boolean getAspectPermissions(final String aspectID, final PermissionType permissionType)
	{
		
	}
*/

	/**Returns the appropriate filters for accessing an identified aspect of the resource.
	@param aspectID The serialized form of the ID of the aspect to be accessed.
	@exception NullPointerException if the given aspect ID is <code>null</code>.
	@exception IllegalArgumentException if the given aspect ID does not represent a valid aspect.
	*/
	public ResourceFilter[] getAspectFilters(final String aspectID);

}
