package com.globalmentor.marmot;

import java.net.URI;

/**The full access level.
@author Garret Wilson
*/
public class FullAccessLevel extends AbstractAccessLevel
{

	/**Default constructor.*/
	public FullAccessLevel()
	{
		this(null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	*/
	public FullAccessLevel(final URI referenceURI)
	{
		super(referenceURI);  //construct the parent class
	}
}