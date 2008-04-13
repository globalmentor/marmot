package com.globalmentor.marmot.resource;

import java.io.OutputStream;
import java.net.URI;
import java.util.Set;

import javax.mail.internet.ContentType;

import static com.globalmentor.java.Objects.*;
import com.globalmentor.marmot.MarmotSession;
import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.security.PermissionType;
import com.globalmentor.net.ResourceIOException;
import com.globalmentor.urf.URFResource;

/**A resource kit that decorates another resource kit.
@author Garret Wilson
*/
public abstract class AbstractResourceKitDecorator implements ResourceKit
{

	/**The decorated resource kit.*/
	private final ResourceKit resourceKit;

		/**@return The decorated resource kit.*/
		protected ResourceKit getResourceKit() {return resourceKit;}

	/**Decorated resource kit constructor.
	@param resourceKit The resource kit to decorate.
	@exception NullPointerException if the given resource kit is <code>null</code>.
	*/
	public AbstractResourceKitDecorator(final ResourceKit resourceKit)
	{
		this.resourceKit=checkInstance(resourceKit, "Resource kit cannot be null.");
	}
		
	/**@return The Marmot instance with which this resource kit is associated, or <code>null</code> if this resource kit has not yet been installed.*/
	public MarmotSession<?> getMarmotSession() {return getResourceKit().getMarmotSession();}

	/**Sets the Marmot instance with which this resource kit is associated.
	@param marmot The Marmot instance with which the resource kit should be associated, or <code>null</code> if the resource kit is not installed.
	*/
	public void setMarmotSession(final MarmotSession<?> marmot) {getResourceKit().setMarmotSession(marmot);}

	/**Returns the content types supported.
	This is the primary method of determining which resource kit to use for a given resource.
	@return A non-<code>null</code> array of the content types this resource kit supports.
	*/
	public ContentType[] getSupportedContentTypes() {return getResourceKit().getSupportedContentTypes();}

	/**Returns the resource types supported.
	This is the secondary method of determining which resource kit to use for a given resource.
	@return A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
	*/
	public URI[] getSupportedResourceTypes() {return getResourceKit().getSupportedResourceTypes();}

	/**Returns the default name extension used for the resource URI.
	@return The default name extension this resource kit uses, or <code>null</code> if by default this resource kit does not use an extension.
	*/
	public String getDefaultNameExtension() {return getResourceKit().getDefaultNameExtension();}

	/**Returns the capabilities of this resource kit.
	@return The capabilities provided by this resource kit.
	*/
	public Set<Capability> getCapabilities() {return getResourceKit().getCapabilities();}

	/**Determines if the resource kit has all the given capabilities.
	@param capabilities The capabilities to check.
	@return <code>true</code> if the resource kit has all the given capabilities, if any.
	*/
	public boolean hasCapabilities(final Capability... capabilities) {return getResourceKit().hasCapabilities(capabilities);}

	/**Retrieves a default resource description for a given resource, without regard to whether it exists.
	@param repository The repository within which the resource would reside.
	@param resourceURI The URI of the resource for which a default resource description should be retrieved.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public URFResource getDefaultResourceDescription(final Repository repository, final URI resourceURI) throws ResourceIOException {return getResourceKit().getDefaultResourceDescription(repository, resourceURI);}

	/**Initializes a resource description, creating whatever properties are appropriate.
	@param repository The repository to use to access the resource content, if needed.
	@param resource The resource description to initialize.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public void initializeResourceDescription(final Repository repository, final URFResource resource) throws ResourceIOException {getResourceKit().initializeResourceDescription(repository, resource);}

	/**Returns the URI of the collection of a child resource.
	This is normally the collection URI of the parent resource URI.
	Some implementations may prefer for child resources by default to be placed within sub-collections.
	@param repository The repository that contains the resource.
	@param parentResourceURI The URI to of the parent resource.
	@return The URI of the child resource collection
	@exception NullPointerException if the given repository and/or parent resource URI is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception ResourceIOException if there is an error accessing the repository.
	@see Repository#getCollectionURI(URI)
	*/
	public URI getChildResourceCollectionURI(final Repository repository, final URI parentResourceURI) throws ResourceIOException {return getResourceKit().getChildResourceCollectionURI(repository, parentResourceURI);}

