package com.globalmentor.marmot.security;

import com.garretwilson.urf.URFResource;

/**A permission in relation to a resource.
@author Garret Wilson
*/
public interface Permission extends URFResource
{

	/**The permission type this permission represents.*/
	public PermissionType getPermissionType();

}