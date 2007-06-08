package com.globalmentor.marmot.security;

import java.net.URI;
import java.security.Principal;

import static com.garretwilson.lang.ObjectUtilities.*;
import static com.garretwilson.rdf.RDFUtilities.*;
import static com.globalmentor.marmot.MarmotConstants.*;

import com.garretwilson.net.Resource;
import com.garretwilson.net.ResourceIOException;
import static com.garretwilson.net.URIConstants.*;
import com.garretwilson.rdf.*;

import static com.garretwilson.rdf.directory.vcard.VCard.*;

import com.garretwilson.util.Debug;
import com.globalmentor.marmot.MarmotConstants;
import com.globalmentor.marmot.repository.Repository;

/**Abstract implementation of a security manager for Marmot.
@author Garret Wilson
*/
public class AbstractMarmotSecurityManager implements MarmotSecurityManager
{

//TODO fix	@return <code>true</code> if the given user has the requested permission in relation to a resource in a repository, or <code>false</code> if the user has no such permission.

	/**The URI for the browse permission type.*/
	protected final static URI BROWSE_PERMISSION_TYPE_URI=createReferenceURI(MARMOT_NAMESPACE_URI, BROWSE_PERMISSION_TYPE_NAME);
	/**The URI for the delete permission type.*/
	protected final static URI DELETE_PERMISSION_TYPE_URI=createReferenceURI(MARMOT_NAMESPACE_URI, DELETE_PERMISSION_TYPE_NAME);
	/**The URI for the discover permission type.*/
	protected final static URI DISCOVER_PERMISSION_TYPE_URI=createReferenceURI(MARMOT_NAMESPACE_URI, DISCOVER_PERMISSION_TYPE_NAME);
	/**The URI for the rename permission type.*/
	protected final static URI RENAME_PERMISSION_TYPE_URI=createReferenceURI(MARMOT_NAMESPACE_URI, RENAME_PERMISSION_TYPE_NAME);
	/**The URI for the write permission type.*/
	protected final static URI WRITE_PERMISSION_TYPE_URI=createReferenceURI(MARMOT_NAMESPACE_URI, WRITE_PERMISSION_TYPE_NAME);

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
		return isAllowed(owner, repository, resourceURI, user, permissionType.getTypeURI());	//ask for permission for this permission type by URI
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
	public boolean isAllowed(final Principal owner, final Repository repository, final URI resourceURI, final Principal user, final URI permissionTypeURI) throws ResourceIOException
	{
		if(repository.getPublicRepositoryURI().equals(resourceURI))	//if this is the repository URI
		{
			if(DELETE_PERMISSION_TYPE_URI.equals(permissionTypeURI) || RENAME_PERMISSION_TYPE_URI.equals(permissionTypeURI))	//if they are asking to delete or rename the repository
			{
				return false;	//the repository cannot be deleted or renamed
			}
			//TODO add rename permission check
		}
		final Boolean allowed=getAllowed(owner, repository, resourceURI, user, permissionTypeURI);	//get the allowance, if any
		if(allowed!=null)	//if a permission was explicitly specified
		{
			return allowed.booleanValue();	//return the allowed value
		}
			//check inherited values
		final URI parentResourceURI=repository.getParentResourceURI(resourceURI);	//get the URI of the parent
		if(parentResourceURI!=null)	//if there is a parent resource
		{
			if(isAllowed(owner, repository, parentResourceURI, user, permissionTypeURI))	//if the parent allows this permission
			{
				return true;	//this resource inherits the parent's allowance
			}
			if(DISCOVER_PERMISSION_TYPE_URI.equals(permissionTypeURI))	//if they specified one of the browse-imputed permissions
			{
				return isAllowed(owner, repository, parentResourceURI, user, PermissionType.BROWSE);	//see if a parent has browse permission
			}
			else if(DELETE_PERMISSION_TYPE_URI.equals(permissionTypeURI))	//if they specified one of the write-imputed permissions
			{
				return isAllowed(owner, repository, parentResourceURI, user, PermissionType.WRITE);	//see if a parent has write permission
			}
		}
		return false;	//if there is no parent, inherited access defaults to false		
/*TODO del when new method works
Debug.trace("checking permission", permissionTypeURI, "for resource", resourceURI);
		Boolean allowed;	//we'll look for allowance up the hierarcy
		URI checkResourceURI=resourceURI;	//we'll look up the chain for a resource URI
		do
		{
Debug.trace("ready to get allowed for resource", checkResourceURI);
			allowed=getAllowed(owner, repository, checkResourceURI, user, permissionTypeURI);	//get the allowance, if any
Debug.trace("got allowed for resource", checkResourceURI, allowed);
			if(allowed!=null)	//if we found an allowance
			{
				break;	//stop looking
			}
			else	//if we didn't find an allowance
			{
				checkResourceURI=repository.getParentResourceURI(checkResourceURI);	//get the URI of the parent
			}
		}
		while(checkResourceURI!=null);	//keep checking up the chain until we run out of resources
		if(allowed==null)	//if no allowance was specified for this user, check for imputed access
		{
Debug.trace("no allowance found; need to check for imputed permissions");

			if(DISCOVER_PERMISSION_TYPE_URI.equals(permissionTypeURI))	//if they specified one of the browse-imputed permissions
			{
Debug.trace("this is a discover permission");
				final URI parentResourceURI=repository.getParentResourceURI(resourceURI);	//get the URI of this resource's parent
Debug.trace("parent is", parentResourceURI, "checking for browse permission");
				allowed=Boolean.valueOf(parentResourceURI!=null && isAllowed(owner, repository, parentResourceURI, user, PermissionType.BROWSE));	//see if a parent has browse permission
Debug.trace("new allowed:", allowed);
			}
			else if(DELETE_PERMISSION_TYPE_URI.equals(permissionTypeURI))	//if they specified one of the write-imputed permissions
			{
				final URI parentResourceURI=repository.getParentResourceURI(resourceURI);	//get the URI of this resource's parent
				allowed=Boolean.valueOf(parentResourceURI!=null && isAllowed(owner, repository, parentResourceURI, user, PermissionType.WRITE));	//see if a parent has write permission
			}
			else	//if they specified any other permission
			{
				allowed=Boolean.FALSE;	//we'll assume no allowance				
			}
		}
		assert allowed!=null : "Failed to determine allowance.";
		return allowed.booleanValue();	//return the allowance we determined
*/
	}

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
	protected Boolean getAllowed(final Principal owner, final Repository repository, final URI resourceURI, final Principal user, final URI permissionTypeURI) throws ResourceIOException
	{
//Debug.trace("trying to get allowed for resource", resourceURI);
		if(checkInstance(owner, "Owner cannot be null.").equals(user))	//if the user is the owner
		{
			return true;	//allow the owner to do anything
		}
		Boolean allowed=null;	//we don't know whether this permission is allowed or not
		if(checkInstance(repository, "Repository cannot be null.").resourceExists(checkInstance(resourceURI, "Resource URI cannot be null.")))	//see if the resource exists; if not, consider it to have inherited access
		{
			final RDFResource resource=repository.getResourceDescription(resourceURI);
			final RDFResource accessResource=asResource(resource.getPropertyValue(MARMOT_NAMESPACE_URI, ACCESS_PROPERTY_NAME));	//get the marmot:access property value, if any
//	Debug.trace("got access resource:", accessResource);
			if(accessResource!=null)	//if we have access permissions defined
			{
				final RDFResource accessTypeResource=getType(accessResource);	//get the type of access
				final URI accessTypeURI=accessTypeResource.getReferenceURI();	//get the access type URI
//	Debug.trace("access type URI:", accessTypeURI);
				if(accessTypeURI!=null)	//if we know the access type URI
				{
					final AccessType accessType=AccessType.getAccessType(accessTypeURI);	//see what access type is indicated
					if(accessType!=null)	//if we recognize the access type, it's a premade access type that applies to all users
					{
//	Debug.trace("access type:", accessType);
						final PermissionType permissionType=PermissionType.getPermissionType(permissionTypeURI);	//see if there was a known permission type requested
						if(permissionType!=null)	//if a known permission type was requested
						{
//	Debug.trace("permission type:", permissionType);
							allowed=Boolean.valueOf(accessType.getDefaultAllowedPermissionTypes().contains(permissionType));	//indicate whether this permission is allowed by default; either way, this user was specified
//	Debug.trace("new allowed:", allowed);
						}
					}
				}
				else	//if we don't know the access type, it must be a custom access type; see what it says about users TODO verify that this is a custom access type
				{
					
				}
//	Debug.trace("now ready to check access rules");
					//see if access rules change the given allowance, if any, that we have
				final RDFListResource accessRules=asListResource(accessResource);	//get the list of access rules
				if(accessRules!=null)	//if there are access rules
				{
//	Debug.trace("got access rules of size", accessRules.size());
					for(final RDFResource accessRule:accessRules)	//for each access rule
					{
						for(final RDFObject principalObject:accessRule.getPropertyValues(MARMOT_NAMESPACE_URI, PRINCIPAL_PROPERTY_NAME))	//look at all principals defined for this rule
						{
							if(principalObject instanceof RDFResource)	//if a principal resource specified for this rule
							{
								final URI principalURI=((RDFResource)principalObject).getReferenceURI();	//get the principal reference URI
								if(principalURI!=null && isPrincipalMatch(principalURI, user))	//if the current user is matched
								{
									if(allowed==null)	//if no allowance has been specified
									{
										allowed=Boolean.FALSE;	//assume the permission is not allowed
									}
										//allow
									for(final RDFObject allowObject:accessRule.getPropertyValues(MARMOT_NAMESPACE_URI, ALLOW_TYPE_NAME))	//for each access rule property
									{
										if(allowObject instanceof RDFResource)	//if the permission is a resource
										{
											final RDFResource allowResource=(RDFResource)allowObject;	//get the permission as a resource
											if(permissionTypeURI.equals(allowResource.getReferenceURI()))	//if this permission is allowed
											{
												allowed=Boolean.TRUE;	//the permission was allowed
												break;	//stop looking for an allowance
											}
										}
									}
										//TODO check deny
								}
							}
						}
					}
				}
			}
		}
//Debug.trace("ready to return allowed", allowed);
		return allowed;	//return the allowance we found, if any
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
	@exception ResourceIOException if there is an error accessing the repository.
	*/
/*TODO fix or del
	public RDFResource getPermission(final Principal owner, final Repository repository, final URI resourceURI, final Principal user, final URI permissionType) throws ResourceIOException
	{
		if(checkInstance(owner, "Owner cannot be null.").equals(user))	//if the user is the owner
		{
			return new DefaultRDFResource();	//allow the owner to do anything TODO return some shared ALL_PERMISSIONS description
		}
		final RDFResource resource=checkInstance(repository, "Repository cannot be null.").getResourceDescription(checkInstance(resourceURI, "Resource URI cannot be null."));
		final RDFResource accessResource=asResource(resource.getPropertyValue(MARMOT_NAMESPACE_URI, ACCESS_PROPERTY_NAME));	//get the marmot:access property value, if any
		if(accessResource!=null)	//if we have access permissions defined
		{
			
			Boolean allowed=null;	//we don't know whether this permission is allowed or not
			RDFResource permissionResource=null;	//we haven't found any permission, yet

			final RDFResource accessTypeResource=getType(accessResource);	//get the type of access
			final URI accessTypeURI=accessTypeResource.getReferenceURI();	//get the access type URI
			if(accessTypeURI!=null)	//if we know the access type URI
			{
				final AccessType accessType=AccessType.getAccessType(accessTypeURI);	//see what access type is indicated
				if(accessType!=null)	//if we recognize the access type, it's a premade access type that applies to all users
				{
					if(accessType.getDefaultAllowedPermissionTypes().contains(permissionType))	//if this access type allows this permission type
					{
						allowed=Boolean.TRUE;	//indicate that this permission is allowed by default
					}
				}
			}
			else	//if we don't know the access type, it must be a custom access type; see what it says about users TODO verify that this is a custom access type
			{
				
			}
				//see if access rules change the given allowance, if any, that we have
			final RDFListResource accessRules=asListResource(accessResource);	//get the list of access rules
			if(accessRules!=null)	//if there are access rules
			{
				for(final RDFResource accessRule:accessRules)	//for each access rule
				{
					for(final RDFObject principalObject:accessRule.getPropertyValues(MARMOT_NAMESPACE_URI, PRINCIPAL_PROPERTY_NAME))	//look at all principals defined for this rule
					{
						if(principalObject instanceof RDFResource)	//if a principal resource specified for this rule
						{
							final URI principalURI=((RDFResource)principalObject).getReferenceURI();	//get the principal reference URI
							if(principalURI!=null && isPrincipalMatch(principalURI, user))	//if the current user is matched
							{
								if(allowed==null)	//if no allowance has been specified
								{
									allowed=Boolean.FALSE;	//assume the permission is not allowed
								}
									//allow
								for(final RDFObject allowObject:accessRule.getPropertyValues(MARMOT_NAMESPACE_URI, ALLOW_TYPE_NAME))	//for each access rule property
								{
									if(allowObject instanceof RDFResource)	//if the permission is a resource
									{
										final RDFResource allowResource=(RDFResource)allowObject;	//get the permission as a resource
										if(permissionType.equals(allowResource.getReferenceURI()))	//if this permission is allowed
										{
											allowed=Boolean.TRUE;	//the permission was allowed
											permissionResource=allowResource;	//save the permission allowed
											break;	//stop looking for an allowance
										}
									}
								}
									//TODO check deny
							}
						}
					}
				}
			}
			if(allowed==null)	//if no permission was specified for this user
			{
				final URI parentResourceURI=repository.getParentResourceURI(resourceURI);	//get the URI of the parent
				if(parentResourceURI!=null)	//if there is a parent resource
				{
					allowed=
				}
			}
			
		}
		
		
		return null;	//by default don't let a non-owner do anything		
	}
*/



	/**Determines whether a given user has permission to perform some action in relation to a given repository and resource based upon a known access description.
	This implementation does not yet support custom access types.
	@param owner The principal that owns the repository.
	@param repository The repository that contains the resource.
	@param user The user attempting to access the resource, which may be <code>null</code> if the user is anonymous.
	@param accessResource The description of access rights.
	@param permissionType The type of permission requested, indicated by permission type URI.
	@return A description of the requested permission the given user has in relation to a resource in a repository, or <code>null</code> if the user has no such permissions.
	@exception NullPointerException if the given owner, repository, resource type, access description, and/or permission URI is <code>null</code>.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
/*TODO fix or del
	protected Boolean getAllowed(final Principal owner, final Repository repository, final URI resourceURI, final Principal user, final RDFResource accessResource, final URI permissionType) throws ResourceIOException
	{
		checkInstance(accessResource, "Access resource cannot be null.");

		Boolean allowed=null;	//we don't know whether this permission is allowed or not
		
		final RDFResource accessTypeResource=getType(accessResource);	//get the type of access
		final URI accessTypeURI=accessTypeResource.getReferenceURI();	//get the access type URI
		if(accessTypeURI!=null)	//if we know the access type URI
		{
			final AccessType accessType=AccessType.getAccessType(accessTypeURI);	//see what access type is indicated
			if(accessType!=null)	//if we recognize the access type, it's a premade access type that applies to all users
			{
				if(accessType.getDefaultAllowedPermissionTypes().contains(permissionType))	//if this access type allows this permission type
				{
					allowed=Boolean.TRUE;	//indicate that this permission is allowed by default
				}
			}
		}
		else	//if we don't know the access type, it must be a custom access type; see what it says about users TODO verify that this is a custom access type
		{
			
		}
			//see if access rules change the given allowance, if any, that we have
		final RDFListResource accessRules=asListResource(accessResource);	//get the list of access rules
		if(accessRules!=null)	//if there are access rules
		{
			for(final RDFResource accessRule:accessRules)	//for each access rule
			{
				for(final RDFObject principalObject:accessRule.getPropertyValues(MARMOT_NAMESPACE_URI, PRINCIPAL_PROPERTY_NAME))	//look at all principals defined for this rule
				{
					if(principalObject instanceof RDFResource)	//if a principal resource specified for this rule
					{
						final URI principalURI=((RDFResource)principalObject).getReferenceURI();	//get the principal reference URI
						if(principalURI!=null && isPrincipalMatch(principalURI, user))	//if the current user is matched
						{
							if(allowed==null)	//if no allowance has been specified
							{
								allowed=Boolean.FALSE;	//assume the permission is not allowed
							}
								//allow
							for(final RDFObject allowObject:accessRule.getPropertyValues(MARMOT_NAMESPACE_URI, ALLOW_TYPE_NAME))	//for each access rule property
							{
								if(allowObject instanceof RDFResource && permissionType.equals(((RDFResource)allowObject).getReferenceURI()))	//if this permission is allowed
								{
									allowed=Boolean.TRUE;	//the permission was allowed
									break;	//stop looking for an allowance
								}
							}
								//TODO check deny
						}
					}
				}
			}
		}
		if(allowed==null)	//if no permission was specified for this user
		{
			final URI parentResourceURI=repository.getParentResourceURI(resourceURI);	//get the URI of the parent
			if(parentResourceURI!=null)	//if there is a parent resource
			{
				allowed=
			}
		}
	}
*/

	/**Determines whether the given principal URI matches the given principal.
	A match is found in all of the following situations:
	<ul>
		<li>If the URI is the global wildcard {@value MarmotConstants#ALL_PRINCIPALS_URI}.</li>
		<li>If the principal is a {@link Resource} and its reference URI is equal to the given URI.</li>
		<li>If the given URI is a <code>mailto:</code> URI; and the principal is an {@link RDFResource} with a <code>vcard:email</code> property with a value resource that has a reference URI equal to the given URI.</li>
	</ul>
	This implementation only recognizes one wildcard URI, <code>mailto:*@*</code>.
	@param uri The URI against which to match the principal, which may contain a wildcard user and/or wildcard domain.
	@param principal The principal to check for a match.
	@exception NullPointerException if the given principal URI and/or principal is <code>null</code>.
	*/ 
	protected boolean isPrincipalMatch(final URI uri, final Principal principal)	//TODO add check for *@localhost, equal to the principal name
	{
		checkInstance(principal, "Principal cannot be null.");
		if(checkInstance(uri, "URI cannot be null").equals(ALL_PRINCIPALS_URI))	//if this is the global wildcard URI
		{
			return true;	//all principals match the global wildcard
		}
		if(principal instanceof Resource)	//if the principal is a resource
		{
			final URI principalURI=((Resource)principal).getReferenceURI();	//get the reference URI of the principal
			if(uri.equals(principalURI))	//if te principal URI matches the given URI
			{
				return true;	//indicate that the the principal has the given URI
			}
			if(MAILTO_SCHEME.equals(uri.getScheme()))	//if a mailto: URI was given, check email addresses
			{
				if(principal instanceof RDFResource)	//if the principal is an RDF resource
				{
					for(final RDFObject emailObject:((RDFResource)principal).getPropertyValues(VCARD_NAMESPACE_URI, EMAIL_PROPERTY_NAME))	//for each email(s) designated
					{
						if(emailObject instanceof RDFListResource)	//if the email is a list
						{
							for(final RDFResource emailResource:(RDFListResource)emailObject)	//for each email
							{
								if(uri.equals(emailResource.getReferenceURI()))	//if this email has the same mailto: URI
								{
									return true;	//we found a matching email
								}
							}
						}
						else if(emailObject instanceof RDFResource && uri.equals(((RDFResource)emailObject).getReferenceURI()))	//if this the email is a resource with the same mailto: URI
						{
							return true;	//we found a matching email
						}
					}
				}
			}
		}
		return false;	//indicate that the principal doesn't match
	}

}
