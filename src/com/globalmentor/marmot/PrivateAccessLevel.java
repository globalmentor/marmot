package com.globalmentor.marmot;

import java.net.URI;

/**The private access level.
@author Garret Wilson
*/
public class PrivateAccessLevel extends AbstractAccessLevel
{

	/**Default constructor.*/
	public PrivateAccessLevel()
	{
		this(null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	*/
	public PrivateAccessLevel(final URI referenceURI)
	{
		super(referenceURI);  //construct the parent class
	}
}