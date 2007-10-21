package com.globalmentor.marmot.security;

import java.net.URI;

/**The add permission.
@author Garret Wilson
*/
public class AddPermission extends AbstractPermission
{

	/**Default constructor.*/
	public AddPermission()
	{
		this(null);	//construct the class with no URI
	}

	/**URI constructor.
	@param uri The URI for the new resource.
	*/
	public AddPermission(final URI uri)
	{
		super(uri);  //construct the parent class
	}
}