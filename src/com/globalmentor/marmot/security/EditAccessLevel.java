package com.globalmentor.marmot.security;

import java.net.URI;

import com.globalmentor.marmot.AbstractAccessLevel;

/**The edit access level.
@author Garret Wilson
*/
public class EditAccessLevel extends AbstractAccessLevel
{

	/**Default constructor.*/
	public EditAccessLevel()
	{
		this(null);	//construct the class with no URI
	}

	/**URI constructor.
	@param uri The URI for the new resource.
	*/
	public EditAccessLevel(final URI uri)
	{
		super(uri);  //construct the parent class
	}
}