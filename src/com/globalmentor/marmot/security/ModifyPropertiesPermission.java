package com.globalmentor.marmot.security;

import java.net.URI;

/**The modify properties permission.
@author Garret Wilson
*/
public class ModifyPropertiesPermission extends AbstractPermission
{

	/**Default constructor.*/
	public ModifyPropertiesPermission()
	{
		this(null);	//construct the class with no URI
	}

	/**URI constructor.
	@param uri The URI for the new resource.
	*/
	public ModifyPropertiesPermission(final URI uri)
	{
		super(uri);  //construct the parent class
	}
}