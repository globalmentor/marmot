package com.globalmentor.marmot.security;

import java.util.Set;

import com.globalmentor.urf.URFResource;

/**An access level in relation to a resource.
@author Garret Wilson
*/
public interface AccessLevel extends URFResource
{

	/**The access level type this access level represents.*/
	public AccessLevelType getAccessLevelType();

	/**Returns this access level's allowed permissions.
	@return This access level's allowed permissions.
	@see MarmotSecurity#ALLOW_PROPERTY_URI
	*/
	public Iterable<Permission> getAllows();

	/**Adds a particular permission as allowed.
	@param permission The permission to be allowed.
	@see MarmotSecurity#ALLOW_PROPERTY_URI
	*/
	public void addAllow(final Permission permission);

	/**Adds a particular permission as allowed.
	@param permissionType The type of permission to be allowed.
	@see MarmotSecurity#ALLOW_PROPERTY_URI
	*/
	public void addAllow(final PermissionType permissionType);

	/**Sets the allowed permissions to those specified.
	@param permissions The permissions that should be allowed.
	@see MarmotSecurity#ALLOW_PROPERTY_URI
	*/
	public void setAllowedPermissions(final Set<Permission> permissions);

	/**Sets the allowed permissions to those specified.
	@param permissionTypes The permission types that should be allowed.
	@see MarmotSecurity#ALLOW_PROPERTY_URI
	*/
	public void setAllowedPermissionTypes(final Set<PermissionType> permissionTypes);

	/**Returns this access level's denied permissions.
	@return This access level's denied permissions.
	@see MarmotSecurity#DENY_PROPERTY_URI
	*/
	public Iterable<Permission> getDenies();

	/**Adds a particular permission as denied.
	@param permission The permission to be denied.
	@see MarmotSecurity#DENY_PROPERTY_URI
	*/
	public void addDeny(final Permission permission);

	/**Adds a particular permission as denied.
	@param permissionType The type of permission to be denied.
	@see MarmotSecurity#DENY_PROPERTY_URI
	*/
	public void addDeny(final PermissionType permissionType);

	/**Sets the denied permissions to those specified.
	@param permissions The permissions that should be denied.
	@see MarmotSecurity#DENY_PROPERTY_URI
	*/
	public void setDeniedPermissions(final Set<Permission> permissions);

	/**Sets the denied permissions to those specified.
	@param permissionTypes The permission types that should be denied.
	@see MarmotSecurity#DENY_PROPERTY_URI
	*/
	public void setDeniedPermissionTypes(final Set<PermissionType> permissionTypes);

}