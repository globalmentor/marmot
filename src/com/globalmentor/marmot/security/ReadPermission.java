package com.globalmentor.marmot.security;

import java.net.URI;

/**The read permission.
@author Garret Wilson
*/
public class ReadPermission extends AbstractPermission
{

	/**Default constructor.*/
	public ReadPermission()
	{
		this(null);	//construct the class with no URI
	}

	/**URI constructor.
	@param uri The URI for the new resource.
	*/
	public ReadPermission(final URI uri)
	{
		super(uri);  //construct the parent class
	}
}