package com.globalmentor.marmot.security;

import java.net.URI;

import com.globalmentor.marmot.AbstractAccessLevel;

/**The retrieve access level.
@author Garret Wilson
*/
public class RetrieveAccessLevel extends AbstractAccessLevel
{

	/**Default constructor.*/
	public RetrieveAccessLevel()
	{
		this(null);	//construct the class with no URI
	}

	/**URI constructor.
	@param uri The URI for the new resource.
	*/
	public RetrieveAccessLevel(final URI uri)
	{
		super(uri);  //construct the parent class
	}
}