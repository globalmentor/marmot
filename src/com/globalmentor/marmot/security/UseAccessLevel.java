package com.globalmentor.marmot.security;

import java.net.URI;

import com.globalmentor.marmot.AbstractAccessLevel;

/**The use access level.
@author Garret Wilson
*/
public class UseAccessLevel extends AbstractAccessLevel
{

	/**Default constructor.*/
	public UseAccessLevel()
	{
		this(null);	//construct the class with no URI
	}

	/**URI constructor.
	@param uri The URI for the new resource.
	*/
	public UseAccessLevel(final URI uri)
	{
		super(uri);  //construct the parent class
	}
}