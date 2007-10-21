package com.globalmentor.marmot.security;

import java.net.URI;

import com.globalmentor.marmot.AbstractAccessLevel;

/**The private access level.
@author Garret Wilson
*/
public class PrivateAccessLevel extends AbstractAccessLevel
{

	/**Default constructor.*/
	public PrivateAccessLevel()
	{
		this(null);	//construct the class with no URI
	}

	/**URI constructor.
	@param uri The URI for the new resource.
	*/
	public PrivateAccessLevel(final URI uri)
	{
		super(uri);  //construct the parent class
	}
}