package com.globalmentor.marmot.security;

import java.net.URI;

import com.globalmentor.marmot.AbstractAccessLevel;

/**The inherited access level.
@author Garret Wilson
*/
public class InheritedAccessLevel extends AbstractAccessLevel
{

	/**Default constructor.*/
	public InheritedAccessLevel()
	{
		this(null);	//construct the class with no URI
	}

	/**URI constructor.
	@param uri The URI for the new resource.
	*/
	public InheritedAccessLevel(final URI uri)
	{
		super(uri);  //construct the parent class
	}
}