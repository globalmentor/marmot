package com.globalmentor.marmot.security;

import java.net.URI;

import com.globalmentor.marmot.AbstractAccessLevel;

/**The full access level.
@author Garret Wilson
*/
public class FullAccessLevel extends AbstractAccessLevel
{

	/**Default constructor.*/
	public FullAccessLevel()
	{
		this(null);	//construct the class with no URI
	}

	/**URI constructor.
	@param uri The URI for the new resource.
	*/
	public FullAccessLevel(final URI uri)
	{
		super(uri);  //construct the parent class
	}
}