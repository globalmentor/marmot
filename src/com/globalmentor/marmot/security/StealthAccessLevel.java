package com.globalmentor.marmot.security;

import java.net.URI;

import com.globalmentor.marmot.AbstractAccessLevel;

/**The stealth access level.
@author Garret Wilson
*/
public class StealthAccessLevel extends AbstractAccessLevel
{

	/**Default constructor.*/
	public StealthAccessLevel()
	{
		this(null);	//construct the class with no URI
	}

	/**URI constructor.
	@param uri The URI for the new resource.
	*/
	public StealthAccessLevel(final URI uri)
	{
		super(uri);  //construct the parent class
	}
}