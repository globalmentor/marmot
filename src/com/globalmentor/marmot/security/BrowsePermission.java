package com.globalmentor.marmot.security;

import java.net.URI;

/**The browse permission.
@author Garret Wilson
*/
public class BrowsePermission extends AbstractPermission
{

	/**Default constructor.*/
	public BrowsePermission()
	{
		this(null);	//construct the class with no URI
	}

	/**URI constructor.
	@param uri The URI for the new resource.
	*/
	public BrowsePermission(final URI uri)
	{
		super(uri);  //construct the parent class
	}
}