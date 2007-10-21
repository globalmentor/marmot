package com.globalmentor.marmot.security;

import java.net.URI;

/**The execute permission.
@author Garret Wilson
*/
public class ExecutePermission extends AbstractPermission
{

	/**Default constructor.*/
	public ExecutePermission()
	{
		this(null);	//construct the class with no URI
	}

	/**URI constructor.
	@param uri The URI for the new resource.
	*/
	public ExecutePermission(final URI uri)
	{
		super(uri);  //construct the parent class
	}
}