	/**Returns the URI of a child resource with the given simple name within a parent resource.
	This is normally the simple name resolved against the child resource collection URI, although a resource kit for collections may append an ending path separator.
	The simple name will be encoded before being used to construct the URI.
	@param repository The repository that contains the resource.
	@param parentResourceURI The URI to of the parent resource.
	@param resourceName The unencoded simple name of the child resource.
	@return The URI of the child resource.
	@exception NullPointerException if the given repository and/or resource URI is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception ResourceIOException if there is an error accessing the repository.
	@see #getDefaultNameExtension()
	*/
	public URI getChildResourceURI(final Repository repository, final URI parentResourceURI, final String resourceName) throws ResourceIOException {return getResourceKit().getChildResourceURI(repository, parentResourceURI, resourceName);}

	/**Creates a new resource with the appropriate default contents for this resource type.
	If a resource already exists at the given URI it will be replaced.
	If the resource URI is a collection URI, a collection resource will be created.
	@param resourceURI The reference URI to use to identify the resource.
	@return A description of the resource that was created.
	@exception NullPointerException if the given resource URI is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception ResourceIOException if the resource could not be created.
	*/
	public URFResource createResource(final Repository repository, final URI resourceURI) throws ResourceIOException {return getResourceKit().createResource(repository, resourceURI);}

	/**Creates a new resource with the given description and the appropriate default contents for this resource type.
	If a resource already exists at the given URI it will be replaced.
	If the resource URI is a collection URI, a collection resource will be created.
	@param repository The repository that will contain the resource.
	@param resourceURI The reference URI to use to identify the resource.
	@param resourceDescription A description of the resource; the resource URI is ignored.
	@return A description of the resource that was created.
	@exception NullPointerException if the given repository and/or resource URI is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception ResourceIOException if the resource could not be created.
	*/
	public URFResource createResource(final Repository repository, final URI resourceURI, final URFResource resourceDescription) throws ResourceIOException {return getResourceKit().createResource(repository, resourceURI, resourceDescription);}

	/**Writes default resource content for the given resource.
	If content already exists for the given resource it will be replaced.
	@param repository The repository that contains the resource.
	@param resourceURI The reference URI to use to identify the resource, which may not exist.
	@param resourceDescription A description of the resource; the resource URI is ignored.
	@return A description of the resource the content of which was written.
	@exception NullPointerException if the given repository, resource URI, and/or resource description is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception ResourceIOException if the default resource content could not be written.
	@see #writeDefaultResourceContent(Repository, URI, URFResource, OutputStream)
	*/
	public URFResource writeDefaultResourceContent(final Repository repository, final URI resourceURI, final URFResource resourceDescription) throws ResourceIOException {return getResourceKit().writeDefaultResourceContent(repository, resourceURI, resourceDescription);}

	/**Writes default resource content to the given output stream.
	If content already exists for the given resource it will be replaced.
	This version writes no content.
	@param repository The repository that contains the resource.
	@param resourceURI The reference URI to use to identify the resource, which may not exist.
	@param resourceDescription A description of the resource; the resource URI is ignored.
	@exception NullPointerException if the given repository, resource URI, resource description, and/or output stream is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception ResourceIOException if the default resource content could not be written.
	*/
	public void writeDefaultResourceContent(final Repository repository, final URI resourceURI, final URFResource resourceDescription, final OutputStream outputStream) throws ResourceIOException {getResourceKit().writeDefaultResourceContent(repository, resourceURI, resourceDescription, outputStream);}

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
	public boolean isAspectAllowed(final String aspectID, final PermissionType permissionType) {return getResourceKit().isAspectAllowed(aspectID, permissionType);}

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
	public ResourceContentFilter[] getAspectFilters(final String aspectID) {return getResourceKit().getAspectFilters(aspectID);}

}
