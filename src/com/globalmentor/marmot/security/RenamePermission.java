package com.globalmentor.marmot.security;

import java.net.URI;

/**The rename permission.
@author Garret Wilson
*/
public class RenamePermission extends AbstractPermission
{

	/**Default constructor.*/
	public RenamePermission()
	{
		this(null);	//construct the class with no URI
	}

	/**URI constructor.
	@param uri The URI for the new resource.
	*/
	public RenamePermission(final URI uri)
	{
		super(uri);  //construct the parent class
	}
}