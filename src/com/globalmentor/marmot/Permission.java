package com.globalmentor.marmot;

import com.garretwilson.rdf.RDFResource;

import com.globalmentor.marmot.security.PermissionType;

/**A permission in relation to a resource.
@author Garret Wilson
*/
public interface Permission extends RDFResource
{

	/**The permission type this permission represents.*/
	public PermissionType getPermissionType();

}