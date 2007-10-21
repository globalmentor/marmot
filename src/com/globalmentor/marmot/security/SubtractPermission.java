package com.globalmentor.marmot.security;

import java.net.URI;

/**The subtract permission.
@author Garret Wilson
*/
public class SubtractPermission extends AbstractPermission
{

	/**Default constructor.*/
	public SubtractPermission()
	{
		this(null);	//construct the class with no URI
	}

	/**URI constructor.
	@param uri The URI for the new resource.
	*/
	public SubtractPermission(final URI uri)
	{
		super(uri);  //construct the parent class
	}
}