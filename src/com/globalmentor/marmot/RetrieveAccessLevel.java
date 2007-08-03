package com.globalmentor.marmot;

import java.net.URI;

/**The retrieve access level.
@author Garret Wilson
*/
public class RetrieveAccessLevel extends AbstractAccessLevel
{

	/**Default constructor.*/
	public RetrieveAccessLevel()
	{
		this(null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	*/
	public RetrieveAccessLevel(final URI referenceURI)
	{
		super(referenceURI);  //construct the parent class
	}
}