package com.globalmentor.marmot.security;

import java.net.URI;

/**The preview permission.
@author Garret Wilson
*/
public class PreviewPermission extends AbstractPermission
{

	/**Default constructor.*/
	public PreviewPermission()
	{
		this(null);	//construct the class with no URI
	}

	/**URI constructor.
	@param uri The URI for the new resource.
	*/
	public PreviewPermission(final URI uri)
	{
		super(uri);  //construct the parent class
	}
}