package com.globalmentor.marmot.security;

import java.net.URI;

/**The discover permission.
@author Garret Wilson
*/
public class DiscoverPermission extends AbstractPermission
{

	/**Default constructor.*/
	public DiscoverPermission()
	{
		this(null);	//construct the class with no URI
	}

	/**URI constructor.
	@param uri The URI for the new resource.
	*/
	public DiscoverPermission(final URI uri)
	{
		super(uri);  //construct the parent class
	}
}