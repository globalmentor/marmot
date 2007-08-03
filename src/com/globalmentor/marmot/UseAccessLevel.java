package com.globalmentor.marmot;

import java.net.URI;

/**The use access level.
@author Garret Wilson
*/
public class UseAccessLevel extends AbstractAccessLevel
{

	/**Default constructor.*/
	public UseAccessLevel()
	{
		this(null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	*/
	public UseAccessLevel(final URI referenceURI)
	{
		super(referenceURI);  //construct the parent class
	}
}