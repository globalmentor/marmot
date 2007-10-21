package com.globalmentor.marmot.security;

import java.net.URI;

/**The modify security permission.
@author Garret Wilson
*/
public class ModifySecurityPermission extends AbstractPermission
{

	/**Default constructor.*/
	public ModifySecurityPermission()
	{
		this(null);	//construct the class with no URI
	}

	/**URI constructor.
	@param uri The URI for the new resource.
	*/
	public ModifySecurityPermission(final URI uri)
	{
		super(uri);  //construct the parent class
	}
}