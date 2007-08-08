package com.globalmentor.marmot.security;

import java.net.URI;
import java.security.Principal;
import java.util.EnumSet;
import java.util.Set;

import static com.garretwilson.lang.ObjectUtilities.*;
import static com.garretwilson.rdf.RDFUtilities.*;
import static com.globalmentor.marmot.MarmotConstants.*;

import com.garretwilson.net.ResourceIOException;
import com.garretwilson.rdf.*;

import com.garretwilson.util.Debug;
import com.globalmentor.marmot.*;
import com.globalmentor.marmot.repository.Repository;

/**Abstract implementation of a security manager for Marmot.
@author Garret Wilson
*/
public class AbstractMarmotSecurityManager implements MarmotSecurityManager
{

//TODO fix	@return <code>true</code> if the given user has the requested permission in relation to a resource in a repository, or <code>false</code> if the user has no such permission.

	/**The URI for the property selector type.*/
	protected final static URI PROPERTY_SELECTOR_TYPE_URI=createReferenceURI(MARMOT_NAMESPACE_URI, PROPERTY_SELECTOR_TYPE_NAME);
	/**The URI for the union selector type.*/
	protected final static URI UNION_SELECTOR_TYPE_URI=createReferenceURI(MARMOT_NAMESPACE_URI, UNION_SELECTOR_TYPE_NAME);
	/**The URI for the universal selector type.*/
	protected final static URI UNIVERSAL_SELECTOR_TYPE_URI=createReferenceURI(MARMOT_NAMESPACE_URI, UNIVERSAL_SELECTOR_TYPE_NAME);
	/**The URI for the URI selector type.*/
	protected final static URI URI_SELECTOR_TYPE_URI=createReferenceURI(MARMOT_NAMESPACE_URI, URI_SELECTOR_TYPE_NAME);

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
			return Boolean.TRUE;	//allow the owner to do anything
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
	protected Set<PermissionType> getAllowedPermissionTypes(final Repository repository, final URI resourceURI, final Principal user) throws ResourceIOException
	{
//Debug.trace("trying to get allowed permissions for resource", resourceURI, "with user", user!=null ? user.getName() : "(none)");
		if(checkInstance(repository, "Repository cannot be null.").resourceExists(checkInstance(resourceURI, "Resource URI cannot be null.")))	//see if the resource exists; if not, consider it to have inherited access
		{
			final RDFResource resource=repository.getResourceDescription(resourceURI);	//get the resource description
			final Security security=(Security)resource.getPropertyValue(MARMOT_NAMESPACE_URI, SECURITY_PROPERTY_NAME);	//get the marmot:security property value, if any
			if(security!=null)	//if we have security information defined
			{
				final Access access=security.getAccess();	//get the marmot:access property value, if any
//Debug.trace("got access resource:", RDFUtilities.toString(access));
				if(access!=null)	//if we have access defined
				{
					final RDFListResource accessRules=access.getAccessRules();	//get the list of access rules
					if(accessRules!=null)	//if there are access rules
					{
//Debug.trace("got access rules of size", accessRules.size());
						for(final RDFObject accessRuleObject:accessRules)	//for each access rule
						{
							final AccessRule accessRule=(AccessRule)accessRuleObject;	//TODO fix cast; RDFLiteral will probably eventually be subordinated to RDFResource
							final Selector selector=accessRule.getSelect();	//get the selector, if any
//Debug.trace("trying selector", RDFUtilities.toString(selector));
							if(selector!=null && (user==null || user instanceof RDFResource) && selector.selects((RDFResource)user))	//if this access rule's selector applies to this user
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
