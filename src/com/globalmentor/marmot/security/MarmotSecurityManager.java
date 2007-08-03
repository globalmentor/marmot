package com.globalmentor.marmot.security;

import java.net.URI;
import java.security.Principal;

import com.garretwilson.net.ResourceIOException;
import com.globalmentor.marmot.repository.Repository;

/**Security manager delegate for Marmot.
@author Garret Wilson
*/
public interface MarmotSecurityManager
{

	/**Determines if the given principal is the owner of the given resource in the given repository.
	@param principal The principal in question.
	@param repository The repository containing the resource.
	@param resourceURI The 
	@return
	*/
//TODO fix	public boolean isResourceOwner(final Principal principal, final Repository repository, final URI resourceURI);

	/**Determines whether a given user has permission to perform some action in relation to a given repository and resource.
	This method is additive; if a superclass doesn't find a permission, a subclass may be able to add the permission.
	This is a convenience method that delegates to {@link #isAllowed(Principal, Repository, URI, Principal, URI)}.
	@param owner The principal that owns the repository.
	@param repository The repository that contains the resource.
	@param user The user attempting to access the resource, which may be <code>null</code> if the user is anonymous.
	@param permissionType The type of permission requested.
	@return <code>true</code> if the given permission is allowed for the user in relation to the indicated resource, else <code>false</code>.
	@exception NullPointerException if the given owner, repository, resource URI, and/or permission type is <code>null</code>.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public boolean isAllowed(final Principal owner, final Repository repository, final URI resourceURI, final Principal user, final PermissionType permissionType) throws ResourceIOException;

	/**Determines whether a given user has permission to perform some action in relation to a given repository and resource.
	This method is additive; if a superclass doesn't find a permission, a subclass may be able to add the permission.
	This implementation allows all permissions if the user is the owner.
	@param owner The principal that owns the repository.
	@param repository The repository that contains the resource.
	@param user The user attempting to access the resource, which may be <code>null</code> if the user is anonymous.
	@param permissionTypeURI The type of permission requested, indicated by permission type URI.
	@return <code>true</code> if the given permission is allowed for the user in relation to the indicated resource, else <code>false</code>.
	@exception NullPointerException if the given owner, repository, resource type, and/or permission URI is <code>null</code>.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
//TODO del if nto needed	public boolean isAllowed(final Principal owner, final Repository repository, final URI resourceURI, final Principal user, final URI permissionTypeURI) throws ResourceIOException;

}
