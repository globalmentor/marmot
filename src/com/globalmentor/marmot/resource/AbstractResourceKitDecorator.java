package com.globalmentor.marmot.resource;

import java.io.IOException;
import java.net.URI;

import javax.mail.internet.ContentType;

import static com.garretwilson.lang.ObjectUtilities.*;
import com.garretwilson.rdf.RDFResource;

import com.globalmentor.marmot.MarmotSession;
import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.security.PermissionType;

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
	
	/**Initializes a resource description, creating whatever properties are appropriate.
	@param repository The repository to use to access the resource content, if needed.
	@param resource The resource description to initialize.
	@exception IOException if there is an error accessing the repository.
	*/
	public void initializeResourceDescription(final Repository repository, final RDFResource resource) throws IOException {getResourceKit().initializeResourceDescription(repository, resource);}

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
	public ResourceFilter[] getAspectFilters(final String aspectID) {return getResourceKit().getAspectFilters(aspectID);}

}
