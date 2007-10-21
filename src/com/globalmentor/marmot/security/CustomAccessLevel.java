package com.globalmentor.marmot.security;

import java.net.URI;

import com.globalmentor.marmot.AbstractAccessLevel;

/**The custom access level.
@author Garret Wilson
*/
public class CustomAccessLevel extends AbstractAccessLevel
{

	/**Default constructor.*/
	public CustomAccessLevel()
	{
		this(null);	//construct the class with no URI
	}

	/**URI constructor.
	@param uri The URI for the new resource.
	*/
	public CustomAccessLevel(final URI uri)
	{
		super(uri);  //construct the parent class
	}
}