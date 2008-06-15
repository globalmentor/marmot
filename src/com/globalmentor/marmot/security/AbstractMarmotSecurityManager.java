/*
 * Copyright © 1996-2008 GlobalMentor, Inc. <http://www.globalmentor.com/>
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
import java.util.*;


import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.net.ResourceIOException;
import com.globalmentor.urf.*;
import com.globalmentor.urf.select.*;

import static com.globalmentor.java.Objects.*;
import static com.globalmentor.marmot.security.MarmotSecurity.*;

/**Abstract implementation of a security manager for Marmot.
@author Garret Wilson
*/
public class AbstractMarmotSecurityManager implements MarmotSecurityManager
{

//TODO fix	@return <code>true</code> if the given user has the requested permission in relation to a resource in a repository, or <code>false</code> if the user has no such permission.

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
	public boolean isAllowed(final Principal owner, final Repository repository, final URI resourceURI, final Principal user, final PermissionType permissionType) throws ResourceIOException
	{
//Debug.trace("getting allowed for permission", permissionType, "for user", user!=null ? user.getName() : "(none)");
		if(repository.getPublicRepositoryURI().equals(resourceURI))	//if this is the repository URI
		{
			if(permissionType==PermissionType.DELETE || permissionType==PermissionType.RENAME)	//if they are asking to delete or rename the repository
			{
				return false;	//the repository cannot be deleted or renamed
			}
		}
		if(checkInstance(owner, "Owner cannot be null.").equals(user))	//if the user is the owner
		{
			return true;	//allow the owner to do anything
		}
		return getAllowedPermissionTypes(repository, resourceURI, user).contains(permissionType);	//see if the allowed permission types contain the requested permission
	}

	/**Determines whether a given user has permission to perform some action in relation to a given repository and resource.
	This method is additive; if a superclass doesn't find a permission, a subclass may be able to add the permission.
	Deleting and renaming the resource repository is never allowed.
	This implementation allows all permissions in all other circumstances if the user is the owner.
	@param owner The principal that owns the repository.
	@param repository The repository that contains the resource.
	@param user The user attempting to access the resource, which may be <code>null</code> if the user is anonymous.
	@param permissionTypeURI The type of permission requested, indicated by permission type URI.
	@return <code>true</code> if the given permission is allowed for the user in relation to the indicated resource, else <code>false</code>.
	@exception NullPointerException if the given owner, repository, resource type, and/or permission URI is <code>null</code>.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
/*TODO del if not needed
	public boolean isAllowed(final Principal owner, final Repository repository, final URI resourceURI, final Principal user, final URI permissionTypeURI) throws ResourceIOException
	{
	}
*/

	/**Determines whether a given user has permission to perform some action in relation to a given repository and resource.
	This implementation allows all permissions if the user is the owner.
	@param owner The principal that owns the repository.
	@param repository The repository that contains the resource.
	@param user The user attempting to access the resource, which may be <code>null</code> if the user is anonymous.
	@param permissionTypeURI The type of permission requested, indicated by permission type URI.
	@return A {@link Boolean} value indicating whether this permission was allowed or denied for this user, or <code>null</code> if no access rule was specified for this user or the resource does not exist.
	@exception NullPointerException if the given owner, repository, resource type, and/or permission URI is <code>null</code>.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
/*TODO del if not needed
	protected Boolean getAllowed(final Principal owner, final Repository repository, final URI resourceURI, final Principal user, final URI permissionTypeURI) throws ResourceIOException
	{
//Debug.trace("trying to get allowed for resource", resourceURI);
		if(checkInstance(owner, "Owner cannot be null.").equals(user))	//if the user is the owner
		{
			return Boolean.TRUE;	//allow the owner to do anything
		}
		return allowed;	//return the allowance we found, if any
	}
*/
	
	/**Determines all the permissions allowed a particular user in relation to a given resource.
	@param repository The repository that contains the resource.
	@param user The user attempting to access the resource, which may be <code>null</code> if the user is anonymous.
	@return A set of all permission types allowed the user in relation to the given resoruce.
	@exception NullPointerException if the given repository and/or resource URI is <code>null</code>.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public Set<PermissionType> getAllowedPermissionTypes(final Repository repository, final URI resourceURI, final Principal user) throws ResourceIOException
	{
//Debug.trace("trying to get allowed permissions for resource", resourceURI, "with user", user!=null ? user.getName() : "(none)");
		if(checkInstance(repository, "Repository cannot be null.").resourceExists(checkInstance(resourceURI, "Resource URI cannot be null.")))	//see if the resource exists; if not, consider it to have inherited access
		{
			final URFResource resource=repository.getResourceDescription(resourceURI);	//get the resource description
			final Access access=asInstance(resource.getPropertyValue(ACCESS_PROPERTY_URI), Access.class);	//get the security.access property value, if any
			if(access!=null)	//if we have access defined
			{
				for(final AccessRule accessRule:access)	//for each access rule
				{
					final Selector selector=accessRule.getSelector();	//get the selector, if any
//Debug.trace("trying selector", RDFUtilities.toString(selector));
					if(selector!=null && selector.selects(user))	//if this access rule's selector applies to this user
					{
						final AccessLevel accessLevel=accessRule.getAccessLevel();	//get the access level
//Debug.trace("this selector matches; access level is", accessLevel);
						if(accessLevel!=null)	//if there is an access level
						{
							final AccessLevelType accessLevelType=accessLevel.getAccessLevelType();	//get the access level type
							if(accessLevelType==AccessLevelType.INHERITED)	//if this principal specifically should get inherited access
							{
								break;	//stop looking for other access rules and get the inherited access
							}
							else if(accessLevelType==AccessLevelType.CUSTOM)	//if this is a custom access level
							{
								final Set<PermissionType> permissionTypes=EnumSet.noneOf(PermissionType.class);	//create a set of permission types
									//allow
								for(final Permission allowPermission:accessLevel.getAllows())	//for each allowed permission
								{
									permissionTypes.add(allowPermission.getPermissionType());	//add this permission type
								}
									//deny
								for(final Permission denyPermission:accessLevel.getAllows())	//for each denied permission
								{
									permissionTypes.add(denyPermission.getPermissionType());	//remove this permission type
								}
								return permissionTypes;	//return the custom permission types
							}
							else	//for all other access levels
							{
								return accessLevelType.getDefaultAllowedPermissionTypes();	//return the default permission types of this access level
							}
						}
						else	//if there is no access level defined, deny all permissions
						{
							return EnumSet.noneOf(PermissionType.class);	//there are no permissions, because there is no access level
						}
					}
				}
			}
		}
			//if this principal's access level is inherited
		final URI parentResourceURI=repository.getParentResourceURI(resourceURI);	//get the URI of the parent
		if(parentResourceURI!=null)	//if there is a parent resource
		{
			final Set<PermissionType> permissionTypes=EnumSet.copyOf(getAllowedPermissionTypes(repository, parentResourceURI, user));	//get a copy of the parent permissions for this principal 
			if(permissionTypes.contains(PermissionType.BROWSE))	//if the inherited permissions allow browsing
			{
				permissionTypes.add(PermissionType.DISCOVER);	//impute discovery
			}
			if(permissionTypes.contains(PermissionType.SUBTRACT))	//if the inherited permissions allow subtracting
			{
				permissionTypes.add(PermissionType.DELETE);	//impute deletion
			}
			return permissionTypes;	//return our permission types, allong with the appropriate imputed permissions
		}
		else	//if there is no parent
		{
			return EnumSet.noneOf(PermissionType.class);	//don't return any permissions
		}
	}

}
