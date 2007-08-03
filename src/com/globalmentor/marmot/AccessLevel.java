package com.globalmentor.marmot;

import java.util.Set;

import com.garretwilson.rdf.RDFResource;
import com.globalmentor.marmot.security.*;

/**An access level in relation to a resource.
@author Garret Wilson
*/
public interface AccessLevel extends RDFResource
{

	/**The access level type this access level represents.*/
	public AccessLevelType getAccessLevelType();

	/**@return This access level's allowed permissions.*/
	public Iterable<Permission> getAllows();

	/**Adds a particular permission as allowed.
	@param permission The permission to be allowed.
	*/
	public void addAllow(final Permission permission);

	/**Adds a particular permission as allowed.
	@param permissionType The type of permission to be allowed.
	*/
	public void addAllow(final PermissionType permissionType);

	/**Sets the allowed permissions to those specified.
	@param permissions The permissions that should be allowed.
	*/
	public void setAllowedPermissions(final Set<Permission> permissions);

	/**Sets the allowed permissions to those specified.
	@param permissionTypes The permission types that should be allowed.
	*/
	public void setAllowedPermissionTypes(final Set<PermissionType> permissionTypes);

	/**@return This access level's denied permissions.*/
	public Iterable<Permission> getDenies();

	/**Adds a particular permission as denied.
	@param permission The permission to be denied.
	*/
	public void addDeny(final Permission permission);

	/**Adds a particular permission as denied.
	@param permissionType The type of permission to be denied.
	*/
	public void addDeny(final PermissionType permissionType);

	/**Sets the denied permissions to those specified.
	@param permissions The permissions that should be denied.
	*/
	public void setDeniedPermissions(final Set<Permission> permissions);

	/**Sets the denied permissions to those specified.
	@param permissionTypes The permission types that should be denied.
	*/
	public void setDeniedPermissionTypes(final Set<PermissionType> permissionTypes);

}