/*
 * Copyright © 1996-2011 GlobalMentor, Inc. <http://www.globalmentor.com/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.globalmentor.marmot.security;

import java.net.URI;
import java.security.Principal;

import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.net.ResourceIOException;

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
	@param permissionTypes The types of permission requested.
	@return <code>true</code> if all of the given permissions are allowed for the user in relation to the indicated resource, else <code>false</code>.
	@throws NullPointerException if the given owner, repository, resource URI, and/or permission type is <code>null</code>.
	@throws IllegalArgumentException if no permission types are given. 
	@throws ResourceIOException if there is an error accessing the repository.
	*/
	public boolean isAllowed(final Principal owner, final Repository repository, final URI resourceURI, final Principal user, final PermissionType... permissionTypes) throws ResourceIOException;

	/**Determines whether a given user has permission to perform some action in relation to a given repository and resource.
	This method is additive; if a superclass doesn't find a permission, a subclass may be able to add the permission.
	@param owner The principal that owns the repository.
	@param repository The repository that contains the resource.
	@param user The user attempting to access the resource, which may be <code>null</code> if the user is anonymous.
	@param checkAncestors Whether all the resource's ancestors are required to have the indicated permissions as well.
	@param permissionTypes The types of permission requested.
	@return <code>true</code> if all of the given permissions are allowed for the user in relation to the indicated resource, else <code>false</code>.
	@throws NullPointerException if the given owner, repository, resource URI, and/or permission type is <code>null</code>.
	@throws IllegalArgumentException if no permission types are given. 
	@throws ResourceIOException if there is an error accessing the repository.
	*/
	public boolean isAllowed(final Principal owner, final Repository repository, final URI resourceURI, final Principal user, final boolean checkAncestors, final PermissionType... permissionTypes) throws ResourceIOException;
	
	/**Determines whether a given user has at least one permission to perform some action in relation to a given repository and resource.
	This method is additive; if a superclass doesn't find a permission, a subclass may be able to add the permission.
	@param owner The principal that owns the repository.
	@param repository The repository that contains the resource.
	@param user The user attempting to access the resource, which may be <code>null</code> if the user is anonymous.
	@param permissionTypes The type of permission requested, one of which must be allowed.
	@return <code>true</code> if one of the given permissions is allowed for the user in relation to the indicated resource, else <code>false</code>.
	@throws NullPointerException if the given owner, repository, resource URI, permission types, and/or a permissions type is <code>null</code>.
	@throws IllegalArgumentException if no permission types are given. 
	@throws ResourceIOException if there is an error accessing the repository.
	*/
	public boolean isOneAllowed(final Principal owner, final Repository repository, final URI resourceURI, final Principal user, final PermissionType... permissionTypes) throws ResourceIOException;

	/**Determines whether a given user has some permission to perform some action in relation to a given repository and resource.
	This method is additive; if a superclass doesn't find a permission, a subclass may be able to add the permission.
	@param owner The principal that owns the repository.
	@param repository The repository that contains the resource.
	@param user The user attempting to access the resource, which may be <code>null</code> if the user is anonymous.
	@return <code>true</code> if the some permission is allowed for the user in relation to the indicated resource, else <code>false</code>.
	@throws NullPointerException if the given owner, repository, and/or resource URI is <code>null</code>.
	@throws ResourceIOException if there is an error accessing the repository.
	*/
	public boolean isSomethingAllowed(final Principal owner, final Repository repository, final URI resourceURI, final Principal user) throws ResourceIOException;

	/**Determines whether a given user has permission to perform some action in relation to a given repository and resource.
	This method is additive; if a superclass doesn't find a permission, a subclass may be able to add the permission.
	This implementation allows all permissions if the user is the owner.
	@param owner The principal that owns the repository.
	@param repository The repository that contains the resource.
	@param user The user attempting to access the resource, which may be <code>null</code> if the user is anonymous.
	@param permissionTypeURI The type of permission requested, indicated by permission type URI.
	@return <code>true</code> if the given permission is allowed for the user in relation to the indicated resource, else <code>false</code>.
	@throws NullPointerException if the given owner, repository, resource type, and/or permission URI is <code>null</code>.
	@throws ResourceIOException if there is an error accessing the repository.
	*/
//TODO del if nto needed	public boolean isAllowed(final Principal owner, final Repository repository, final URI resourceURI, final Principal user, final URI permissionTypeURI) throws ResourceIOException;

}
