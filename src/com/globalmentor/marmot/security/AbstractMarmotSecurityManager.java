package com.globalmentor.marmot.security;

import java.net.URI;
import java.security.Principal;

import static com.garretwilson.lang.ObjectUtilities.*;

import com.garretwilson.rdf.DefaultRDFResource;
import com.garretwilson.rdf.RDFResource;
import com.globalmentor.marmot.repository.Repository;

/**Abstract implementation of a security manager for Marmot.
@author Garret Wilson
*/
public class AbstractMarmotSecurityManager implements MarmotSecurityManager
{

	/**Determines whether a given user has permission to perform some action in relation to a given repository and resource.
	This method is additive; if a superclass doesn't find a permission, a subclass may be able to add the permission.
	This is a convenience method that delegates to {@link #getPermission(Principal, Repository, URI, Principal, URI)}.
	@param owner The principal that owns the repository.
	@param repository The repository that contains the resource.
	@param user The user attempting to access the resource, which may be <code>null</code> if the user is anonymous.
	@param permissionType The type of permission requested.
	@return A description of the requested permission the given user has in relation to a resource in a repository, or <code>null</code> if the user has no such permissions.
	@exception NullPointerException if the given owner, repository, resource URI, and/or permission type is <code>null</code>.
	*/
	public RDFResource getPermission(final Principal owner, final Repository repository, final URI resourceURI, final Principal user, final PermissionType permissionType)
	{
		return getPermission(owner, repository, resourceURI, user, permissionType.getTypeURI());	//ask for permission for this permission type by URI
	}

	/**Determines whether a given user has permission to perform some action in relation to a given repository and resource.
	This method is additive; if a superclass doesn't find a permission, a subclass may be able to add the permission.
	This implementation allows all permissions if the user is the owner.
	@param owner The principal that owns the repository.
	@param repository The repository that contains the resource.
	@param user The user attempting to access the resource, which may be <code>null</code> if the user is anonymous.
	@param permissionType The type of permission requested, indicated by permission type URI.
	@return A description of the requested permission the given user has in relation to a resource in a repository, or <code>null</code> if the user has no such permissions.
	@exception NullPointerException if the given owner, repository, resource type, and/or permission URI is <code>null</code>.
	*/
	public RDFResource getPermission(final Principal owner, final Repository repository, final URI resourceURI, final Principal user, final URI permissionType)
	{
		if(checkInstance(owner, "Owner cannot be null.").equals(user))	//if the user is the owner
		{
			return new DefaultRDFResource();	//allow the owner to do anything TODO return some shared ALL_PERMISSIONS description
		}
		return null;	//by default don't let a non-owner do anything		
	}
}
