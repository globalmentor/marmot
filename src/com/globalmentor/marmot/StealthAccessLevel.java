package com.globalmentor.marmot;

import java.net.URI;

/**The stealth access level.
@author Garret Wilson
*/
public class StealthAccessLevel extends AbstractAccessLevel
{

	/**Default constructor.*/
	public StealthAccessLevel()
	{
		this(null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	*/
	public StealthAccessLevel(final URI referenceURI)
	{
		super(referenceURI);  //construct the parent class
	}
}