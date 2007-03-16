package com.globalmentor.marmot.security;

import java.net.URI;
import java.security.Principal;

import com.garretwilson.rdf.RDFResource;
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
	@param owner The principal that owns the repository.
	@param repository The repository that contains the resource.
	@param user The user attempting to access the resource, which may be <code>null</code> if the user is anonymous.
	@param permissionType The type of permission requested, indicated by permission type URI.
	@return A description of the requested permission the given user has in relation to a resource in a repository, or <code>null</code> if the user has no such permissions.
	@exception NullPointerException if the given owner, repository, resource URI, and/or permission URI is <code>null</code>.
	*/
	public RDFResource getPermission(final Principal owner, final Repository repository, final URI resourceURI, final Principal user, final URI permissionType);

}
