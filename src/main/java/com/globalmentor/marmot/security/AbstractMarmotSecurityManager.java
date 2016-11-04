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

import static java.util.Objects.*;

import org.urframework.*;
import org.urframework.select.*;

import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.net.ResourceIOException;

import static com.globalmentor.java.Objects.*;
import static com.globalmentor.marmot.security.MarmotSecurity.*;

/**
 * Abstract implementation of a security manager for Marmot.
 * @author Garret Wilson
 */
public class AbstractMarmotSecurityManager implements MarmotSecurityManager {

	/**
	 * Determines whether a given permission is valid in the abstract in respect to a resource.
	 * @param repository The repository that contains the resource.
	 * @param resourceURI The URI of the resource to be verified.
	 * @param permissionType The type of permission requested.
	 * @return <code>true</code> if the given permission is valid in relation to the indicated resource, else <code>false</code>.
	 * @throws NullPointerException if the given repository, resource URI, and/or permission type is <code>null</code>.
	 * @throws ResourceIOException if there is an error accessing the repository.
	 */
	protected boolean isPermissionValid(final Repository repository, final URI resourceURI, final PermissionType permissionType) throws ResourceIOException {
		if(repository.getRootURI().equals(resourceURI)) { //if this is the repository URI
			if(permissionType == PermissionType.DELETE || permissionType == PermissionType.RENAME) { //if they are asking to delete or rename the repository
				return false; //the repository cannot be deleted or renamed
			}
		}
		return true; //all other permissions are valid
	}

	//TODO fix	@return <code>true</code> if the given user has the requested permission in relation to a resource in a repository, or <code>false</code> if the user has no such permission.

	@Override
	public boolean isAllowed(final Principal owner, final Repository repository, final URI resourceURI, final Principal user,
			final PermissionType... permissionTypes) throws ResourceIOException {
		return isAllowed(owner, repository, resourceURI, user, false, permissionTypes);
	}

	@Override
	public boolean isAllowed(final Principal owner, final Repository repository, URI resourceURI, final Principal user, boolean checkAncestors,
			final PermissionType... permissionTypes) throws ResourceIOException {
		//Log.trace("getting allowed for permission", permissionType, "for user", user!=null ? user.getName() : "(none)");
		if(permissionTypes.length == 0) {
			throw new IllegalArgumentException("At least one permission type must be specified.");
		}
		do {
			for(int i = permissionTypes.length - 1; i >= 0; --i) { //check all permissions for validity
				final PermissionType permissionType = requireNonNull(permissionTypes[i]);
				if(!isPermissionValid(repository, resourceURI, permissionType)) { //if this permission isn't valid
					return false;
				}
			}
			if(owner.equals(user)) { //if the user is the owner
				return true; //allow the owner to do anything TODO fix the small case in which ancestors are requested and a permission isn't valid for parent resource, even for the owner 
			}
			final Set<PermissionType> allowedPermissionTypes = getAllowedPermissionTypes(repository, resourceURI, user); //get the allowed permission types
			for(final PermissionType permissionType : permissionTypes) {
				if(!allowedPermissionTypes.contains(permissionType)) { //if the allowed permission type does not contain one of the requested permissions
					return false;
				}
			}
		} while(checkAncestors && (resourceURI = repository.getParentResourceURI(resourceURI)) != null); //if we should check ancestors, look at the parent
		return true;
	}

