package com.globalmentor.marmot.security;

import java.net.URI;

import com.globalmentor.marmot.AbstractAccessLevel;

/**The preview access level.
@author Garret Wilson
*/
public class PreviewAccessLevel extends AbstractAccessLevel
{

	/**Default constructor.*/
	public PreviewAccessLevel()
	{
		this(null);	//construct the class with no URI
	}

	/**URI constructor.
	@param uri The URI for the new resource.
	*/
	public PreviewAccessLevel(final URI uri)
	{
		super(uri);  //construct the parent class
	}
}