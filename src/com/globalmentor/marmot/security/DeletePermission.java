package com.globalmentor.marmot.security;

import java.net.URI;

/**The delete permission.
@author Garret Wilson
*/
public class DeletePermission extends AbstractPermission
{

	/**Default constructor.*/
	public DeletePermission()
	{
		this(null);	//construct the class with no URI
	}

	/**URI constructor.
	@param uri The URI for the new resource.
	*/
	public DeletePermission(final URI uri)
	{
		super(uri);  //construct the parent class
	}
}