	@Override
	public boolean isOneAllowed(final Principal owner, final Repository repository, final URI resourceURI, final Principal user,
			PermissionType... permissionTypes) throws ResourceIOException {
		if(permissionTypes.length == 0) {
			throw new IllegalArgumentException("At least one permission type must be specified.");
		}
		permissionTypes = permissionTypes.clone(); //clone the permission types so that we can modify them locally
		int permissionTypeCount = permissionTypes.length;
		//weed out invalid requested permissions
		for(int i = permissionTypes.length - 1; i >= 0; --i) { //remove permission types that don't apply
			final PermissionType permissionType = requireNonNull(permissionTypes[i]);
			if(!isPermissionValid(repository, resourceURI, permissionType)) { //if this permission isn't valid
				permissionTypes[i] = null; //don't allow this permission
				--permissionTypeCount; //note that we have one fewer permission to check
			}
		}
		if(permissionTypeCount == 0) { //if no valid permissions are requested, don't check further---at least one valid permission must be requested, even for the owner
			return false;
		}
		if(requireNonNull(owner, "Owner cannot be null.").equals(user)) { //if the user is the owner
			return true; //allow the owner to do anything that is valid
		}
		final Set<PermissionType> allowedPermissionTypes = getAllowedPermissionTypes(repository, resourceURI, user); //get the allowed permission types
		for(final PermissionType permissionType : permissionTypes) {
			if(permissionType != null && allowedPermissionTypes.contains(permissionType)) { //if the allowed permission types contain the requested permission
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isSomethingAllowed(final Principal owner, final Repository repository, final URI resourceURI, final Principal user) throws ResourceIOException {
		if(requireNonNull(owner, "Owner cannot be null.").equals(user)) { //if the user is the owner
			return true; //the owner can always do *something* with the resource
		}
		return !getAllowedPermissionTypes(repository, resourceURI, user).isEmpty(); //see if there is at least one allowed permission for this user
	}

	//	/**
	//	 * Determines whether a given user has permission to perform some action in relation to a given repository and resource. This method is additive; if a
	//	 * superclass doesn't find a permission, a subclass may be able to add the permission. Deleting and renaming the resource repository is never allowed. This
	//	 * implementation allows all permissions in all other circumstances if the user is the owner.
	//	 * @param owner The principal that owns the repository.
	//	 * @param repository The repository that contains the resource.
	//	 * @param user The user attempting to access the resource, which may be <code>null</code> if the user is anonymous.
	//	 * @param permissionTypeURI The type of permission requested, indicated by permission type URI.
	//	 * @return <code>true</code> if the given permission is allowed for the user in relation to the indicated resource, else <code>false</code>.
	//	 * @throws NullPointerException if the given owner, repository, resource type, and/or permission URI is <code>null</code>.
	//	 * @throws ResourceIOException if there is an error accessing the repository.
	//	 */
	/*TODO del if not needed
		public boolean isAllowed(final Principal owner, final Repository repository, final URI resourceURI, final Principal user, final URI permissionTypeURI) throws ResourceIOException
		{
		}
	*/

	//	/**
	//	 * Determines whether a given user has permission to perform some action in relation to a given repository and resource. This implementation allows all
	//	 * permissions if the user is the owner.
	//	 * @param owner The principal that owns the repository.
	//	 * @param repository The repository that contains the resource.
	//	 * @param user The user attempting to access the resource, which may be <code>null</code> if the user is anonymous.
	//	 * @param permissionTypeURI The type of permission requested, indicated by permission type URI.
	//	 * @return A {@link Boolean} value indicating whether this permission was allowed or denied for this user, or <code>null</code> if no access rule was
	//	 *         specified for this user or the resource does not exist.
	//	 * @throws NullPointerException if the given owner, repository, resource type, and/or permission URI is <code>null</code>.
	//	 * @throws ResourceIOException if there is an error accessing the repository.
	//	 */
	/*TODO del if not needed
		protected Boolean getAllowed(final Principal owner, final Repository repository, final URI resourceURI, final Principal user, final URI permissionTypeURI) throws ResourceIOException
		{
	//Log.trace("trying to get allowed for resource", resourceURI);
			if(requireNonNull(owner, "Owner cannot be null.").equals(user)) {	//if the user is the owner
				return Boolean.TRUE;	//allow the owner to do anything
			}
			return allowed;	//return the allowance we found, if any
		}
	*/

	/**
	 * Determines all the permissions allowed a particular user in relation to a given resource.
	 * @param repository The repository that contains the resource.
	 * @param resourceURI The URI of the resource to be verified.
	 * @param user The user attempting to access the resource, which may be <code>null</code> if the user is anonymous.
	 * @return A set of all permission types allowed the user in relation to the given resource.
	 * @throws NullPointerException if the given repository and/or resource URI is <code>null</code>.
	 * @throws ResourceIOException if there is an error accessing the repository.
	 */
	public Set<PermissionType> getAllowedPermissionTypes(final Repository repository, final URI resourceURI, final Principal user) throws ResourceIOException {
		//Log.trace("trying to get allowed permissions for resource", resourceURI, "with user", user!=null ? user.getName() : "(none)");
		if(requireNonNull(repository, "Repository cannot be null.").resourceExists(requireNonNull(resourceURI, "Resource URI cannot be null."))) { //see if the resource exists; if not, consider it to have inherited access
			final URFResource resource = repository.getResourceDescription(resourceURI); //get the resource description
			final Access access = asInstance(resource.getPropertyValue(ACCESS_PROPERTY_URI), Access.class).orElse(null); //get the security.access property value, if any
			if(access != null) { //if we have access defined
				for(final AccessRule accessRule : access) { //for each access rule
					final Selector selector = accessRule.getSelector(); //get the selector, if any
					//Log.trace("trying selector", RDFUtilities.toString(selector));
					if(selector != null && selector.selects(user)) { //if this access rule's selector applies to this user
						final AccessLevel accessLevel = accessRule.getAccessLevel(); //get the access level
						//Log.trace("this selector matches; access level is", accessLevel);
						if(accessLevel != null) { //if there is an access level
							final AccessLevelType accessLevelType = accessLevel.getAccessLevelType(); //get the access level type
							if(accessLevelType == AccessLevelType.INHERITED) { //if this principal specifically should get inherited access
								break; //stop looking for other access rules and get the inherited access
							} else if(accessLevelType == AccessLevelType.CUSTOM) { //if this is a custom access level
								final Set<PermissionType> permissionTypes = EnumSet.noneOf(PermissionType.class); //create a set of permission types
								//allow
								for(final Permission allowPermission : accessLevel.getAllows()) { //for each allowed permission
									permissionTypes.add(allowPermission.getPermissionType()); //add this permission type
								}
								//deny
								for(final Permission denyPermission : accessLevel.getAllows()) { //for each denied permission
									permissionTypes.add(denyPermission.getPermissionType()); //remove this permission type
								}
								return permissionTypes; //return the custom permission types
							} else { //for all other access levels
								return accessLevelType.getDefaultAllowedPermissionTypes(); //return the default permission types of this access level
							}
						} else { //if there is no access level defined, deny all permissions
							return EnumSet.noneOf(PermissionType.class); //there are no permissions, because there is no access level
						}
					}
				}
			}
		}
		//if this principal's access level is inherited
		final URI parentResourceURI = repository.getParentResourceURI(resourceURI); //get the URI of the parent
		if(parentResourceURI != null) { //if there is a parent resource
			final Set<PermissionType> permissionTypes = EnumSet.copyOf(getAllowedPermissionTypes(repository, parentResourceURI, user)); //get a copy of the parent permissions for this principal 
			if(permissionTypes.contains(PermissionType.BROWSE)) { //if the inherited permissions allow browsing
				permissionTypes.add(PermissionType.DISCOVER); //impute discovery
			}
			if(permissionTypes.contains(PermissionType.SUBTRACT)) { //if the inherited permissions allow subtracting
				permissionTypes.add(PermissionType.DELETE); //impute deletion
			}
			return permissionTypes; //return our permission types, along with the appropriate imputed permissions
		} else { //if there is no parent
			return EnumSet.noneOf(PermissionType.class); //don't return any permissions
		}
	}

}
