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

import java.util.Set;

import org.urframework.URFResource;


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